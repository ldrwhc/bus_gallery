import http from './axiosInstance';

/**
 * 分页/检索地区列表
 * @param {Object} params { page, size, keyword ... }
 */
export const fetchRegions = (params = {}) =>
    http.get('/regions', { params });

/**
 * 地区分类视图，包含地区下属公司列表
 */
export const fetchRegionCatalog = () => http.get('/catalog/regions');