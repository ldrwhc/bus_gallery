import { fetchRoutes } from '@/api/routes';

export default {
    namespaced: true,
    state: () => ({
        list: [],
        loading: false,
        error: null
    }),
    mutations: {
        SET_ROUTES(state, routes) {
            state.list = routes;
        },
        SET_LOADING(state, val) {
            state.loading = val;
        },
        SET_ERROR(state, err) {
            state.error = err;
        }
    },
    actions: {
        async loadRoutes({ commit }, params = {}) {
            commit('SET_LOADING', true);
            commit('SET_ERROR', null);
            try {
                const resp = await fetchRoutes(params);
                commit('SET_ROUTES', Array.isArray(resp?.records) ? resp.records : (Array.isArray(resp) ? resp : []));
            } catch (e) {
                commit('SET_ERROR', e?.message || '加载线路失败');
                commit('SET_ROUTES', []);
            } finally {
                commit('SET_LOADING', false);
            }
        }
    },
    getters: {
        routeOptions: (state) =>
            state.list.map(r => ({ value: r.id, label: r.routeNumber, ...r }))
    }
};
