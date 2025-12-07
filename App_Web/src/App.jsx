import React, { useState, useEffect } from 'react';
import Layout from './components/Layout';
import StatsView from './components/StatsView';
import LogsView from './components/LogsView';
import { useLogs } from './hooks/useLogs';
import { useStats } from './hooks/useStats';
import { useHealth } from './hooks/useHealth';
import { fetchStats } from './services/api';

function App() {
    const [activeTab, setActiveTab] = useState('stats');
    const [selectedPeer, setSelectedPeer] = useState(null);
    const [peers, setPeers] = useState([]);

    // Fetch available peers for the filter
    useEffect(() => {
        const getPeers = async () => {
            try {
                const data = await fetchStats();
                // Extract unique peers from stats
                const uniquePeers = data.map(item => ({
                    peer_id: item.peer_id,
                    peer_ip: item.peer_ip
                }));
                // Remove duplicates if any
                const unique = uniquePeers.filter((v, i, a) => a.findIndex(t => (t.peer_id === v.peer_id)) === i);
                setPeers(unique);
            } catch (err) {
                console.error("Failed to fetch peers", err);
            }
        };
        getPeers();
        const interval = setInterval(getPeers, 5000); // Poll every 5 seconds
        return () => clearInterval(interval);
    }, []);

    const { logs, loading: logsLoading, error: logsError, paused, setPaused, clearLogs, refresh } = useLogs(selectedPeer);
    const { stats, loading: statsLoading, error: statsError } = useStats(selectedPeer);
    const { health, loading: healthLoading } = useHealth();

    return (
        <Layout
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            peers={peers}
            selectedPeer={selectedPeer}
            setSelectedPeer={setSelectedPeer}
        >
            {activeTab === 'stats' ? (
                <StatsView
                    stats={stats}
                    health={health}
                    loading={statsLoading || healthLoading}
                    error={statsError}
                />
            ) : (
                <LogsView
                    logs={logs}
                    loading={logsLoading}
                    error={logsError}
                    paused={paused}
                    setPaused={setPaused}
                    clearLogs={clearLogs}
                    refresh={refresh}
                />
            )}
        </Layout>
    );
}

export default App;
