import {
    fetchVehicleGallery,
    fetchVehicleGalleryDetail,
    fetchVehicleComments,
    createVehicleComment
} from '@/api/vehicles';
import { fetchSnapshotByPlate } from '@/api/snapshots';

const defaultFilters = () => ({
    regionId: null,
    companyId: null,
    brandId: null,
    modelId: null,
    keyword: ''
});

const sanitizeParams = (params) => {
    const result = {};
    Object.entries(params).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '' || Number.isNaN(value)) {
            return;
        }
        result[key] = value;
    });
    return result;
};

const parseList = (payload) => {
    if (!payload) return [];
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.records)) return payload.records;
    if (Array.isArray(payload?.items)) return payload.items;
    if (Array.isArray(payload?.rows)) return payload.rows;
    if (Array.isArray(payload?.data)) return payload.data;
    return [];
};

const COMMENT_CACHE_TTL_MS = 30 * 1000;
const COMMENT_MIN_REFRESH_MS = 2000;

const state = () => ({
    gallery: [],
    galleryLoading: false,
    galleryError: null,
    filters: defaultFilters(),
    pagination: {
        page: 1,
        size: 12,
        total: 0
    },
    detailMap: {},
    detailLoadingMap: {},
    commentCache: {},
    commentLoadingMap: {},
    commentErrorMap: {},
    commentLastFetchMap: {}
});

const getters = {
    galleryCount: (state) => state.gallery.length,
    galleryFilters: (state) => state.filters,
    vehicleDetail: (state) => (id) => state.detailMap[id] || null
};

const mutations = {
    SET_GALLERY(state, gallery) {
        state.gallery = gallery;
    },
    SET_GALLERY_LOADING(state, status) {
        state.galleryLoading = status;
    },
    SET_GALLERY_ERROR(state, error) {
        state.galleryError = error;
    },
    SET_FILTERS(state, filters) {
        state.filters = { ...state.filters, ...filters };
    },
    RESET_FILTERS(state) {
        state.filters = defaultFilters();
    },
    SET_PAGINATION(state, pagination) {
        state.pagination = { ...state.pagination, ...pagination };
    },
    SET_VEHICLE_DETAIL(state, { vehicleId, detail }) {
        state.detailMap = {
            ...state.detailMap,
            [vehicleId]: detail
        };
    },
    SET_VEHICLE_FAVORITE_STATE(state, { vehicleId, liked, likeTotal, likes }) {
        const existing = state.detailMap[vehicleId] || {};
        state.detailMap = {
            ...state.detailMap,
            [vehicleId]: {
                ...existing,
                favorite: {
                    liked,
                    likeTotal,
                    likes
                }
            }
        };
    },
    SET_DETAIL_LOADING(state, { vehicleId, loading }) {
        state.detailLoadingMap = {
            ...state.detailLoadingMap,
            [vehicleId]: loading
        };
    },
    SET_COMMENT_CACHE(state, { vehicleId, page, size, records, total, fetchedAt }) {
        state.commentCache = {
            ...state.commentCache,
            [vehicleId]: {
                vehicleId,
                page,
                size,
                records: Array.isArray(records) ? records : [],
                total: total ?? 0,
                fetchedAt: fetchedAt ?? Date.now()
            }
        };
    },
    SET_COMMENT_LOADING(state, { vehicleId, loading }) {
        state.commentLoadingMap = {
            ...state.commentLoadingMap,
            [vehicleId]: loading
        };
    },
    SET_COMMENT_ERROR(state, { vehicleId, error }) {
        state.commentErrorMap = {
            ...state.commentErrorMap,
            [vehicleId]: error
        };
    },
    SET_COMMENT_LAST_FETCH(state, { vehicleId, ts }) {
        state.commentLastFetchMap = {
            ...state.commentLastFetchMap,
            [vehicleId]: ts
        };
    }
};

const actions = {
    async loadVehicleGallery({ state, commit }, params = {}) {
        const { page: pageParam, size: sizeParam, ...filterOverrides } = params;
        const page = pageParam ?? state.pagination.page ?? 1;
        const size = sizeParam ?? state.pagination.size ?? 12;
        const filters = { ...state.filters, ...filterOverrides };
        commit('SET_FILTERS', filters);
        commit('SET_GALLERY_LOADING', true);
        commit('SET_GALLERY_ERROR', null);
        try {
            const response = await fetchVehicleGallery({
                page,
                size,
                ...sanitizeParams(filters)
            });
            const list = parseList(response);
            const total =
                response?.total ??
                response?.pagination?.total ??
                response?.meta?.total ??
                list.length;
            commit('SET_GALLERY', list);
            commit('SET_PAGINATION', { page, size, total });
            return list;
        } catch (error) {
            commit('SET_GALLERY_ERROR', error.message || '加载车辆图库失败');
            throw error;
        } finally {
            commit('SET_GALLERY_LOADING', false);
        }
    },
    async updateGalleryFilters({ commit, dispatch, state }, filters = {}) {
        commit('SET_FILTERS', { ...state.filters, ...filters });
        return dispatch('loadVehicleGallery', { page: 1 });
    },
    async resetGalleryFilters({ commit, dispatch }) {
        commit('RESET_FILTERS');
        return dispatch('loadVehicleGallery', { page: 1 });
    },
    async loadVehicleDetail({ state, commit }, payload) {
        const vehicleId = typeof payload === 'object' && payload !== null ? payload.vehicleId : payload;
        const force = typeof payload === 'object' && payload !== null ? Boolean(payload.force) : false;
        const plateFromPayload =
            typeof payload === 'object' && payload !== null ? (payload.plateNumber || '').trim() : '';
        if (!vehicleId) return null;
        if (state.detailLoadingMap[vehicleId]) return state.detailMap[vehicleId] || null;
        if (!force && state.detailMap[vehicleId]) return state.detailMap[vehicleId];
        commit('SET_DETAIL_LOADING', { vehicleId, loading: true });
        try {
            const findPlateByVehicleId = () => {
                if (plateFromPayload) return plateFromPayload;
                const direct = state.detailMap[vehicleId]?.vehicle?.plateNumber;
                if (direct) return direct;
                for (const item of state.gallery || []) {
                    const vid = item?.vehicle?.id || item?.vehicleId;
                    if (vid === vehicleId) {
                        return item?.vehicle?.plateNumber || item?.plateNumber || null;
                    }
                }
                return null;
            };

            const applySnapshotByPlate = async (plate) => {
                if (!plate) return false;
                try {
                    const snapshot = await fetchSnapshotByPlate(plate);
                    if (snapshot && Array.isArray(snapshot.variants) && snapshot.variants.length) {
                        snapshot.variants.forEach((variant) => {
                            const id = variant?.vehicle?.id;
                            if (!id) return;
                            const detail = {
                                ...snapshot,
                                vehicle: variant.vehicle,
                                vehicleConfig: variant.vehicleConfig,
                                images: variant.images || [],
                                variants: snapshot.variants
                            };
                            commit('SET_VEHICLE_DETAIL', { vehicleId: id, detail });
                        });
                        return true;
                    }
                } catch (e) {
                    // fallback to normal detail endpoint
                }
                return false;
            };

            const plate = findPlateByVehicleId();
            const hasSnapshot = await applySnapshotByPlate(plate);
            if (hasSnapshot) return state.detailMap[vehicleId] || null;

            const detail = await fetchVehicleGalleryDetail(vehicleId);
            const detailPlate = detail?.vehicle?.plateNumber;
            const snapshotApplied = await applySnapshotByPlate(detailPlate);
            if (!snapshotApplied) {
                commit('SET_VEHICLE_DETAIL', { vehicleId, detail });
            }
            return state.detailMap[vehicleId] || detail;
        } finally {
            commit('SET_DETAIL_LOADING', { vehicleId, loading: false });
        }
    },
    async loadVehicleComments({ state, commit }, { vehicleId, page = 1, size = 50, force = false } = {}) {
        if (!vehicleId) return { records: [], total: 0 };
        const cached = state.commentCache[vehicleId];
        const now = Date.now();
        const isFresh =
            cached &&
            cached.page === page &&
            cached.size === size &&
            now - (cached.fetchedAt || 0) < COMMENT_CACHE_TTL_MS;
        if (isFresh && !force) {
            return cached;
        }
        const lastFetch = state.commentLastFetchMap[vehicleId] || 0;
        if (state.commentLoadingMap[vehicleId]) {
            return cached || { records: [], total: 0 };
        }
        if (force && now - lastFetch < COMMENT_MIN_REFRESH_MS) {
            return cached || { records: [], total: 0 };
        }
        commit('SET_COMMENT_LOADING', { vehicleId, loading: true });
        commit('SET_COMMENT_ERROR', { vehicleId, error: null });
        commit('SET_COMMENT_LAST_FETCH', { vehicleId, ts: now });
        try {
            const resp = await fetchVehicleComments(vehicleId, { page, size });
            const records = resp?.records || resp?.items || resp?.data || resp || [];
            const total = resp?.total ?? records.length ?? 0;
            commit('SET_COMMENT_CACHE', { vehicleId, page, size, records, total, fetchedAt: now });
            return state.commentCache[vehicleId];
        } catch (error) {
            commit('SET_COMMENT_ERROR', { vehicleId, error: error?.message || '加载评论失败' });
            if (cached) return cached;
            throw error;
        } finally {
            commit('SET_COMMENT_LOADING', { vehicleId, loading: false });
        }
    },
    async addVehicleComment({ state, commit }, { vehicleId, content }) {
        if (!vehicleId || !content) {
            throw new Error('vehicleId or content missing');
        }
        const comment = await createVehicleComment(vehicleId, content);
        const cached = state.commentCache[vehicleId];
        const nextTotal = (cached?.total ?? 0) + 1;
        const page = cached?.page ?? 1;
        const size = cached?.size ?? 50;
        const existing = cached?.records || [];
        const hasComment = existing.some((item) => item?.id && item.id === comment?.id);
        const nextRecords = page === 1 ? [comment, ...existing.filter((item) => item?.id !== comment?.id)] : existing;
        const trimmed = page === 1 ? nextRecords.slice(0, size) : existing;
        commit('SET_COMMENT_CACHE', {
            vehicleId,
            page,
            size,
            records: hasComment ? existing : trimmed,
            total: nextTotal,
            fetchedAt: Date.now()
        });
        return comment;
    }
};

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
};
