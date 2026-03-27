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
        size: response?.size ?? params.size ?? 12,
        nextLaunch: response?.nextLaunch ?? null,
        nextId: response?.nextId ?? null
    }));

export const fetchManageVehiclePage = (params = {}) =>
    http.get('/vehicles/manage', { params }).then((response) => ({
        records: normalizeRecords(response),
        total: response?.total ?? 0,
        size: response?.size ?? params.size ?? 12,
        nextLaunch: response?.nextLaunch ?? null,
        nextId: response?.nextId ?? null
    }));

export const fetchVehicleGalleryDetail = (vehicleId) =>
    http.get(`/vehicles/${vehicleId}`).then(normalizeDetail);

export const trackVehicleView = (vehicleId) =>
    http.post(`/vehicles/${vehicleId}/view`);

export const createVehicle = (payload) => http.post('/vehicles', payload).then(normalizeDetail);

export const updateVehicle = (vehicleId, payload) =>
    http.put(`/vehicles/${vehicleId}`, payload).then(normalizeDetail);

export const deleteVehicle = (vehicleId) =>
    http.delete(`/vehicles/${vehicleId}`);

export const batchDeleteVehicles = (ids) =>
    http.post('/vehicles/batch-delete', { ids });

/**
 * 统一的 “上传图片 + 建档” 接口
 */
export const uploadVehicleWithImage = (vehiclePayload, file, idempotencyKey) => {
    const formData = new FormData();
    formData.append('file', file);
    // Keep payload as plain text so Spring @RequestPart("payload") String can parse it reliably.
    formData.append('payload', JSON.stringify(vehiclePayload));

    const config = {
        timeout: 60000
    };
    const normalizedIdempotencyKey = String(idempotencyKey || '').trim();
    if (normalizedIdempotencyKey) {
        // Custom headers trigger CORS preflight in cross-origin deployments; only send when needed.
        config.headers = { 'Idempotency-Key': normalizedIdempotencyKey };
    }
    return http.post('/upload', formData, config);
};

export const initVehicleChunkUpload = ({ fileName, contentType, totalSize, chunkSize, totalChunks }) =>
    http.post('/upload/chunk/init', {
        fileName,
        contentType,
        totalSize,
        chunkSize,
        totalChunks
    });

export const uploadVehicleChunkPart = (uploadId, index, chunk, onUploadProgress) => {
    const formData = new FormData();
    formData.append('file', chunk, `chunk-${index}.bin`);
    return http.post(`/upload/chunk/${uploadId}/part`, formData, {
        params: { index },
        timeout: 120000,
        onUploadProgress
    });
};

export const fetchVehicleChunkProgress = (uploadId) =>
    http.get(`/upload/chunk/${uploadId}/progress`);

export const completeVehicleChunkUpload = (uploadId, vehiclePayload, idempotencyKey) => {
    const config = {
        timeout: 120000
    };
    const normalizedIdempotencyKey = String(idempotencyKey || '').trim();
    if (normalizedIdempotencyKey) {
        config.headers = { 'Idempotency-Key': normalizedIdempotencyKey };
    }
    return http.post(
        `/upload/chunk/${uploadId}/complete`,
        { payload: vehiclePayload },
        config
    );
};

export const cancelVehicleChunkUpload = (uploadId) =>
    http.delete(`/upload/chunk/${uploadId}`);

export const fetchVehicleComments = (vehicleId, params = {}) =>
    http.get(`/vehicles/${vehicleId}/comments`, { params });

export const createVehicleComment = (vehicleId, content) =>
    http.post(`/vehicles/${vehicleId}/comments`, { content });

export const deleteVehicleComment = (vehicleId, commentId) =>
    http.delete(`/vehicles/${vehicleId}/comments/${commentId}`);

export const setFavorite = (vehicleId, liked) =>
    http.put(`/favorites/${vehicleId}`, { liked: Boolean(liked) });

export const fetchFavoriteSummary = (vehicleId) => http.get(`/favorites/${vehicleId}/summary`);

export const fetchFavorites = (payload = {}) => {
    const params = typeof payload === 'object' && payload !== null ? payload : { userId: payload };
    const page = Number(params.page) > 0 ? Number(params.page) : 1;
    const size = Number(params.size) > 0 ? Number(params.size) : 12;
    const query = {
        page,
        size,
        ...(params.userId ? { userId: params.userId } : {})
    };
    return http.get('/favorites', { params: query }).then((response) => {
        const records = normalizeRecords(response);
        return {
            records,
            total: response?.total ?? records.length,
            page: response?.page ?? page,
            size: response?.size ?? size
        };
    });
};

export const fetchVehiclesByPlate = (plateNumber) =>
    http.get(`/vehicles/plate/${encodeURIComponent(plateNumber)}`);
