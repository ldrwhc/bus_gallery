import http from './axiosInstance';

/**
 * 分页/检索公司
 */
export const fetchCompanies = (params = {}) =>
    http.get('/companies', { params });

/**
 * 公司详情
 */
export const fetchCompanyDetail = (companyId) =>
    http.get(`/companies/${companyId}`);

/**
 * 公司旗下车辆（含配置、图片）
 */
export const fetchCompanyVehicles = (companyId, params = {}) =>
    http.get(`/companies/${companyId}/vehicles`, { params });

/**
 * 公司分类（含旗下车型）
 */
export const fetchCompanyCatalog = () => http.get('/catalog/companies');