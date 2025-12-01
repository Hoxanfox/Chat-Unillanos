import axios from 'axios';

const API_URL = 'http://localhost:8081/gateway/logs';

export const api = axios.create({
    baseURL: API_URL,
});

export const fetchLogs = async () => {
    const response = await api.get('');
    return response.data;
};

export const fetchStats = async () => {
    const response = await api.get('/stats');
    return response.data;
};

export const clearLogs = async () => {
    const response = await api.delete('');
    return response.data;
};

export const fetchHealth = async () => {
    const response = await api.get('/health');
    return response.data;
};
