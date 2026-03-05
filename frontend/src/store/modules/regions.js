import { fetchRegions, fetchRegionCatalog } from '@/api/regions';

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
    regionOptions: (state) =>
        state.list.map((item) => ({
            label: item.name,
            value: item.id
        })),
    regionById: (state) => (id) =>
        state.list.find((item) => item.id === id) || null,
    regionCatalogMap: (state) =>
        state.catalog.reduce((acc, region) => {
            acc[region.id] = region;
            return acc;
        }, {})
};

const mutations = {
    SET_REGIONS(state, regions) {
        state.list = regions;
    },
    SET_REGION_CATALOG(state, catalog) {
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
    async loadRegions({ commit }, params = {}) {
        commit('SET_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const list = await fetchRegions(params);
            commit('SET_REGIONS', parseList(list));
            return list;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载地区失败');
            throw error;
        } finally {
            commit('SET_LOADING', false);
        }
    },
    async loadRegionCatalog({ state, commit }, { force = false } = {}) {
        if (state.catalog.length && !force) return state.catalog;
        commit('SET_CATALOG_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const catalog = await fetchRegionCatalog();
            commit('SET_REGION_CATALOG', Array.isArray(catalog) ? catalog : []);
            return catalog;
        } catch (error) {
            commit('SET_ERROR', error.message || '加载地区分类失败');
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