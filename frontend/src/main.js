import { createApp } from 'vue';
import App from './App.vue';
import router from './router';

import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './styles/global.scss';

import store from './store';

const app = createApp(App);
app.use(store);
app.use(router);
app.use(ElementPlus);

const installLightAntiCrawler = () => {
    const isProtectedImage = (target) =>
        target instanceof HTMLElement &&
        (target.tagName === 'IMG' || Boolean(target.closest('img, [data-anti-copy="image"]')));

    document.addEventListener('contextmenu', (event) => {
        if (isProtectedImage(event.target)) {
            event.preventDefault();
        }
    });

    document.addEventListener('dragstart', (event) => {
        if (isProtectedImage(event.target)) {
            event.preventDefault();
        }
    });
};

// Validate persisted token before first paint to avoid showing logged-in UI for stale sessions.
const bootstrapAndMount = async () => {
    await store.dispatch('auth/bootstrap');
    installLightAntiCrawler();
    app.mount('#app');
};

bootstrapAndMount();
