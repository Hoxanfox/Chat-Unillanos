import { useState, useEffect } from 'react';
import { fetchHealth } from '../services/api';

export const useHealth = () => {
    const [health, setHealth] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const getHealth = async () => {
            try {
                const data = await fetchHealth();
                setHealth(data);
                setError(null);
            } catch (err) {
                setError('Error fetching health status');
                console.error(err);
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
