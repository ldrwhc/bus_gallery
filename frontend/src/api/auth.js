import http from './axiosInstance';

export const login = (payload) => http.post('/auth/login', payload);

export const register = (payload) => http.post('/auth/register', payload);

export const logout = () => http.post('/auth/logout');
