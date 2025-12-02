import axios from 'axios';

const API_URL = 'http://localhost:8081/gateway';

export const api = axios.create({
    baseURL: API_URL,
});

export const fetchLogs = async () => {
    const response = await api.get('/logs');
    return response.data;
};

export const fetchStats = async () => {
    const response = await api.get('/logs/stats');
    return response.data;
};

export const clearLogs = async () => {
    const response = await api.delete('/logs');
    return response.data;
};

export const fetchHealth = async () => {
    const response = await api.get('/logs/health');
    return response.data;
};

export const fetchNetworkPeers = async () => {
    const response = await api.get('/network');
    return response.data;
};
