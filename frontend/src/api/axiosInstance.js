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
    timeout: 20000
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
            if (payload.code === 'A0401') {
                resetAuthState();
                const redirect = router.currentRoute.value.fullPath;
                notifyAuthRequired(payload.message || '登录已失效，请重新登录');
                if (router.currentRoute.value.name !== 'Login') {
                    router.push({ name: 'Login', query: { redirect } });
                }
            }
            const error = new Error(payload.message || '请求错误');
            error.code = payload.code;
            return Promise.reject(error);
        }
        return payload;
    },
    async (error) => {
        const status = error?.response?.status;
        const requestConfig = error?.config;
        const method = (requestConfig?.method || 'get').toLowerCase();
        const isGetRequest = method === 'get';
        const isTimeout = error?.code === 'ECONNABORTED' || /timeout/i.test(error?.message || '');
        const isNetworkFlake = !error?.response && !status;
        if (
            requestConfig &&
            isGetRequest &&
            !requestConfig.__retriedTransient &&
            (isTimeout || isNetworkFlake)
        ) {
            requestConfig.__retriedTransient = true;
            await new Promise((resolve) => setTimeout(resolve, 400));
            return http.request(requestConfig);
        }
        if (status === 429 && requestConfig && isGetRequest && !requestConfig.__retried429) {
            requestConfig.__retried429 = true;
            const retryAfterHeader = Number(error?.response?.headers?.['retry-after']);
            const delayMs = Number.isFinite(retryAfterHeader)
                ? Math.max(200, Math.min(2000, retryAfterHeader * 1000))
                : 350;
            await new Promise((resolve) => setTimeout(resolve, delayMs));
            return http.request(requestConfig);
        }
        if (status === 401) {
            resetAuthState();
            const redirect = router.currentRoute.value.fullPath;
            notifyAuthRequired('登录已失效，请重新登录');
            if (router.currentRoute.value.name !== 'Login') {
                router.push({ name: 'Login', query: { redirect } });
            }
        }
        if (axios.isCancel(error) || error?.code === 'ERR_CANCELED') {
            return Promise.reject(error);
        }
        const message = error?.response?.data?.message || error.message || '请求错误';
        console.error('请求错误:', message);
        return Promise.reject(new Error(message));
    }
);

export default http;
