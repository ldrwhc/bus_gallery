import {
    fetchCompanies,
    fetchCompanyCatalog,
    fetchCompanyDetail,
    fetchCompanyVehicles
} from '@/api/companies';

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
    vehiclesByCompany: {},
    detailLoadingMap: {},
    vehiclesLoadingMap: {}
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
    companyVehicles: (state) => (companyId) =>
        state.vehiclesByCompany[companyId] || []
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
    },
    SET_COMPANY_VEHICLES(state, { companyId, vehicles }) {
        state.vehiclesByCompany = {
            ...state.vehiclesByCompany,
            [companyId]: vehicles
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
            commit('SET_ERROR', error.message || '加载公司失败');
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
            commit('SET_ERROR', error.message || '加载公司目录失败');
            throw error;
        } finally {
            commit('SET_CATALOG_LOADING', false);
        }
    },
    async loadCompanyDetail({ state, commit }, companyId) {
        if (!companyId) return null;
        if (state.detailMap[companyId]) return state.detailMap[companyId];
        commit('SET_DETAIL_LOADING', { companyId, loading: true });
        commit('SET_ERROR', null);
        try {
            const detail = await fetchCompanyDetail(companyId);
            commit('SET_COMPANY_DETAIL', { companyId, detail });
            return detail;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载公司详情失败');
            throw error;
        } finally {
            commit('SET_DETAIL_LOADING', { companyId, loading: false });
        }
    },
    async loadCompanyVehicles(
        { state, commit },
        { companyId, params = {}, force = false } = {}
    ) {
        if (!companyId) return [];
        if (state.vehiclesByCompany[companyId] && !force) {
            return state.vehiclesByCompany[companyId];
        }
        commit('SET_VEHICLES_LOADING', { companyId, loading: true });
        commit('SET_ERROR', null);
        try {
            const data = await fetchCompanyVehicles(companyId, params);
            const list = parseList(data);
            commit('SET_COMPANY_VEHICLES', { companyId, vehicles: list });
            return list;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载公司车辆失败');
            throw error;
        } finally {
            commit('SET_VEHICLES_LOADING', { companyId, loading: false });
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
