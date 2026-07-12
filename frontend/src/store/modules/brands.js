import { fetchBrands, fetchBrandCatalog } from '@/api/brands';

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
    error: null
});

const getters = {
    brandOptions: (state) =>
        state.list.map((item) => ({
            label: item.chnName ? `${item.chnName} [${item.name}]` : item.name,
            value: item.id
        })),
    brandById: (state) => (id) =>
        state.list.find((item) => item.id === id) ||
        state.catalog.find((item) => item.id === id) ||
        null
};

const mutations = {
    SET_BRANDS(state, brands) {
        state.list = brands;
    },
    SET_BRAND_CATALOG(state, catalog) {
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
    }
};

const actions = {
    async loadBrands({ commit }, params = {}) {
        commit('SET_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const list = await fetchBrands(params);
            commit('SET_BRANDS', parseList(list));
            return list;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载品牌失败');
            throw error;
        } finally {
            commit('SET_LOADING', false);
        }
    },
    async loadBrandCatalog({ state, commit }, { force = false } = {}) {
        if (state.catalog.length && !force) return state.catalog;
        commit('SET_CATALOG_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const catalog = await fetchBrandCatalog();
            commit('SET_BRAND_CATALOG', Array.isArray(catalog) ? catalog : []);
            return catalog;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载品牌目录失败');
            throw error;
        } finally {
            commit('SET_CATALOG_LOADING', false);
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
