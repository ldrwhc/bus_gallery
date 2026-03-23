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

export const fetchAdminCompanies = () => http.get('/admin/tables/companies');
export const createAdminCompany = (payload) => http.post('/admin/tables/companies', payload);
export const updateAdminCompany = (id, payload) => http.put(`/admin/tables/companies/${id}`, payload);
export const deleteAdminCompany = (id) => http.delete(`/admin/tables/companies/${id}`);

export const fetchAdminBrands = () => http.get('/admin/tables/brands');
export const createAdminBrand = (payload) => http.post('/admin/tables/brands', payload);
export const updateAdminBrand = (id, payload) => http.put(`/admin/tables/brands/${id}`, payload);
export const deleteAdminBrand = (id) => http.delete(`/admin/tables/brands/${id}`);

export const fetchAdminModels = () => http.get('/admin/tables/models');
export const createAdminModel = (payload) => http.post('/admin/tables/models', payload);
export const updateAdminModel = (id, payload) => http.put(`/admin/tables/models/${id}`, payload);
export const deleteAdminModel = (id) => http.delete(`/admin/tables/models/${id}`);

export const fetchAdminSuspectImages = () => http.get('/admin/images/suspects');
export const cleanupAdminSuspectImages = (imageIds) =>
    http.post('/admin/images/suspects/cleanup', imageIds ? { imageIds } : {});
