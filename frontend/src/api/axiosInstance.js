import axios from 'axios';
import router from '@/router';
import { ElMessage } from 'element-plus';
import { getToken, persistToken } from '@/utils/auth';

const configuredBase = (import.meta.env.VITE_API_BASE_URL || '').trim();
const fallbackOrigin =
    typeof window !== 'undefined' && window.location?.origin
        ? window.location.origin
        : 'http://localhost:8080';
const normalizedBase = configuredBase || `${fallbackOrigin.replace(/\/$/, '')}/api`;

const http = axios.create({
    baseURL: normalizedBase,
    timeout: 10000
});

let authToastCooldown = false;
const notifyAuthRequired = (message) => {
    if (authToastCooldown) return;
    authToastCooldown = true;
    ElMessage.warning(message || '请先登录');
    setTimeout(() => {
        authToastCooldown = false;
    }, 1500);
};

const resetAuthState = async () => {
    persistToken('');
    try {
        const mod = await import('@/store');
        mod.default.commit('auth/RESET');
    } catch (e) {
        // ignore
    }
};

http.interceptors.request.use(
    (config) => {
        const token = getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        const isFormData =
            typeof FormData !== 'undefined' && config.data instanceof FormData;
        if (isFormData) {
            if (config.headers?.delete) {
                config.headers.delete('Content-Type');
                config.headers.delete('content-type');
            } else {
                delete config.headers?.['Content-Type'];
                delete config.headers?.['content-type'];
            }
        } else if (!config.headers?.['Content-Type'] && !config.headers?.['content-type']) {
            config.headers = config.headers || {};
            config.headers['Content-Type'] = 'application/json;charset=utf-8';
        }
        return config;
    },
    (error) => Promise.reject(error)
);

http.interceptors.response.use(
    (response) => {
        const payload = response.data;
        if (
            payload &&
            typeof payload === 'object' &&
            Object.prototype.hasOwnProperty.call(payload, 'code') &&
            Object.prototype.hasOwnProperty.call(payload, 'message') &&
            payload.code !== '00000'
        ) {
            return Promise.reject(new Error(payload.message || '请求错误'));
        }
        return payload;
    },
    (error) => {
        const status = error?.response?.status;
        if (status === 401) {
            resetAuthState();
            const redirect = router.currentRoute.value.fullPath;
            notifyAuthRequired('登录已失效，请重新登录');
            if (router.currentRoute.value.name !== 'Login') {
                router.push({ name: 'Login', query: { redirect } });
            }
        }
        const message = error?.response?.data?.message || error.message || '请求错误';
        console.error('请求错误:', message);
        return Promise.reject(new Error(message));
    }
);

export default http;
