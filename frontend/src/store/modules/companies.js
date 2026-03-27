import {
    fetchCompanies,
    fetchCompanyCatalog,
    fetchCompanyDetail,
    fetchCompanyVehicles,
    fetchCompanyModelSummaries
} from '@/api/companies';

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
    vehiclesByCompany: {},
    modelSummariesByCompany: {},
    detailLoadingMap: {},
    vehiclesLoadingMap: {},
    summaryLoadingMap: {},
    detailFetchedAtMap: {},
    vehiclesFetchedAtMap: {},
    summaryFetchedAtMap: {}
});

const getters = {
    companyOptions: (state) =>
        state.list.map((item) => ({
            label: item.name,
            value: item.id
        })),
    companyById: (state) => (id) =>
        state.list.find((item) => item.id === id) ||
        state.catalog.find((item) => item.id === id) ||
        null,
    companyCatalogMap: (state) =>
        state.catalog.reduce((acc, company) => {
            acc[company.id] = company;
            return acc;
        }, {}),
    companyVehicles: (state) => (companyId) => state.vehiclesByCompany[companyId] || [],
    companyModelSummaries: (state) => (companyId) => state.modelSummariesByCompany[companyId] || []
};

const mutations = {
    SET_COMPANIES(state, companies) {
        state.list = companies;
    },
    SET_COMPANY_CATALOG(state, catalog) {
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
    SET_COMPANY_DETAIL(state, { companyId, detail }) {
        state.detailMap = {
            ...state.detailMap,
            [companyId]: detail
        };
        state.detailFetchedAtMap = {
            ...state.detailFetchedAtMap,
            [companyId]: Date.now()
        };
    },
    SET_COMPANY_VEHICLES(state, { companyId, vehicles }) {
        state.vehiclesByCompany = {
            ...state.vehiclesByCompany,
            [companyId]: vehicles
        };
        state.vehiclesFetchedAtMap = {
            ...state.vehiclesFetchedAtMap,
            [companyId]: Date.now()
        };
    },
    SET_COMPANY_MODEL_SUMMARIES(state, { companyId, summaries }) {
        state.modelSummariesByCompany = {
            ...state.modelSummariesByCompany,
            [companyId]: summaries
        };
        state.summaryFetchedAtMap = {
            ...state.summaryFetchedAtMap,
            [companyId]: Date.now()
        };
    },
    SET_DETAIL_LOADING(state, { companyId, loading }) {
        state.detailLoadingMap = {
            ...state.detailLoadingMap,
            [companyId]: loading
        };
    },
    SET_VEHICLES_LOADING(state, { companyId, loading }) {
        state.vehiclesLoadingMap = {
            ...state.vehiclesLoadingMap,
            [companyId]: loading
        };
    },
    SET_SUMMARY_LOADING(state, { companyId, loading }) {
        state.summaryLoadingMap = {
            ...state.summaryLoadingMap,
            [companyId]: loading
        };
    }
};

const actions = {
    async loadCompanies({ commit }, params = {}) {
        commit('SET_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const data = await fetchCompanies(params);
            commit('SET_COMPANIES', parseList(data));
            return data;
        } catch (error) {
            commit('SET_ERROR', error.message || 'Failed to load companies');
            throw error;
        } finally {
            commit('SET_LOADING', false);
        }
    },
    async loadCompanyCatalog({ state, commit }, { force = false } = {}) {
        if (state.catalog.length && !force) return state.catalog;
        commit('SET_CATALOG_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const catalog = await fetchCompanyCatalog();
            commit('SET_COMPANY_CATALOG', Array.isArray(catalog) ? catalog : []);
            return catalog;
        } catch (error) {
            commit('SET_ERROR', error.message || 'Failed to load company catalog');
            throw error;
        } finally {
            commit('SET_CATALOG_LOADING', false);
        }
    },
    async loadCompanyDetail({ state, commit }, payload) {
        const companyId = normalizeId(
            typeof payload === 'object' && payload !== null ? payload.companyId : payload
        );
        const force = Boolean(typeof payload === 'object' && payload !== null ? payload.force : false);
        if (!companyId) return null;
        if (!force && state.detailMap[companyId] && isFresh(state.detailFetchedAtMap, companyId, DETAIL_TTL_MS)) {
            return state.detailMap[companyId];
        }
        if (detailInFlightMap.has(companyId)) {
            return detailInFlightMap.get(companyId);
        }

        commit('SET_DETAIL_LOADING', { companyId, loading: true });
        commit('SET_ERROR', null);

        const request = (async () => {
            try {
                const detail = await fetchCompanyDetail(companyId);
                commit('SET_COMPANY_DETAIL', { companyId, detail });
                return detail;
            } catch (error) {
                commit('SET_ERROR', error.message || 'Failed to load company detail');
                throw error;
            } finally {
                commit('SET_DETAIL_LOADING', { companyId, loading: false });
            }
        })();

        detailInFlightMap.set(companyId, request);
        try {
            return await request;
        } finally {
            detailInFlightMap.delete(companyId);
        }
    },
    async loadCompanyVehicles(
        { state, commit },
        { companyId, params = {}, force = false } = {}
    ) {
        const normalizedCompanyId = normalizeId(companyId);
        if (!normalizedCompanyId) return [];

        if (
            !force &&
            state.vehiclesByCompany[normalizedCompanyId] &&
            isFresh(state.vehiclesFetchedAtMap, normalizedCompanyId, VEHICLES_TTL_MS)
        ) {
            return state.vehiclesByCompany[normalizedCompanyId];
        }
        if (vehiclesInFlightMap.has(normalizedCompanyId)) {
            return vehiclesInFlightMap.get(normalizedCompanyId);
        }

        commit('SET_VEHICLES_LOADING', { companyId: normalizedCompanyId, loading: true });
        commit('SET_ERROR', null);

        const request = (async () => {
            try {
                const data = await fetchCompanyVehicles(normalizedCompanyId, params);
                const list = parseList(data);
                commit('SET_COMPANY_VEHICLES', { companyId: normalizedCompanyId, vehicles: list });
                return list;
            } catch (error) {
                commit('SET_ERROR', error.message || 'Failed to load company vehicles');
                throw error;
            } finally {
                commit('SET_VEHICLES_LOADING', { companyId: normalizedCompanyId, loading: false });
            }
        })();

        vehiclesInFlightMap.set(normalizedCompanyId, request);
        try {
            return await request;
        } finally {
            vehiclesInFlightMap.delete(normalizedCompanyId);
        }
    },
    async loadCompanyModelSummaries(
        { state, commit },
        { companyId, force = false } = {}
    ) {
        const normalizedCompanyId = normalizeId(companyId);
        if (!normalizedCompanyId) return [];

        if (
            !force &&
            state.modelSummariesByCompany[normalizedCompanyId] &&
            isFresh(state.summaryFetchedAtMap, normalizedCompanyId, SUMMARY_TTL_MS)
        ) {
            return state.modelSummariesByCompany[normalizedCompanyId];
        }
        if (summaryInFlightMap.has(normalizedCompanyId)) {
            return summaryInFlightMap.get(normalizedCompanyId);
        }

        commit('SET_SUMMARY_LOADING', { companyId: normalizedCompanyId, loading: true });
        commit('SET_ERROR', null);

        const request = (async () => {
            try {
                const data = await fetchCompanyModelSummaries(normalizedCompanyId);
                const summaries = parseList(data);
                commit('SET_COMPANY_MODEL_SUMMARIES', {
                    companyId: normalizedCompanyId,
                    summaries
                });
                return summaries;
            } catch (error) {
                commit('SET_ERROR', error.message || 'Failed to load company model summaries');
                throw error;
            } finally {
                commit('SET_SUMMARY_LOADING', { companyId: normalizedCompanyId, loading: false });
            }
        })();

        summaryInFlightMap.set(normalizedCompanyId, request);
        try {
            return await request;
        } finally {
            summaryInFlightMap.delete(normalizedCompanyId);
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
