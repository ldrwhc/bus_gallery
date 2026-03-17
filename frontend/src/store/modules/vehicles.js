import {
    fetchVehicleGallery,
    fetchVehicleGalleryDetail
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
    detailLoadingMap: {}
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
    async loadVehicleDetail({ state, commit }, vehicleId) {
        if (!vehicleId) return null;
        if (state.detailLoadingMap[vehicleId]) return state.detailMap[vehicleId] || null;
        if (state.detailMap[vehicleId]) return state.detailMap[vehicleId];
        commit('SET_DETAIL_LOADING', { vehicleId, loading: true });
        try {
            const findPlateByVehicleId = () => {
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

            const plate = findPlateByVehicleId();
            if (plate) {
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
                        return state.detailMap[vehicleId] || null;
                    }
                } catch (e) {
                    // fallback to normal detail endpoint
                }
            }

            const detail = await fetchVehicleGalleryDetail(vehicleId);
            commit('SET_VEHICLE_DETAIL', { vehicleId, detail });
            return detail;
        } finally {
            commit('SET_DETAIL_LOADING', { vehicleId, loading: false });
        }
    }
};

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
};
