import http from './axiosInstance';

/**
 * 品牌列表（支持分页、关键词）
 */
export const fetchBrands = (params = {}) =>
    http.get('/brands', { params });

/**
 * 品牌分类视图（含旗下车型）
 */
export const fetchBrandCatalog = () => http.get('/catalog/brands');