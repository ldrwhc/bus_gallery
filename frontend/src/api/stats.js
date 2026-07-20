import http from './axiosInstance';

export const fetchStats = () =>
    http.get('/stats');
