import axios from 'axios';
import router from '@/router';
import { getToken, persistToken } from '@/utils/auth';

const configuredBase = (import.meta.env.VITE_API_BASE_URL || '').trim();
const fallbackOrigin =
    typeof window !== 'undefined' && window.location?.origin
        ? window.location.origin
        : 'http://localhost:8080';
const normalizedBase = configuredBase || `${fallbackOrigin.replace(/\/$/, '')}/api`;

const http = axios.create({
    baseURL: normalizedBase,
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
        const status = error?.response?.status;
        if (status === 401) {
            persistToken('');
            const redirect = router.currentRoute.value.fullPath;
            if (router.currentRoute.value.name !== 'Login') {
                router.push({ name: 'Login', query: { redirect } });
            }
        }
        const message = error?.response?.data?.message || error.message || '请求错误';
        console.error('请求错误：', message);
        return Promise.reject(new Error(message));
    }
);

export default http;
