import {
    fetchModels,
    fetchModelCatalog,
    fetchModelDetail,
    fetchModelVehicles
} from '@/api/models';

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
    list: [],
    catalog: [],
    loading: false,
    catalogLoading: false,
    error: null,
    detailMap: {},
    vehiclesByModel: {},
    detailLoadingMap: {},
    vehiclesLoadingMap: {}
});

const getters = {
    modelOptions: (state) =>
        state.list.map((item) => ({
            label: item.name || item.modelName || item.code,
            value: item.id
        })),
    modelById: (state) => (id) =>
        state.list.find((item) => item.id === id) ||
        state.catalog.find((item) => item.id === id) ||
        null,
    modelVehicles: (state) => (modelId) =>
        state.vehiclesByModel[modelId] || []
};

const mutations = {
    SET_MODELS(state, models) {
        state.list = models;
    },
    SET_MODEL_CATALOG(state, catalog) {
        state.catalog = catalog;
    },
    SET_LOADING(state, status) {
        state.loading = status;
    },
    SET_CATALOG_LOADING(state, status) {
        state.catalogLoading = status;
    },
    SET_ERROR(state, error) {
        state.error = error;
    },
    SET_MODEL_DETAIL(state, { modelId, detail }) {
        state.detailMap = {
            ...state.detailMap,
            [modelId]: detail
        };
    },
    SET_MODEL_VEHICLES(state, { modelId, vehicles }) {
        state.vehiclesByModel = {
            ...state.vehiclesByModel,
            [modelId]: vehicles
        };
    },
    SET_DETAIL_LOADING(state, { modelId, loading }) {
        state.detailLoadingMap = {
            ...state.detailLoadingMap,
            [modelId]: loading
        };
    },
    SET_VEHICLES_LOADING(state, { modelId, loading }) {
        state.vehiclesLoadingMap = {
            ...state.vehiclesLoadingMap,
            [modelId]: loading
        };
    }
};

const actions = {
    async loadModels({ commit }, params = {}) {
        commit('SET_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const list = await fetchModels(params);
            commit('SET_MODELS', parseList(list));
            return list;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载车型失败');
            throw error;
        } finally {
            commit('SET_LOADING', false);
        }
    },
    async loadModelCatalog({ state, commit }, { force = false } = {}) {
        if (state.catalog.length && !force) return state.catalog;
        commit('SET_CATALOG_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const catalog = await fetchModelCatalog();
            commit('SET_MODEL_CATALOG', Array.isArray(catalog) ? catalog : []);
            return catalog;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载型号分类失败');
            throw error;
        } finally {
            commit('SET_CATALOG_LOADING', false);
        }
    },
    async loadModelDetail({ state, commit }, modelId) {
        if (!modelId) return null;
        if (state.detailMap[modelId]) return state.detailMap[modelId];
        commit('SET_DETAIL_LOADING', { modelId, loading: true });
        commit('SET_ERROR', null);
        try {
            const detail = await fetchModelDetail(modelId);
            commit('SET_MODEL_DETAIL', { modelId, detail });
            return detail;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载车型详情失败');
            throw error;
        } finally {
            commit('SET_DETAIL_LOADING', { modelId, loading: false });
        }
    },
    async loadModelVehicles(
        { state, commit },
        { modelId, params = {}, force = false } = {}
    ) {
        if (!modelId) return [];
        if (state.vehiclesByModel[modelId] && !force) {
            return state.vehiclesByModel[modelId];
        }
        commit('SET_VEHICLES_LOADING', { modelId, loading: true });
        commit('SET_ERROR', null);
        try {
            const data = await fetchModelVehicles(modelId, params);
            const list = parseList(data);
            commit('SET_MODEL_VEHICLES', { modelId, vehicles: list });
            return list;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载车型车辆失败');
            throw error;
        } finally {
            commit('SET_VEHICLES_LOADING', { modelId, loading: false });
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