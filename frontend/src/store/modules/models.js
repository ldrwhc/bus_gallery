import {
    fetchModels,
    fetchModelCatalog,
    fetchModelDetail,
    fetchModelVehicles,
    fetchModelCompanySummaries
} from '@/api/models';

const DETAIL_TTL_MS = 20 * 1000;
const VEHICLES_TTL_MS = 30 * 1000;
const SUMMARY_TTL_MS = 30 * 1000;

const detailInFlightMap = new Map();
const vehiclesInFlightMap = new Map();
const summaryInFlightMap = new Map();

const parseList = (payload) => {
    if (!payload) return [];
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.records)) return payload.records;
    if (Array.isArray(payload?.items)) return payload.items;
    if (Array.isArray(payload?.rows)) return payload.rows;
    if (Array.isArray(payload?.data)) return payload.data;
    return [];
};

const normalizeId = (value) => {
    const numeric = Number(value);
    if (!Number.isInteger(numeric) || numeric <= 0) return null;
    return numeric;
};

const isFresh = (fetchedAtMap, key, ttlMs) => {
    const ts = fetchedAtMap?.[key];
    return Number.isFinite(ts) && Date.now() - ts < ttlMs;
};

const state = () => ({
    list: [],
    catalog: [],
    loading: false,
    catalogLoading: false,
    error: null,
    detailMap: {},
    vehiclesByModel: {},
    companySummariesByModel: {},
    detailLoadingMap: {},
    vehiclesLoadingMap: {},
    summaryLoadingMap: {},
    detailFetchedAtMap: {},
    vehiclesFetchedAtMap: {},
    summaryFetchedAtMap: {}
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
    modelVehicles: (state) => (modelId) => state.vehiclesByModel[modelId] || [],
    modelCompanySummaries: (state) => (modelId) => state.companySummariesByModel[modelId] || []
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
        state.detailFetchedAtMap = {
            ...state.detailFetchedAtMap,
            [modelId]: Date.now()
        };
    },
    SET_MODEL_VEHICLES(state, { modelId, vehicles }) {
        state.vehiclesByModel = {
            ...state.vehiclesByModel,
            [modelId]: vehicles
        };
        state.vehiclesFetchedAtMap = {
            ...state.vehiclesFetchedAtMap,
            [modelId]: Date.now()
        };
    },
    SET_MODEL_COMPANY_SUMMARIES(state, { modelId, summaries }) {
        state.companySummariesByModel = {
            ...state.companySummariesByModel,
            [modelId]: summaries
        };
        state.summaryFetchedAtMap = {
            ...state.summaryFetchedAtMap,
            [modelId]: Date.now()
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
    },
    SET_SUMMARY_LOADING(state, { modelId, loading }) {
        state.summaryLoadingMap = {
            ...state.summaryLoadingMap,
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
            commit('SET_ERROR', error.message || 'Failed to load models');
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
            commit('SET_ERROR', error.message || 'Failed to load model catalog');
            throw error;
        } finally {
            commit('SET_CATALOG_LOADING', false);
        }
    },
    async loadModelDetail({ state, commit }, payload) {
        const modelId = normalizeId(
            typeof payload === 'object' && payload !== null ? payload.modelId : payload
        );
        const force = Boolean(typeof payload === 'object' && payload !== null ? payload.force : false);
        if (!modelId) return null;

        if (!force && state.detailMap[modelId] && isFresh(state.detailFetchedAtMap, modelId, DETAIL_TTL_MS)) {
            return state.detailMap[modelId];
        }
        if (detailInFlightMap.has(modelId)) {
            return detailInFlightMap.get(modelId);
        }

        commit('SET_DETAIL_LOADING', { modelId, loading: true });
        commit('SET_ERROR', null);

        const request = (async () => {
            try {
                const detail = await fetchModelDetail(modelId);
                commit('SET_MODEL_DETAIL', { modelId, detail });
                return detail;
            } catch (error) {
                commit('SET_ERROR', error.message || 'Failed to load model detail');
                throw error;
            } finally {
                commit('SET_DETAIL_LOADING', { modelId, loading: false });
            }
        })();

        detailInFlightMap.set(modelId, request);
        try {
            return await request;
        } finally {
            detailInFlightMap.delete(modelId);
        }
    },
    async loadModelVehicles(
        { state, commit },
        { modelId, params = {}, force = false } = {}
    ) {
        const normalizedModelId = normalizeId(modelId);
        if (!normalizedModelId) return [];

        if (
            !force &&
            state.vehiclesByModel[normalizedModelId] &&
            isFresh(state.vehiclesFetchedAtMap, normalizedModelId, VEHICLES_TTL_MS)
        ) {
            return state.vehiclesByModel[normalizedModelId];
        }
        if (vehiclesInFlightMap.has(normalizedModelId)) {
            return vehiclesInFlightMap.get(normalizedModelId);
        }

        commit('SET_VEHICLES_LOADING', { modelId: normalizedModelId, loading: true });
        commit('SET_ERROR', null);

        const request = (async () => {
            try {
                const data = await fetchModelVehicles(normalizedModelId, params);
                const list = parseList(data);
                commit('SET_MODEL_VEHICLES', { modelId: normalizedModelId, vehicles: list });
                return list;
            } catch (error) {
                commit('SET_ERROR', error.message || 'Failed to load model vehicles');
                throw error;
            } finally {
                commit('SET_VEHICLES_LOADING', { modelId: normalizedModelId, loading: false });
            }
        })();

        vehiclesInFlightMap.set(normalizedModelId, request);
        try {
            return await request;
        } finally {
            vehiclesInFlightMap.delete(normalizedModelId);
        }
    },
    async loadModelCompanySummaries(
        { state, commit },
        { modelId, force = false } = {}
    ) {
        const normalizedModelId = normalizeId(modelId);
        if (!normalizedModelId) return [];

        if (
            !force &&
            state.companySummariesByModel[normalizedModelId] &&
            isFresh(state.summaryFetchedAtMap, normalizedModelId, SUMMARY_TTL_MS)
        ) {
            return state.companySummariesByModel[normalizedModelId];
        }
        if (summaryInFlightMap.has(normalizedModelId)) {
            return summaryInFlightMap.get(normalizedModelId);
        }

        commit('SET_SUMMARY_LOADING', { modelId: normalizedModelId, loading: true });
        commit('SET_ERROR', null);

        const request = (async () => {
            try {
                const data = await fetchModelCompanySummaries(normalizedModelId);
                const summaries = parseList(data);
                commit('SET_MODEL_COMPANY_SUMMARIES', {
                    modelId: normalizedModelId,
                    summaries
                });
                return summaries;
            } catch (error) {
                commit('SET_ERROR', error.message || 'Failed to load model company summaries');
                throw error;
            } finally {
                commit('SET_SUMMARY_LOADING', { modelId: normalizedModelId, loading: false });
            }
        })();

        summaryInFlightMap.set(normalizedModelId, request);
        try {
            return await request;
        } finally {
            summaryInFlightMap.delete(normalizedModelId);
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
