import axios from 'axios';
import { getToken } from '@/utils/auth';

const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=utf-8'
    }
});

http.interceptors.request.use(
    (config) => {
        const token = getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

http.interceptors.response.use(
    (response) => response.data,
    (error) => {
        const message = error?.response?.data?.message || error.message || '请求出错';
        console.error('请求错误：', message);
        return Promise.reject(new Error(message));
    }
);

export default http;
