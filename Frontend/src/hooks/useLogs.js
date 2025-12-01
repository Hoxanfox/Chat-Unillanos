import { useState, useEffect, useCallback } from 'react';
import { fetchLogs, clearLogs as apiClearLogs } from '../services/api';

export const useLogs = (selectedPeer) => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [paused, setPaused] = useState(false);

    const getLogs = useCallback(async () => {
        try {
            const data = await fetchLogs();
            // Filter by peer if selected
            const filteredData = selectedPeer
                ? data.filter(log => log.peer_id === selectedPeer)
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
        getLogs(); // Initial fetch

        if (paused) return;

        const interval = setInterval(getLogs, 500);
        return () => clearInterval(interval);
    }, [getLogs, paused]);

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
