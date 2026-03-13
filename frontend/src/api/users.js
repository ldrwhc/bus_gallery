import http from './axiosInstance';

export const fetchMe = () => http.get('/users/me');

export const fetchMyImages = (params = {}) =>
    http.get('/users/me/images', { params });

export const fetchUserProfile = (userId) => http.get(`/users/${userId}`);

export const fetchUserImages = (userId, params = {}) =>
    http.get(`/users/${userId}/images`, { params });
