// src/api/axiosInstance.js
import axios from 'axios';

// 创建axios实例
const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api', // 适配vite环境变量
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=utf-8'
    }
});

// 请求拦截器
http.interceptors.request.use(
    (config) => {
        // 可添加token等请求头
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 响应拦截器
http.interceptors.response.use(
    (response) => {
        return response.data;
    },
    (error) => {
        console.error('请求错误：', error);
        return Promise.reject(error);
    }
);

export default http;