import http from './axiosInstance';

const normalizeRecords = (response) => {
    if (!response) return [];
    if (Array.isArray(response.records)) return response.records;
    if (Array.isArray(response.items)) return response.items;
    if (Array.isArray(response.data)) return response.data;
    if (Array.isArray(response.rows)) return response.rows;
    if (Array.isArray(response)) return response;
    return [];
};

const normalizeDetail = (response) => {
    if (!response) return null;
    if (response.vehicle) {
        return response;
    }
    return {
        vehicle: response,
        vehicleConfig: null,
        images: []
    };
};

export const fetchVehicleGallery = (params = {}) =>
    http.get('/vehicles', { params }).then((response) => ({
        records: normalizeRecords(response),
        total: response?.total ?? response?.pagination?.total ?? response?.meta?.total ?? 0,
        page: response?.page ?? params.page ?? 1,
        size: response?.size ?? params.size ?? 12
    }));

export const fetchVehicleGalleryDetail = (vehicleId) =>
    http.get(`/vehicles/${vehicleId}`).then(normalizeDetail);

export const createVehicle = (payload) => http.post('/vehicles', payload);

export const updateVehicle = (vehicleId, payload) =>
    http.put(`/vehicles/${vehicleId}`, payload);

export const deleteVehicle = (vehicleId) =>
    http.delete(`/vehicles/${vehicleId}`);

/**
 * 统一的 “上传图片 + 建档” 接口
 */
export const uploadVehicleWithImage = (vehiclePayload, file, idempotencyKey) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append(
        'payload',
        new Blob([JSON.stringify(vehiclePayload)], { type: 'application/json' })
    );

    return http.post('/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
            'Idempotency-Key': idempotencyKey || crypto.randomUUID?.() || Date.now().toString()
        }
    });
};

export const fetchVehicleComments = (vehicleId, params = {}) =>
    http.get(`/vehicles/${vehicleId}/comments`, { params });

export const createVehicleComment = (vehicleId, content) =>
    http.post(`/vehicles/${vehicleId}/comments`, { content });

export const toggleFavorite = (vehicleId) => http.post(`/favorites/${vehicleId}/toggle`);

export const fetchFavoriteSummary = (vehicleId) => http.get(`/favorites/${vehicleId}/summary`);

export const fetchFavorites = (userId) =>
    http.get('/favorites', { params: userId ? { userId } : undefined });

export const fetchVehiclesByPlate = (plateNumber) => http.get(`/vehicles/plate/${plateNumber}`);
