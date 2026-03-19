import http from './axiosInstance';

export const login = (payload) => http.post('/auth/login', payload);

export const register = (payload) => http.post('/auth/register', payload);

export const logout = () => http.post('/auth/logout');

export const sendRegisterEmailCode = (payload) => http.post('/auth/register/send-email-code', payload);

export const sendPasswordChangeEmailCode = (payload) => http.post('/auth/password/change/send-email-code', payload);

export const confirmPasswordChange = (payload) => http.post('/auth/password/change/confirm', payload);

export const sendForgotPasswordEmailCode = (payload) => http.post('/auth/password/forgot/send-email-code', payload);

export const verifyForgotPasswordEmailCode = (payload) => http.post('/auth/password/forgot/verify-email-code', payload);

export const resetForgotPassword = (payload) => http.post('/auth/password/forgot/reset', payload);

export const sendBindEmailCode = (payload) => http.post('/auth/email/bind/send-email-code', payload);

export const confirmBindEmail = (payload) => http.post('/auth/email/bind/confirm', payload);
