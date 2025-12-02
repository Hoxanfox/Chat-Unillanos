import React, { useState, useMemo } from 'react';
import { Play, Pause, Trash2, Search, Filter } from 'lucide-react';
import clsx from 'clsx';

const LogsView = ({ logs, loading, error, paused, setPaused, clearLogs, refresh }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedLevel, setSelectedLevel] = useState('ALL');

    const filteredLogs = useMemo(() => {
        let result = logs;

        // Filter by Level
        if (selectedLevel !== 'ALL') {
            result = result.filter(log =>
                log.contenido.level?.toUpperCase() === selectedLevel
            );
        }

        // Filter by Search Term
        if (searchTerm) {
            result = result.filter(log =>
                log.contenido.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
                log.contenido.source.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }
        return result;
    }, [logs, searchTerm, selectedLevel]);

    const getLevelBadge = (level) => {
        switch (level?.toUpperCase()) {
            case 'ERROR':
                return <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20">ERROR</span>;
            case 'WARN':
            case 'WARNING':
                return <span className="px-2 py-1 rounded-full text-xs font-medium bg-yellow-500/10 text-yellow-400 border border-yellow-500/20">WARN</span>;
            case 'INFO':
                return <span className="px-2 py-1 rounded-full text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20">INFO</span>;
            case 'DEBUG':
                return <span className="px-2 py-1 rounded-full text-xs font-medium bg-purple-500/10 text-purple-400 border border-purple-500/20">DEBUG</span>;
            default:
                return <span className="px-2 py-1 rounded-full text-xs font-medium bg-gray-500/10 text-gray-400 border border-gray-500/20">{level}</span>;
        }
    };

    return (
        <div className="flex flex-col h-full space-y-4">
            {/* Toolbar */}
            <div className="flex flex-wrap items-center justify-between gap-4 bg-gray-900 p-4 rounded-xl border border-gray-800">
                <div className="flex items-center gap-2 flex-1 min-w-[200px]">
                    <div className="relative flex-1 max-w-md">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={18} />
                        <input
                            type="text"
                            placeholder="Search logs..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full bg-gray-950 border border-gray-700 rounded-lg pl-10 pr-4 py-2 text-sm text-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>

                    <div className="relative">
                        <Filter className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={16} />
                        <select
                            value={selectedLevel}
                            onChange={(e) => setSelectedLevel(e.target.value)}
                            className="bg-gray-950 border border-gray-700 rounded-lg pl-10 pr-4 py-2 text-sm text-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500 appearance-none cursor-pointer"
                        >
                            <option value="ALL">All Levels</option>
                            <option value="DEBUG">Debug</option>
                            <option value="INFO">Info</option>
                            <option value="WARNING">Warning</option>
                            <option value="ERROR">Error</option>
                        </select>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        onClick={() => setPaused(!paused)}
                        className={clsx(
                            "flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors",
                            paused
                                ? "bg-yellow-500/10 text-yellow-500 hover:bg-yellow-500/20 border border-yellow-500/20"
                                : "bg-green-500/10 text-green-500 hover:bg-green-500/20 border border-green-500/20"
                        )}
                    >
                        {paused ? <Play size={16} /> : <Pause size={16} />}
                        {paused ? 'Resume' : 'Pause'}
                    </button>

                    <button
                        onClick={clearLogs}
                        className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-red-500/10 text-red-500 hover:bg-red-500/20 border border-red-500/20 transition-colors"
                    >
                        <Trash2 size={16} />
                        Clear
                    </button>
                </div>
            </div>

            {/* Table */}
            <div className="flex-1 bg-gray-900 rounded-xl border border-gray-800 overflow-hidden flex flex-col">
                <div className="overflow-x-auto flex-1">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-950 text-gray-400 uppercase text-xs font-semibold sticky top-0 z-10">
                            <tr>
                                <th className="px-6 py-3 border-b border-gray-800">Timestamp</th>
                                <th className="px-6 py-3 border-b border-gray-800">Level</th>
                                <th className="px-6 py-3 border-b border-gray-800">Source</th>
                                <th className="px-6 py-3 border-b border-gray-800">Message</th>
                                <th className="px-6 py-3 border-b border-gray-800">Peer IP</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-800">
                            {loading && logs.length === 0 ? (
                                <tr>
                                    <td colSpan="5" className="px-6 py-8 text-center text-gray-500">Loading logs...</td>
                                </tr>
                            ) : filteredLogs.length === 0 ? (
                                <tr>
                                    <td colSpan="5" className="px-6 py-8 text-center text-gray-500">No logs found</td>
                                </tr>
                            ) : (
                                filteredLogs.map((log, idx) => (
                                    <tr key={idx} className="hover:bg-gray-800/50 transition-colors group">
                                        <td className="px-6 py-3 text-gray-400 whitespace-nowrap font-mono text-xs">
                                            {log.contenido.timestamp}
                                        </td>
                                        <td className="px-6 py-3">
                                            {getLevelBadge(log.contenido.level)}
                                        </td>
                                        <td className="px-6 py-3 text-gray-300 font-medium">
                                            {log.contenido.source}
                                        </td>
                                        <td className="px-6 py-3 text-gray-300 max-w-xl break-words font-mono text-xs">
                                            {log.contenido.message}
                                        </td>
                                        <td className="px-6 py-3 text-gray-500 text-xs">
                                            {log.peer_ip}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
                <div className="px-4 py-2 bg-gray-950 border-t border-gray-800 text-xs text-gray-500 flex justify-between items-center">
                    <span>Showing {filteredLogs.length} logs</span>
                    <span>{paused ? 'Updates Paused' : 'Live Updates Active'}</span>
                </div>
            </div>
        </div>
    );
};

export default LogsView;
