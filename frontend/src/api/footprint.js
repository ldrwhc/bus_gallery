import http from './axiosInstance';

export const fetchUserFootprint = (userId) =>
    http.get(`/users/${userId}/footprint`);
