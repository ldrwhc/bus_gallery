import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
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
            meta: { title: '地区' }
        },
        {
            path: '/companies/:companyId?',
            name: 'CompanyCatalog',
            component: () => import('@/views/CompanyCatalog.vue'),
            props: (route) => ({ companyId: route.params.companyId || null }),
            meta: { title: '公司' }
        },
        {
            path: '/brands/:brandId?',
            name: 'BrandCatalog',
            component: () => import('@/views/BrandCatalog.vue'),
            props: (route) => ({ brandId: route.params.brandId || null }),
            meta: { title: '品牌' }
        },
        {
            path: '/models/:modelId?',
            name: 'ModelCatalog',
            component: () => import('@/views/ModelCatalog.vue'),
            props: (route) => ({ modelId: route.params.modelId || null }),
            meta: { title: '车型' }
        },
        {
            path: '/stats',
            name: 'Stats',
            component: () => import('@/views/Stats.vue'),
            meta: { title: '统计' }
        },
        {
            path: '/about',
            name: 'About',
            component: () => import('@/views/About.vue'),
            meta: { title: '文档' }
        },
        {
            path: '/upload',
            name: 'Upload',
            component: () => import('@/views/Upload.vue'),
            meta: { title: '上传图片', requiresAuth: true }
        },
        {
            path: '/routes',
            name: 'RouteCatalog',
            component: () => import('@/views/RouteCatalog.vue'),
            meta: { title: '线路' }
        },
        {
            path: '/compare',
            name: 'Compare',
            component: () => import('@/views/Compare.vue'),
            meta: { title: '对比' }
        },
        {
            path: '/routes/:routeId',
            name: 'RouteDetail',
            component: () => import('@/views/RouteDetail.vue'),
            props: true,
            meta: { title: '线路详情' }
        },
        {
            path: '/search',
            name: 'SearchResults',
            component: () => import('@/views/SearchResults.vue'),
            meta: { title: '搜索' }
        },
        {
            path: '/group-buy',
            name: 'GroupBuyMarket',
            component: () => import('@/views/GroupBuyMarket.vue'),
            meta: { title: '拼团交易台', requiresAuth: true }
        },
        {
            path: '/group-buy/goods',
            name: 'GroupGoodsDetail',
            component: () => import('@/views/GroupGoodsDetail.vue'),
            meta: { title: '拼团商品详情', requiresAuth: true }
        },
        {
            path: '/trade/download',
            name: 'TradeDownload',
            component: () => import('@/views/TradeDownload.vue'),
            meta: { title: '交易下载', requiresAuth: true }
        },
        {
            path: '/review',
            name: 'ReviewCenter',
            component: () => import('@/views/ReviewCenter.vue'),
            meta: { title: '审核中心', requiresAuth: true, roles: ['REVIEWER', 'STATION'] }
        },
        {
            path: '/dashboard',
            name: 'AdminDashboard',
            component: () => import('@/views/AdminDashboard.vue'),
            meta: { title: '站长后台', requiresAuth: true, roles: ['STATION'] }
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
                    return { name: 'UserProfile', params: { userId: id }, query: to.query };
                }
                return true;
            }
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/AuthPortal.vue'),
            meta: { title: '登录' }
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/AuthPortal.vue'),
            meta: { title: '注册' }
        },
        {
            path: '/forgot-password',
            name: 'ForgotPassword',
            component: () => import('@/views/ForgotPassword.vue'),
            meta: { title: '找回密码' }
        },
        {
            path: '/:pathMatch(.*)*',
            redirect: '/'
        }
    ],
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) return savedPosition;
        return { top: 0 };
    }
});

let lastAuthPromptAt = 0;
const notifyLoginRequired = () => {
    const now = Date.now();
    if (now - lastAuthPromptAt < 1200) return;
    lastAuthPromptAt = now;
    ElMessage.warning('请先登录');
};

router.beforeEach(async (to) => {
    const requiresAuth = to.meta?.requiresAuth;
    const hasToken = store.getters['auth/hasToken'];
    let isAuthed = store.getters['auth/isAuthenticated'];

    if (!isAuthed && hasToken) {
        await store.dispatch('auth/bootstrap');
        isAuthed = store.getters['auth/isAuthenticated'];
    }

    if (requiresAuth && !isAuthed) {
        notifyLoginRequired();
        return { name: 'Login', query: { redirect: to.fullPath } };
    }

    const roleLimits = to.meta?.roles;
    if (requiresAuth && Array.isArray(roleLimits) && roleLimits.length > 0) {
        const role = store.state.auth.profile?.role;
        if (!role || !roleLimits.includes(role)) {
            ElMessage.warning('当前账号无权限访问该页面');
            return { name: 'Home' };
        }
    }

    if ((to.name === 'Login' || to.name === 'Register') && isAuthed) {
        return { name: 'Home' };
    }
    return true;
});

router.afterEach((to) => {
    document.title = to.meta?.title ? `${to.meta.title} - ${defaultTitle}` : defaultTitle;
});

export default router;

