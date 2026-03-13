import { login as loginApi, register as registerApi, logout as logoutApi } from '@/api/auth';
import { fetchMe } from '@/api/users';
import { getToken, persistToken } from '@/utils/auth';

const state = () => ({
    token: getToken() || '',
    profile: null,
    loading: false,
    error: null
});

const getters = {
    isAuthenticated: (state) => Boolean(state.token)
};

const mutations = {
    SET_LOADING(state, status) {
        state.loading = status;
    },
    SET_ERROR(state, error) {
        state.error = error;
    },
    SET_TOKEN(state, token) {
        state.token = token;
    },
    SET_PROFILE(state, profile) {
        state.profile = profile;
    },
    RESET(state) {
        state.token = '';
        state.profile = null;
        state.error = null;
    }
};

const actions = {
    async login({ commit }, payload) {
        commit('SET_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const data = await loginApi(payload);
            persistToken(data.token);
            commit('SET_TOKEN', data.token);
            commit('SET_PROFILE', data.profile);
            return data.profile;
        } catch (error) {
            commit('SET_ERROR', error.message);
            throw error;
        } finally {
            commit('SET_LOADING', false);
        }
    },
    async register({ commit }, payload) {
        commit('SET_LOADING', true);
        commit('SET_ERROR', null);
        try {
            const data = await registerApi(payload);
            persistToken(data.token);
            commit('SET_TOKEN', data.token);
            commit('SET_PROFILE', data.profile);
            return data.profile;
        } catch (error) {
            commit('SET_ERROR', error.message);
            throw error;
        } finally {
            commit('SET_LOADING', false);
        }
    },
    async logout({ commit, state }) {
        if (state.token) {
            try {
                await logoutApi();
            } catch (e) {
                // ignore
            }
        }
        persistToken('');
        commit('RESET');
    },
    async fetchProfile({ commit, state }) {
        if (!state.token) return null;
        try {
            const profile = await fetchMe();
            commit('SET_PROFILE', profile);
            return profile;
        } catch (error) {
            commit('SET_ERROR', error.message);
            throw error;
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
