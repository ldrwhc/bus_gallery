import http from './axiosInstance';

export const fetchRoutes = (params = {}, signal) =>
    http.get('/routes', { params, ...(signal ? { signal } : {}) });

export const fetchRouteDetail = (id) =>
    http.get(`/routes/${id}`);

export const createRoute = (data) =>
    http.post('/routes', data);

export const updateRoute = (id, data) =>
    http.put(`/routes/${id}`, data);

export const deleteRoute = (id) =>
    http.delete(`/routes/${id}`);
