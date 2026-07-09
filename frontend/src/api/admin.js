import http from './axiosInstance';

export const fetchAdminOverview = () => http.get('/admin/overview');

export const fetchAdminUsers = () => http.get('/admin/users');

export const updateAdminUserRole = (userId, role, reviewRegionId) =>
    http.put(`/admin/users/${userId}/role`, { role, reviewRegionId });

export const fetchAdminSubmissionIds = (status = 'PENDING') =>
    http.get('/admin/submissions', { params: { status } });

export const fetchAdminRegions = () => http.get('/admin/tables/regions');
export const createAdminRegion = (payload) => http.post('/admin/tables/regions', payload);
export const updateAdminRegion = (id, payload) => http.put(`/admin/tables/regions/${id}`, payload);
export const deleteAdminRegion = (id) => http.delete(`/admin/tables/regions/${id}`);
export const batchDeleteAdminRegions = (ids) => http.post('/admin/tables/regions/batch-delete', { ids });

export const fetchAdminCompanies = () => http.get('/admin/tables/companies');
export const createAdminCompany = (payload) => http.post('/admin/tables/companies', payload);
export const updateAdminCompany = (id, payload) => http.put(`/admin/tables/companies/${id}`, payload);
export const deleteAdminCompany = (id) => http.delete(`/admin/tables/companies/${id}`);
export const batchDeleteAdminCompanies = (ids) => http.post('/admin/tables/companies/batch-delete', { ids });

export const fetchAdminBrands = () => http.get('/admin/tables/brands');
export const createAdminBrand = (payload) => http.post('/admin/tables/brands', payload);
export const updateAdminBrand = (id, payload) => http.put(`/admin/tables/brands/${id}`, payload);
export const deleteAdminBrand = (id) => http.delete(`/admin/tables/brands/${id}`);
export const batchDeleteAdminBrands = (ids) => http.post('/admin/tables/brands/batch-delete', { ids });

export const fetchAdminModels = () => http.get('/admin/tables/models');
export const createAdminModel = (payload) => http.post('/admin/tables/models', payload);
export const updateAdminModel = (id, payload) => http.put(`/admin/tables/models/${id}`, payload);
export const deleteAdminModel = (id) => http.delete(`/admin/tables/models/${id}`);
export const batchDeleteAdminModels = (ids) => http.post('/admin/tables/models/batch-delete', { ids });

export const fetchAdminComments = (params = {}) => http.get('/admin/comments', { params });
export const deleteAdminComment = (commentId) => http.delete(`/admin/comments/${commentId}`);
export const batchDeleteAdminComments = (ids) => http.post('/admin/comments/batch-delete', { ids });

export const fetchAdminSuspectImages = () => http.get('/admin/images/suspects');
export const cleanupAdminSuspectImages = (imageIds) =>
    http.post('/admin/images/suspects/cleanup', imageIds ? { imageIds } : {});

export const fetchAdminRoutes = (params = {}) =>
    http.get('/admin/tables/routes', { params });
export const createAdminRoute = (payload) =>
    http.post('/admin/tables/routes', payload);
export const updateAdminRoute = (id, payload) =>
    http.put(`/admin/tables/routes/${id}`, payload);
export const deleteAdminRoute = (id) =>
    http.delete(`/admin/tables/routes/${id}`);
export const batchDeleteAdminRoutes = (ids) =>
    http.post('/admin/tables/routes/batch-delete', { ids });
