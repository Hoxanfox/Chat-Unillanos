import { useState, useEffect, useCallback } from 'react';
import { fetchLogs, clearLogs as apiClearLogs } from '../services/api';

export const useLogs = (selectedPeer) => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [paused, setPaused] = useState(false);

    // Keep getLogs for manual refresh if needed, but main logic is SSE
    const getLogs = useCallback(async () => {
        try {
            const data = await fetchLogs();
            // Filter by peer if selected
            const filteredData = selectedPeer
                ? data.filter(log => log.peer_ip === selectedPeer)
                : data;

            // Sort by timestamp desc
            const sortedData = filteredData.sort((a, b) => {
                return new Date(b.contenido.timestamp) - new Date(a.contenido.timestamp);
            });

            setLogs(sortedData);
            setError(null);
        } catch (err) {
            setError('Error fetching logs');
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, [selectedPeer]);

    useEffect(() => {
        // Initial fetch via HTTP GET for reliability
        getLogs();

        if (paused) return;

        // Hardcode base URL to avoid double path issues if VITE_API_URL has path
        const baseUrl = 'http://localhost:8081';

        // Construct URL: If selectedPeer, add param. Else use base stream (aggregated).
        const streamUrl = selectedPeer
            ? `${baseUrl}/gateway/logs/stream?peer=${selectedPeer}`
            : `${baseUrl}/gateway/logs/stream`;

        console.log("Connecting to SSE:", streamUrl);
        const eventSource = new EventSource(streamUrl);

        eventSource.onopen = () => {
            console.log("SSE Connected");
            setLoading(false);
        };

        const handleMessage = (event) => {
            try {
                console.log("SSE Message Received:", event.data);
                const data = JSON.parse(event.data);

                // Adapt format
                // If selectedPeer (Proxy), data is raw DTOLog -> Wrap it and add peer_ip.
                // If !selectedPeer (Aggregated), data is PeerResponse -> Use as is.
                const adaptedLog = selectedPeer
                    ? { peer_id: 'proxy', peer_ip: selectedPeer, contenido: data }
                    : data;

                setLogs(prevLogs => {
                    // Deduplication check: Avoid adding if exact match exists at the top
                    // We check the first few logs to be efficient
                    const isDuplicate = prevLogs.slice(0, 50).some(l =>
                        l.peer_ip === adaptedLog.peer_ip &&
                        l.contenido.timestamp === adaptedLog.contenido.timestamp &&
                        l.contenido.message === adaptedLog.contenido.message
                    );

                    if (isDuplicate) {
                        console.log("Duplicate log ignored:", adaptedLog.contenido.message);
                        return prevLogs;
                    }

                    console.log("New log added:", adaptedLog.contenido.message);
                    return [adaptedLog, ...prevLogs].slice(0, 1000);
                });
                setLoading(false);
            } catch (e) {
                console.error("Error parsing SSE log:", e);
            }
        };

        eventSource.onmessage = handleMessage;
        eventSource.addEventListener('log', handleMessage);

        eventSource.onerror = (err) => {
            console.error("SSE Error:", err);
            eventSource.close();
            setLoading(false); // Ensure we don't get stuck loading
        };

        // Cleanup
        return () => {
            eventSource.removeEventListener('log', handleMessage);
            eventSource.close();
        };
    }, [selectedPeer, paused, getLogs]);

    const clearLogs = async () => {
        try {
            await apiClearLogs();
            setLogs([]);
        } catch (err) {
            setError('Error clearing logs');
        }
    };

    return { logs, loading, error, paused, setPaused, clearLogs, refresh: getLogs };
};
