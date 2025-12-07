import { useState, useEffect } from 'react';
import { fetchHealth, fetchNetworkPeers } from '../services/api';

export const useHealth = () => {
    const [health, setHealth] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const getHealth = async () => {
            try {
                // Fetch both network topology and health stats
                const [networkData, healthData] = await Promise.all([
                    fetchNetworkPeers(),
                    fetchHealth().catch(() => []) // If health fails (e.g. no peers), return empty
                ]);

                // Dedup network data by ID
                const uniqueNetworkData = networkData.filter((v, i, a) => a.findIndex(t => (t.id === v.id)) === i);

                // Merge data
                const mergedHealth = uniqueNetworkData.map(peer => {
                    // Find detailed health info if available (Match by ID)
                    const detail = healthData.find(h => h.peer_id === peer.id);

                    if (detail) {
                        // Ensure port is included if missing in detail
                        return { ...detail, port: peer.puerto };
                    } else {
                        // If no detail (offline or not responding), construct status from network data
                        return {
                            peer_id: peer.id,
                            peer_ip: peer.ip, // Use 'ip' from Peer model
                            port: peer.puerto,
                            contenido: {
                                service: 'N/A',
                                logsEnMemoria: 0,
                                status: peer.estado === 'ONLINE' ? 'UP' : 'DOWN'
                            }
                        };
                    }
                });

                setHealth(mergedHealth);
                setError(null);
            } catch (err) {
                console.error('Error fetching health status:', err);
                setError('Error fetching health status');
            } finally {
                setLoading(false);
            }
        };

        getHealth();
        const interval = setInterval(getHealth, 2000); // Refresh every 2s
        return () => clearInterval(interval);
    }, []);

    return { health, loading, error };
};
