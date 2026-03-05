import { createRouter, createWebHistory } from 'vue-router';

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
            path: '/regions/:regionId?',
            name: 'RegionCatalog',
            component: () => import('@/views/RegionCatalog.vue'),
            props: (route) => ({ regionId: route.params.regionId || null }),
            meta: { title: '地区分类' }
        },
        {
            path: '/companies/:companyId?',
            name: 'CompanyCatalog',
            component: () => import('@/views/CompanyCatalog.vue'),
            props: (route) => ({ companyId: route.params.companyId || null }),
            meta: { title: '公司分类' }
        },
        {
            path: '/brands/:brandId?',
            name: 'BrandCatalog',
            component: () => import('@/views/BrandCatalog.vue'),
            props: (route) => ({ brandId: route.params.brandId || null }),
            meta: { title: '品牌分类' }
        },
        {
            path: '/models/:modelId?',
            name: 'ModelCatalog',
            component: () => import('@/views/ModelCatalog.vue'),
            props: (route) => ({ modelId: route.params.modelId || null }),
            meta: { title: '车型分类' }
        },
        {
            path: '/upload',
            name: 'Upload',
            component: () => import('@/views/Upload.vue'),
            meta: { title: '上传图片' }
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

router.afterEach((to) => {
    document.title = to.meta?.title
        ? `${to.meta.title} - ${defaultTitle}`
        : defaultTitle;
});

export default router;