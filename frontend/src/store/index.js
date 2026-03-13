import { createStore } from 'vuex';
import regions from './modules/regions';
import companies from './modules/companies';
import brands from './modules/brands';
import models from './modules/models';
import vehicles from './modules/vehicles';
import auth from './modules/auth';

const store = createStore({
    modules: {
        regions,
        companies,
        brands,
        models,
        vehicles,
        auth
    },
    strict: import.meta.env.DEV
});

export default store;
