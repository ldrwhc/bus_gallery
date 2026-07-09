import http from './axiosInstance';

export const searchAll = (keyword, scope = 'all') =>
    http.get('/search', { params: { keyword, scope } });

export const searchSuggest = (keyword) =>
    http.get('/search/suggest', { params: { keyword } });
