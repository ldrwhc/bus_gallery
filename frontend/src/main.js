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
app.mount('#app');