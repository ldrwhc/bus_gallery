import { createRouter, createWebHistory } from 'vue-router';
import store from '@/store';

const defaultTitle = import.meta.env.VITE_APP_TITLE || 'Bus Gallery';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: () => import('@/views/Home.vue'),
            meta: { title: '首页' }
        },
        {
            path: '/gallery',
            name: 'Gallery',
            component: () => import('@/views/Gallery.vue'),
            meta: { title: '图库' }
        },
        {
            path: '/regions/:regionId?',
            name: 'RegionCatalog',
            component: () => import('@/views/RegionCatalog.vue'),
            props: (route) => ({ regionId: route.params.regionId || null }),
            meta: { title: '按地区' }
        },
        {
            path: '/companies/:companyId?',
            name: 'CompanyCatalog',
            component: () => import('@/views/CompanyCatalog.vue'),
            props: (route) => ({ companyId: route.params.companyId || null }),
            meta: { title: '按公司' }
        },
        {
            path: '/brands/:brandId?',
            name: 'BrandCatalog',
            component: () => import('@/views/BrandCatalog.vue'),
            props: (route) => ({ brandId: route.params.brandId || null }),
            meta: { title: '按品牌' }
        },
        {
            path: '/models/:modelId?',
            name: 'ModelCatalog',
            component: () => import('@/views/ModelCatalog.vue'),
            props: (route) => ({ modelId: route.params.modelId || null }),
            meta: { title: '按车型' }
        },
        {
            path: '/about',
            name: 'About',
            component: () => import('@/views/About.vue'),
            meta: { title: '关于' }
        },
        {
            path: '/upload',
            name: 'Upload',
            component: () => import('@/views/Upload.vue'),
            meta: { title: '上传图片', requiresAuth: true }
        },
        {
            path: '/users/:userId',
            name: 'UserProfile',
            component: () => import('@/views/UserProfile.vue'),
            props: true,
            meta: { title: '用户主页' }
        },
        {
            path: '/account',
            name: 'Account',
            component: () => import('@/views/UserProfile.vue'),
            meta: { title: '我的账户', requiresAuth: true },
            beforeEnter: async (to) => {
                const isAuthed = store.getters['auth/isAuthenticated'];
                if (!isAuthed) {
                    return { name: 'Login', query: { redirect: to.fullPath } };
                }
                if (!store.state.auth.profile) {
                    try {
                        await store.dispatch('auth/fetchProfile');
                    } catch (e) {
                        return { name: 'Login', query: { redirect: to.fullPath } };
                    }
                }
                const id = store.state.auth.profile?.id;
                if (id && to.params.userId !== id) {
                    return { name: 'UserProfile', params: { userId: id } };
                }
                return true;
            }
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/Login.vue'),
            meta: { title: '登录' }
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/Register.vue'),
            meta: { title: '注册' }
        },
        {
            path: '/:pathMatch(.*)*',
            redirect: '/'
        }
    ],
    scrollBehavior() {
        return { top: 0 };
    }
});

router.beforeEach((to, from, next) => {
    const requiresAuth = to.meta?.requiresAuth;
    const isAuthed = store.getters['auth/isAuthenticated'];
    if (requiresAuth && !isAuthed) {
        next({ name: 'Login', query: { redirect: to.fullPath } });
        return;
    }
    if ((to.name === 'Login' || to.name === 'Register') && isAuthed) {
        next({ name: 'Home' });
        return;
    }
    next();
});

router.afterEach((to) => {
    document.title = to.meta?.title ? `${to.meta.title} - ${defaultTitle}` : defaultTitle;
});

export default router;
