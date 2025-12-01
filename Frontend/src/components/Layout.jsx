import React from 'react';
import { LayoutDashboard, ScrollText, Network } from 'lucide-react';
import clsx from 'clsx';

const Layout = ({ children, activeTab, setActiveTab, peers, selectedPeer, setSelectedPeer }) => {
    return (
        <div className="flex h-screen bg-gray-950 text-gray-100 font-sans">
            {/* Sidebar */}
            <aside className="w-64 bg-gray-900 border-r border-gray-800 flex flex-col">
                <div className="p-6 flex items-center gap-3 border-b border-gray-800">
                    <Network className="w-8 h-8 text-blue-500" />
                    <h1 className="text-xl font-bold tracking-tight">P2P Monitor</h1>
                </div>

                <nav className="flex-1 p-4 space-y-2">
                    <button
                        onClick={() => setActiveTab('stats')}
                        className={clsx(
                            "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors",
                            activeTab === 'stats'
                                ? "bg-blue-600/10 text-blue-400 border border-blue-600/20"
                                : "hover:bg-gray-800 text-gray-400"
                        )}
                    >
                        <LayoutDashboard size={20} />
                        <span className="font-medium">Dashboard / Stats</span>
                    </button>

                    <button
                        onClick={() => setActiveTab('logs')}
                        className={clsx(
                            "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors",
                            activeTab === 'logs'
                                ? "bg-blue-600/10 text-blue-400 border border-blue-600/20"
                                : "hover:bg-gray-800 text-gray-400"
                        )}
                    >
                        <ScrollText size={20} />
                        <span className="font-medium">Live Logs</span>
                    </button>
                </nav>

                <div className="p-4 border-t border-gray-800">
                    <div className="text-xs text-gray-500 uppercase font-bold mb-2">Global Filter</div>
                    <select
                        value={selectedPeer || ''}
                        onChange={(e) => setSelectedPeer(e.target.value || null)}
                        className="w-full bg-gray-950 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">All Peers</option>
                        {peers.map((peer) => (
                            <option key={peer.peer_id} value={peer.peer_id}>
                                {peer.peer_ip} ({peer.peer_id.substring(0, 8)}...)
                            </option>
                        ))}
                    </select>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 overflow-hidden flex flex-col">
                <header className="h-16 border-b border-gray-800 flex items-center px-8 bg-gray-900/50 backdrop-blur-sm">
                    <h2 className="text-lg font-semibold text-gray-200">
                        {activeTab === 'stats' ? 'System Statistics' : 'Live Log Stream'}
                    </h2>
                </header>
                <div className="flex-1 overflow-auto p-8">
                    {children}
                </div>
            </main>
        </div>
    );
};

export default Layout;
