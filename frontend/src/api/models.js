import http from './axiosInstance';

/**
 * 车型列表
 */
export const fetchModels = (params = {}) =>
    http.get('/models', { params });

/**
 * 车型详情
 */
export const fetchModelDetail = (modelId) =>
    http.get(`/models/${modelId}`);

/**
 * 某车型在不同公司投放的车辆
 */
export const fetchModelVehicles = (modelId, params = {}) =>
    http.get(`/models/${modelId}/vehicles`, { params });

/**
 * 型号分类视图（含运营公司）
 */
export const fetchModelCatalog = () => http.get('/catalog/models');