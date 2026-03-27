<template>
    <div class="page home-view">
        <main class="home-main constrained">
            <section class="hero">
                <div class="hero__text">
                    <p class="eyebrow">Bus Gallery</p>
                    <h1>公交车辆图库</h1>
                    <p class="description">
                        收录全国公交车辆的车牌、配置与上线资料。
                    </p>
                </div>

                <div class="hero__visual">
                    <div class="visual-card">
                        <p class="visual-title">图库收录</p>
                        <p class="visual-number">{{ pagination.total || '--' }}</p>
                        <p class="visual-caption">条车辆记录</p>
                    </div>
                </div>
            </section>

            <section class="gallery-section">
                <header class="section-header">
                    <div>
                        <h2>车辆图库</h2>
                        <p class="subtitle">当前筛选下共 {{ pagination.total }} 条</p>
                    </div>
                    <button class="ghost-btn" type="button" @click="handleResetFilters">
                        重置筛选
                    </button>
                </header>

                <div v-if="galleryLoading" class="state state--loading">
                    正在加载车辆信息...
                </div>

                <div v-else-if="galleryError" class="state state--error">
                    <p>{{ galleryError }}</p>
                    <button class="primary-btn" type="button" @click="handleRetry">
                        重新加载
                    </button>
                </div>

                <div v-else-if="!gallery.length" class="state state--empty">
                    <p>暂无符合条件的车辆</p>
                    <button class="ghost-btn" type="button" @click="handleResetFilters">
                        查看全部
                    </button>
                </div>

                <div v-else class="gallery-grid">
                    <VehicleCard v-for="item in gallery" :key="item?.vehicle?.id || item.vehicleId"
                        :vehicle="item.vehicle" :config="item.config" :images="item.images" :variants="item.variants"
                        :variant-count="item.variantCount"
                        @view-detail="openVehicleDetail" />
                </div>

                <div v-if="pagination.total > pageSize" class="pagination-wrap">
                    <el-pagination
                        background
                        layout="prev, pager, next, jumper, total"
                        :current-page="currentPage"
                        :page-size="pageSize"
                        :total="pagination.total"
                        @current-change="handlePageChange"
                    />
                </div>
            </section>
        </main>

        <VehicleDetailModal v-if="isDetailVisible" :visible="isDetailVisible" :detail="activeVehicleDetail"
            :loading="activeVehicleLoading" @close="closeDetail" />
    </div>
</template>

<script setup>
import { computed, reactive, ref, watch, defineAsyncComponent } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import VehicleCard from '@/components/Gallery/VehicleCard.vue';
import { fetchVehicleGallery } from '@/api/vehicles';

const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));

const PAGE_SIZE = 12;

const store = useStore();
const route = useRoute();
const router = useRouter();

const filters = reactive({
    regionId: null,
    companyId: null,
    brandId: null,
    modelId: null,
    keyword: ''
});

const cursorByPage = ref({
    1: { lastLaunch: null, lastId: null }
});
const currentPage = ref(1);
const loadingToken = ref(0);

const rawGallery = computed(() => store.state.vehicles.gallery);
const galleryLoading = computed(() => store.state.vehicles.galleryLoading);
const galleryError = computed(() => store.state.vehicles.galleryError);
const pagination = computed(() => store.state.vehicles.pagination);
const pageSize = computed(() => pagination.value.size || PAGE_SIZE);

const normalizePlate = (p) => (p || '').replace(/\s+/g, '');
const normalizePage = (value) => {
    const num = Number(value);
    return Number.isInteger(num) && num > 0 ? num : 1;
};
const normalizeKeyword = (value) => (typeof value === 'string' ? value.trim() : '');
const resetCursorCache = () => {
    cursorByPage.value = { 1: { lastLaunch: null, lastId: null } };
};

const sanitizeFilters = (source = filters) => {
    const payload = {};
    Object.entries(source).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') {
            return;
        }
        payload[key] = value;
    });
    return payload;
};

const buildRouteQuery = (page, keyword) => {
    const query = {};
    const normalizedKeyword = normalizeKeyword(keyword);
    if (normalizedKeyword) {
        query.keyword = normalizedKeyword;
    }
    if (page > 1) {
        query.page = String(page);
    }
    return query;
};

const isSameQuery = (a = {}, b = {}) => {
    const aEntries = Object.entries(a).filter(([, val]) => val != null && val !== '');
    const bEntries = Object.entries(b).filter(([, val]) => val != null && val !== '');
    if (aEntries.length !== bEntries.length) return false;
    return aEntries.every(([key, val]) => String(b[key] ?? '') === String(val));
};

const setRoutePage = async (page, keyword, replace = false) => {
    const query = buildRouteQuery(page, keyword);
    if (isSameQuery(query, route.query)) {
        return;
    }
    const navigation = { name: 'Gallery', query };
    if (replace) {
        await router.replace(navigation);
        return;
    }
    await router.push(navigation);
};

const gallery = computed(() => {
    const map = new Map();
    rawGallery.value.forEach((item) => {
        const plate = normalizePlate(item?.vehicle?.plateNumber);
        if (!plate) {
            return;
        }
        const variantKey = item?.vehicle?.id != null
            ? `vid:${item.vehicle.id}`
            : `fallback:${plate}:${item?.vehicle?.launchDate || ''}:${item?.vehicle?.company?.id || ''}`;
        if (!map.has(plate)) {
            map.set(plate, {
                ...item,
                variants: [item],
                images: [...(item.images || [])],
                variantCount: 1,
                variantKeys: new Set([variantKey])
            });
        } else {
            const acc = map.get(plate);
            if (!acc.variantKeys.has(variantKey)) {
                acc.variantKeys.add(variantKey);
                acc.variants.push(item);
                acc.variantCount = acc.variants.length;
            }
            acc.images = acc.images.length ? acc.images : [...(item.images || [])];
        }
    });
    return Array.from(map.values()).map((item) => {
        const { variantKeys, ...rest } = item;
        return rest;
    });
});

const activeVehicleId = ref(null);
const activeVehicleDetail = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] || null : null
);
const activeVehicleLoading = computed(
    () =>
        (activeVehicleId.value && store.state.vehicles.detailLoadingMap[activeVehicleId.value]) ||
        false
);
const isDetailVisible = computed(() => Boolean(activeVehicleId.value));

const ensureCursorForPage = async (targetPage, filterPayload) => {
    if (targetPage <= 1) {
        return { lastLaunch: null, lastId: null };
    }
    if (cursorByPage.value[targetPage]) {
        return cursorByPage.value[targetPage];
    }

    const knownPages = Object.keys(cursorByPage.value)
        .map((num) => Number(num))
        .filter((num) => Number.isInteger(num) && num >= 1 && num < targetPage)
        .sort((a, b) => b - a);
    let pageCursor = knownPages.length ? knownPages[0] : 1;
    let cursor = cursorByPage.value[pageCursor] || { lastLaunch: null, lastId: null };

    while (pageCursor < targetPage) {
        const response = await fetchVehicleGallery({
            size: PAGE_SIZE,
            ...filterPayload,
            ...(cursor?.lastLaunch ? { lastLaunch: cursor.lastLaunch } : {}),
            ...(cursor?.lastId ? { lastId: cursor.lastId } : {})
        });
        const hasNextCursor = response?.nextLaunch != null || response?.nextId != null;
        if (!hasNextCursor) {
            return null;
        }
        cursor = {
            lastLaunch: response?.nextLaunch || null,
            lastId: response?.nextId || null
        };
        pageCursor += 1;
        cursorByPage.value[pageCursor] = cursor;
    }

    return cursorByPage.value[targetPage] || null;
};

const loadGalleryByRoute = async (page, keyword) => {
    const normalizedPage = normalizePage(page);
    const normalizedKeyword = normalizeKeyword(keyword);
    const token = ++loadingToken.value;

    const keywordChanged = normalizedKeyword !== filters.keyword;
    filters.keyword = normalizedKeyword;
    if (keywordChanged) {
        resetCursorCache();
    }

    const filterPayload = sanitizeFilters(filters);
    const cursor =
        normalizedPage > 1 ? await ensureCursorForPage(normalizedPage, filterPayload) : { lastLaunch: null, lastId: null };

    if (token !== loadingToken.value) {
        return;
    }

    if (normalizedPage > 1 && !cursor) {
        currentPage.value = 1;
        await setRoutePage(1, normalizedKeyword, true);
        return;
    }

    const response = await store.dispatch('vehicles/loadVehicleGallery', {
        ...filterPayload,
        size: PAGE_SIZE,
        page: normalizedPage,
        ...(cursor?.lastLaunch ? { lastLaunch: cursor.lastLaunch } : {}),
        ...(cursor?.lastId ? { lastId: cursor.lastId } : {})
    });
    if (token !== loadingToken.value) {
        return;
    }

    if (response?.nextLaunch != null || response?.nextId != null) {
        cursorByPage.value[normalizedPage + 1] = {
            lastLaunch: response?.nextLaunch || null,
            lastId: response?.nextId || null
        };
    }
    currentPage.value = normalizedPage;
};

const openVehicleDetail = async (vehicleId) => {
    if (!vehicleId) return;
    activeVehicleId.value = vehicleId;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', vehicleId);
    } catch (error) {
        console.error(error);
    }
};

const closeDetail = () => {
    activeVehicleId.value = null;
};

const handleRetry = () => {
    loadGalleryByRoute(currentPage.value, filters.keyword).catch((error) => {
        console.error(error);
    });
};

const handleResetFilters = () => {
    Object.assign(filters, {
        regionId: null,
        companyId: null,
        brandId: null,
        modelId: null,
        keyword: ''
    });
    resetCursorCache();
    setRoutePage(1, '', true).catch((error) => {
        console.error(error);
    });
};

const handlePageChange = (nextPage) => {
    setRoutePage(nextPage, filters.keyword).catch((error) => {
        console.error(error);
    });
};

watch(
    () => [route.query.keyword, route.query.page],
    async ([keyword, page]) => {
        const normalizedKeyword = normalizeKeyword(keyword);
        const normalizedPage = normalizePage(page);
        try {
            await loadGalleryByRoute(normalizedPage, normalizedKeyword);
        } catch (error) {
            console.error(error);
        }
    },
    { immediate: true }
);
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f7fb;
}

.constrained {
    width: min(1200px, 100%);
    margin: 0 auto;
    flex: 1;
    padding: clamp(32px, 5vw, 72px) clamp(16px, 4vw, 32px) clamp(96px, 8vw, 120px);
    box-sizing: border-box;
}

.hero {
    background: radial-gradient(circle at top left, #e0f2fe, #c7d2fe);
    border-radius: 32px;
    padding: 40px;
    color: #fff;
    display: flex;
    gap: 40px;
    align-items: center;
    margin-bottom: 32px;

    &__text {
        flex: 1;
    }

    &__visual {
        flex: 0 0 280px;
        display: flex;
        justify-content: center;
    }
}

@media (max-width: 900px) {
    .hero {
        padding: 24px;
        gap: 16px;
        min-height: 200px;
    }

    .hero__visual {
        display: none;
    }

    .gallery-section {
        padding: 24px;
    }
}

@media (max-width: 600px) {
    .hero {
        padding: 24px;
    }

    .gallery-grid {
        grid-template-columns: 1fr;
    }
}

.eyebrow {
    letter-spacing: 0.2em;
    font-size: 0.75rem;
    text-transform: uppercase;
    opacity: 0.8;
    margin-bottom: 8px;
}

.description {
    margin: 12px 0 20px;
    color: rgba(255, 255, 255, 0.8);
}

.visual-card {
    width: 100%;
    border-radius: 24px;
    padding: 32px;
    background: rgba(15, 23, 42, 0.3);
    text-align: center;
    backdrop-filter: blur(4px);
}

.visual-title {
    letter-spacing: 0.2em;
    font-size: 0.75rem;
    margin-bottom: 12px;
    opacity: 0.85;
}

.visual-number {
    font-size: 3.5rem;
    font-weight: 700;
    margin: 0;
}

.visual-caption {
    margin-top: 8px;
    color: rgba(255, 255, 255, 0.9);
}

.gallery-section {
    background: #fff;
    border-radius: 24px;
    padding: 32px;
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;

    .subtitle {
        color: #6b7280;
        margin-top: 4px;
    }
}

.gallery-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
}

.pagination-wrap {
    margin-top: 22px;
    display: flex;
    justify-content: center;
}

.state {
    border-radius: 18px;
    padding: 48px;
    text-align: center;
    background: #f8fafc;
    color: #475569;

    &--loading {
        color: #2563eb;
    }

    &--error {
        background: #fee2e2;
        color: #b91c1c;
    }

    &--empty {
        color: #475569;
    }

    button {
        margin-top: 16px;
    }
}

.primary-btn,
.ghost-btn {
    border: none;
    border-radius: 999px;
    padding: 10px 18px;
    cursor: pointer;
    font-weight: 600;
    transition: all 0.2s;
}

.primary-btn {
    background: #2563eb;
    color: #fff;

    &:hover {
        background: #1d4ed8;
    }
}

.ghost-btn {
    background: rgba(37, 99, 235, 0.1);
    color: #2563eb;

    &:hover {
        background: rgba(37, 99, 235, 0.2);
    }
}

@media (max-width: 600px) {
    .gallery-grid {
        grid-template-columns: minmax(0, 1fr);
    }
}
</style>

