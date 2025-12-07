import { useState, useEffect } from 'react';
import { fetchStats } from '../services/api';

export const useStats = (selectedPeer) => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const getStats = async () => {
            try {
                const data = await fetchStats();

                let aggregatedStats = {
                    total: 0,
                    info: 0,
                    error: 0,
                    warning: 0,
                    debug: 0
                };

                if (data && data.length > 0) {
                    if (selectedPeer) {
                        const peerStats = data.find(s => s.peer_ip === selectedPeer);
                        if (peerStats) {
                            aggregatedStats = peerStats.contenido;
                        }
                    } else {
                        // Aggregate all peers
                        data.forEach(peer => {
                            aggregatedStats.total += peer.contenido.total || 0;
                            aggregatedStats.info += peer.contenido.info || 0;
                            aggregatedStats.error += peer.contenido.error || 0;
                            aggregatedStats.warning += peer.contenido.warning || 0;
                            aggregatedStats.debug += peer.contenido.debug || 0;
                        });
                    }
                }

                setStats(aggregatedStats);
                setError(null);
            } catch (err) {
                console.error('Error fetching stats:', err);
                // Don't show error to user, just show zeros
                setStats({
                    total: 0,
                    info: 0,
                    error: 0,
                    warning: 0,
                    debug: 0
                });
            } finally {
                setLoading(false);
            }
        };

        getStats();
        const interval = setInterval(getStats, 500); // Refresh stats every 0.5s
        return () => clearInterval(interval);
    }, [selectedPeer]);

    return { stats, loading, error };
};
