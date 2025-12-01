import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { Activity, AlertTriangle, AlertCircle, Info } from 'lucide-react';

const StatCard = ({ title, value, icon: Icon, color, subtext }) => (
    <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 flex flex-col gap-4 hover:border-gray-700 transition-colors">
        <div className="flex items-center justify-between">
            <div className={`p-3 rounded-lg bg-${color}-500/10 text-${color}-500`}>
                <Icon size={24} />
            </div>
            <span className="text-3xl font-bold text-gray-100">{value}</span>
        </div>
        <div>
            <h3 className="text-gray-400 font-medium">{title}</h3>
            {subtext && <p className="text-xs text-gray-600 mt-1">{subtext}</p>}
        </div>
    </div>
);

const StatsView = ({ stats, health, loading, error }) => {
    if (loading) return <div className="text-center p-10 text-gray-500">Loading statistics...</div>;
    if (error) return <div className="text-center p-10 text-red-500">{error}</div>;
    if (!stats) return <div className="text-center p-10 text-gray-500">No stats available</div>;

    const data = [
        { name: 'Info', value: stats.info, color: '#3b82f6' }, // blue-500
        { name: 'Warning', value: stats.warning, color: '#eab308' }, // yellow-500
        { name: 'Error', value: stats.error, color: '#ef4444' }, // red-500
        { name: 'Debug', value: stats.debug, color: '#a855f7' }, // purple-500
    ].filter(d => d.value > 0);

    return (
        <div className="space-y-8">
            {/* Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    title="Total Logs"
                    value={stats.total}
                    icon={Activity}
                    color="blue"
                />
                <StatCard
                    title="Errors"
                    value={stats.error}
                    icon={AlertCircle}
                    color="red"
                />
                <StatCard
                    title="Warnings"
                    value={stats.warning}
                    icon={AlertTriangle}
                    color="yellow"
                />
                <StatCard
                    title="Info Messages"
                    value={stats.info}
                    icon={Info}
                    color="blue"
                />
            </div>

            {/* Charts Section */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 h-96">
                    <h3 className="text-lg font-semibold mb-6 text-gray-200">Log Level Distribution</h3>
                    <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                            <Pie
                                data={data}
                                cx="50%"
                                cy="50%"
                                innerRadius={60}
                                outerRadius={100}
                                paddingAngle={5}
                                dataKey="value"
                            >
                                {data.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.color} stroke="none" />
                                ))}
                            </Pie>
                            <Tooltip
                                contentStyle={{ backgroundColor: '#111827', borderColor: '#374151', color: '#f3f4f6' }}
                                itemStyle={{ color: '#f3f4f6' }}
                            />
                            <Legend />
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* Health Status Table */}
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 h-96 flex flex-col">
                    <h3 className="text-lg font-semibold mb-4 text-gray-200">Network Health Status</h3>
                    <div className="overflow-auto flex-1">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-950 text-gray-400 uppercase text-xs font-semibold sticky top-0">
                                <tr>
                                    <th className="px-4 py-3 border-b border-gray-800">Peer IP</th>
                                    <th className="px-4 py-3 border-b border-gray-800">Service</th>
                                    <th className="px-4 py-3 border-b border-gray-800">Logs in Memory</th>
                                    <th className="px-4 py-3 border-b border-gray-800">Status</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-800">
                                {health && health.map((peer, idx) => (
                                    <tr key={idx} className="hover:bg-gray-800/50 transition-colors">
                                        <td className="px-4 py-3 text-gray-300 font-mono">{peer.peer_ip}</td>
                                        <td className="px-4 py-3 text-gray-400">{peer.contenido.service}</td>
                                        <td className="px-4 py-3 text-gray-400">{peer.contenido.logsEnMemoria}</td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-1 rounded-full text-xs font-medium border ${peer.contenido.status === 'UP'
                                                ? 'bg-green-500/10 text-green-400 border-green-500/20'
                                                : 'bg-red-500/10 text-red-400 border-red-500/20'
                                                }`}>
                                                {peer.contenido.status}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                                {(!health || health.length === 0) && (
                                    <tr>
                                        <td colSpan="4" className="px-4 py-8 text-center text-gray-500">No health data available</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StatsView;
