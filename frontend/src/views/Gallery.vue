<template>
    <div class="page gallery-page">
        <main class="main-constrained">
            <!-- Hero: dynamic content for search vs browse mode -->
            <section class="hero" :class="{ 'hero--search': isSearchMode }">
                <div class="hero__text">
                    <p class="eyebrow">Bus Gallery</p>
                    <h1 v-if="!isSearchMode">公交车辆图库</h1>
                    <h1 v-else>搜索结果</h1>
                    <p v-if="!isSearchMode" class="description">
                        收录全国公交车辆的车牌、配置与上线资料。
                    </p>
                    <p v-else class="description">
                        关键词 <mark>{{ filters.keyword }}</mark> · 找到
                        <strong>{{ pagination.total || 0 }}</strong> 辆车
                        <template v-if="routeResults.total"> · <strong>{{ routeResults.total }}</strong> 条线路</template>
                    </p>
                </div>

                <div class="hero__visual">
                    <div class="visual-card">
                        <p class="visual-title" v-if="!isSearchMode">图库收录</p>
                        <p class="visual-title" v-else>匹配结果</p>
                        <p class="visual-number">{{ pagination.total || '--' }}</p>
                        <p class="visual-caption">条车辆记录</p>
                    </div>
                </div>
            </section>

            <!-- Search bar + facet tags  (search mode only)  -->
            <section v-if="isSearchMode" class="search-section">
                <form class="search-form" @submit.prevent="handleSearchSubmit">
                    <input
                        v-model.trim="searchInput"
                        type="text"
                        placeholder="搜索车牌 / 车型 / 公司 / 地区 / 配置"
                        class="search-input"
                    />
                    <button class="search-btn" type="submit">搜索</button>
                    <button class="clear-btn" type="button" @click="handleResetFilters">清除</button>
                </form>

                <!-- Facet tags from search API -->
                <div v-if="searchFacets.brands?.items?.length" class="facet-row">
                    <span class="facet-label">品牌：</span>
                    <button
                        v-for="item in searchFacets.brands.items" :key="'b-'+item.id"
                        class="facet-tag"
                        @click="appendFacet(item.title)"
                    >{{ item.title }}</button>
                </div>
                <div v-if="searchFacets.companies?.items?.length" class="facet-row">
                    <span class="facet-label">公司：</span>
                    <button
                        v-for="item in searchFacets.companies.items" :key="'c-'+item.id"
                        class="facet-tag"
                        @click="appendFacet(item.title)"
                    >{{ item.title }}</button>
                </div>
                <div v-if="searchFacets.regions?.items?.length" class="facet-row">
                    <span class="facet-label">地区：</span>
                    <button
                        v-for="item in searchFacets.regions.items" :key="'rg-'+item.id"
                        class="facet-tag"
                        @click="appendFacet(item.title)"
                    >{{ item.title }}</button>
                </div>
            </section>

            <!-- Gallery -->
            <section class="gallery-section">
                <header class="section-header">
                    <div>
                        <h2 v-if="!isSearchMode">车辆图库</h2>
                        <h2 v-else>车辆列表</h2>
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
                    <button class="primary-btn" type="button" @click="handleRetry">重新加载</button>
                </div>

                <div v-else-if="!gallery.length" class="state state--empty">
                    <p>暂无符合条件的车辆</p>
                    <button class="ghost-btn" type="button" @click="handleResetFilters">查看全部</button>
                </div>

                <div v-else class="gallery-grid">
                    <VehicleCard
                        v-for="item in gallery" :key="item?.vehicle?.id || item.vehicleId"
                        :vehicle="item.vehicle" :config="item.config" :images="item.images"
                        :variants="item.variants" :variant-count="item.variantCount"
                        @view-detail="openVehicleDetail"
                    />
                </div>

                <!-- Route results (search mode) -->
                <div v-if="isSearchMode && routeResults.total" class="route-results">
                    <h3 class="route-head">🚌 相关线路 ({{ routeResults.total }})</h3>
                    <div class="route-list">
                        <div
                            v-for="r in routeResults.items" :key="'rt-'+r.id"
                            class="route-item"
                            @click="$router.push({ name: 'RouteCatalog' })"
                        >
                            <span class="route-number">{{ r.title }}</span>
                            <span v-if="r.subtitle" class="route-stops">{{ r.subtitle }}</span>
                        </div>
                    </div>
                </div>

                <div v-if="pagination.total > pageSize" class="pagination-wrap">
                    <el-pagination
                        background
                        layout="prev, pager, next, total"
                        :current-page="currentPage"
                        :page-size="pageSize"
                        :total="pagination.total"
                        @current-change="handlePageChange"
                    />
                </div>
            </section>
        </main>

        <VehicleDetailModal
            v-if="isDetailVisible" :visible="isDetailVisible"
            :detail="activeVehicleDetail" :loading="activeVehicleLoading"
            @close="closeDetail"
        />
    </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted, defineAsyncComponent } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import { fetchVehicleGallery } from '@/api/vehicles';
import { searchAll } from '@/api/search';
import VehicleCard from '@/components/Gallery/VehicleCard.vue';

const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));

const PAGE_SIZE = 12;

const store = useStore();
const route = useRoute();
const router = useRouter();

const searchInput = ref('');
const searchFacets = ref({});
const routeResults = ref({ total: 0, items: [] });

const filters = reactive({
    regionId: null,
    companyId: null,
    brandId: null,
    modelId: null,
    routeId: null,
    keyword: ''
});

const cursorByPage = ref({ 1: { lastLaunch: null, lastId: null } });
const currentPage = ref(1);
const loadingToken = ref(0);

const rawGallery = computed(() => store.state.vehicles.gallery);
const galleryLoading = computed(() => store.state.vehicles.galleryLoading);
const galleryError = computed(() => store.state.vehicles.galleryError);
const pagination = computed(() => store.state.vehicles.pagination);
const pageSize = computed(() => pagination.value.size || PAGE_SIZE);

const isSearchMode = computed(() => Boolean(filters.keyword));

const normalizePlate = (p) => (p || '').replace(/\s+/g, '');
const normalizePage = (v) => { const n = Number(v); return Number.isInteger(n) && n > 0 ? n : 1; };
const normalizeKeyword = (v) => (typeof v === 'string' ? v.trim() : '');

const resetCursorCache = () => { cursorByPage.value = { 1: { lastLaunch: null, lastId: null } }; };

const sanitizeFilters = (source = filters) => {
    const p = {};
    Object.entries(source).forEach(([k, v]) => { if (v !== null && v !== undefined && v !== '') p[k] = v; });
    return p;
};

const buildRouteQuery = (page, keyword) => {
    const q = {};
    const nk = normalizeKeyword(keyword);
    if (nk) q.keyword = nk;
    if (page > 1) q.page = String(page);
    return q;
};

const isSameQuery = (a = {}, b = {}) => {
    const ae = Object.entries(a).filter(([, v]) => v != null && v !== '');
    const be = Object.entries(b).filter(([, v]) => v != null && v !== '');
    if (ae.length !== be.length) return false;
    return ae.every(([k, v]) => String(b[k] ?? '') === String(v));
};

const setRoutePage = async (page, keyword, replace = false) => {
    const q = buildRouteQuery(page, keyword);
    if (isSameQuery(q, route.query)) return;
    const nav = { name: 'Gallery', query: q };
    if (replace) await router.replace(nav); else await router.push(nav);
};

const gallery = computed(() => {
    const map = new Map();
    rawGallery.value.forEach((item) => {
        const plate = normalizePlate(item?.vehicle?.plateNumber);
        if (!plate) return;
        const vk = item?.vehicle?.id != null
            ? `vid:${item.vehicle.id}`
            : `fallback:${plate}:${item?.vehicle?.launchDate || ''}:${item?.vehicle?.company?.id || ''}`;
        if (!map.has(plate)) {
            map.set(plate, { ...item, variants: [item], images: [...(item.images || [])], variantCount: 1, variantKeys: new Set([vk]) });
        } else {
            const acc = map.get(plate);
            if (!acc.variantKeys.has(vk)) { acc.variantKeys.add(vk); acc.variants.push(item); acc.variantCount = acc.variants.length; }
            acc.images = acc.images.length ? acc.images : [...(item.images || [])];
        }
    });
    return Array.from(map.values()).map(({ variantKeys, ...rest }) => rest);
});

const activeVehicleId = ref(null);
const activeVehicleDetail = computed(() => activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] || null : null);
const activeVehicleLoading = computed(() => (activeVehicleId.value && store.state.vehicles.detailLoadingMap[activeVehicleId.value]) || false);
const isDetailVisible = computed(() => Boolean(activeVehicleId.value));

const getCursorForPage = (tp) => tp <= 1 ? { lastLaunch: null, lastId: null } : (cursorByPage.value[tp] || null);

const ensureCursorForPage = async (targetPage, filterPayload, token) => {
    if (targetPage <= 1) return true;
    for (let p = 2; p <= targetPage; p++) {
        if (cursorByPage.value[p]) continue;
        const prev = cursorByPage.value[p - 1];
        if (!prev) return false;
        const probe = await fetchVehicleGallery({
            ...filterPayload, size: PAGE_SIZE,
            ...(prev.lastLaunch ? { lastLaunch: prev.lastLaunch } : {}),
            ...(prev.lastId ? { lastId: prev.lastId } : {})
        });
        if (token !== loadingToken.value) return false;
        if (probe?.nextLaunch == null && probe?.nextId == null) return false;
        cursorByPage.value[p] = { lastLaunch: probe?.nextLaunch || null, lastId: probe?.nextId || null };
    }
    return true;
};

const loadGalleryByRoute = async (page, keyword) => {
    const np = normalizePage(page);
    const nk = normalizeKeyword(keyword);
    const token = ++loadingToken.value;

    const kwChanged = nk !== filters.keyword;
    filters.keyword = nk;
    if (kwChanged) { resetCursorCache(); searchInput.value = nk; }

    const fp = sanitizeFilters(filters);
    let cursor = getCursorForPage(np);
    if (token !== loadingToken.value) return;

    if (np > 1 && !cursor) {
        const resolved = await ensureCursorForPage(np, fp, token);
        if (token !== loadingToken.value) return;
        cursor = getCursorForPage(np);
        if (!resolved || !cursor) { currentPage.value = 1; await setRoutePage(1, nk, true); return; }
    }

    const resp = await store.dispatch('vehicles/loadVehicleGallery', {
        ...fp, size: PAGE_SIZE, page: np,
        ...(cursor?.lastLaunch ? { lastLaunch: cursor.lastLaunch } : {}),
        ...(cursor?.lastId ? { lastId: cursor.lastId } : {})
    });
    if (token !== loadingToken.value) return;

    if (resp?.nextLaunch != null || resp?.nextId != null) {
        cursorByPage.value[np + 1] = { lastLaunch: resp?.nextLaunch || null, lastId: resp?.nextId || null };
    }
    currentPage.value = np;

    // Fetch related facets when keyword is present
    if (nk) fetchSearchFacets(nk);
};

const fetchSearchFacets = async (kw) => {
    if (!kw) { searchFacets.value = {}; routeResults.value = { total: 0, items: [] }; return; }
    try {
        const resp = await searchAll(kw, 'all');
        searchFacets.value = resp || {};
        routeResults.value = resp?.routes || { total: 0, items: [] };
    } catch { searchFacets.value = {}; routeResults.value = { total: 0, items: [] }; }
};

const appendFacet = (term) => {
    const current = searchInput.value.trim();
    searchInput.value = current ? `${current} ${term}` : term;
    handleSearchSubmit();
};

const openVehicleDetail = async (vehicleId) => {
    if (!vehicleId) return;
    activeVehicleId.value = vehicleId;
    try { await store.dispatch('vehicles/loadVehicleDetail', { vehicleId, force: true }); }
    catch (e) { console.error(e); }
};

const closeDetail = () => { activeVehicleId.value = null; };

const handleRetry = () => { loadGalleryByRoute(currentPage.value, filters.keyword).catch(console.error); };

const handleResetFilters = () => {
    Object.assign(filters, { regionId: null, companyId: null, brandId: null, modelId: null, routeId: null, keyword: '' });
    resetCursorCache();
    searchInput.value = '';
    searchFacets.value = {};
    setRoutePage(1, '', true).catch(console.error);
};

const handlePageChange = (np) => { setRoutePage(normalizePage(np), filters.keyword).catch(console.error); };

const handleSearchSubmit = () => {
    const kw = searchInput.value.trim();
    if (!kw) { handleResetFilters(); return; }
    setRoutePage(1, kw, false).catch(console.error);
};

onMounted(() => {
    const kw = normalizeKeyword(route.query.keyword);
    if (kw) searchInput.value = kw;
});

watch(
    () => [route.query.keyword, route.query.page],
    async ([keyword, page]) => {
        const nk = normalizeKeyword(keyword);
        const np = normalizePage(page);
        try { await loadGalleryByRoute(np, nk); }
        catch (e) { console.error(e); }
    },
    { immediate: true }
);
</script>

<style scoped lang="scss">
.page { min-height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }

.main-constrained {
    width: min(1200px, 100%); margin: 0 auto; flex: 1;
    padding: clamp(24px, 4vw, 48px) clamp(16px, 3vw, 28px) clamp(80px, 6vw, 100px);
    box-sizing: border-box;
}

// ---- Hero ----
.hero {
    background: linear-gradient(135deg, #1e3a5f 0%, #1a4972 40%, #0f5c8b 100%);
    border-radius: 20px; padding: 36px 40px; color: #fff;
    display: flex; gap: 36px; align-items: center; margin-bottom: 28px;
    box-shadow: 0 12px 32px rgba(15, 40, 70, 0.25);

    &--search {
        background: linear-gradient(135deg, #0f5c8b 0%, #0e7490 50%, #0891b2 100%);
    }

    &__text { flex: 1;
        mark { background: rgba(255,255,255,0.22); color: #fff; padding: 2px 8px; border-radius: 4px; }
        strong { font-weight: 700; }
    }
    &__visual { flex: 0 0 240px; display: flex; justify-content: center; }
}

.eyebrow { letter-spacing: 0.18em; font-size: 0.78rem; text-transform: uppercase; opacity: 0.75; margin-bottom: 6px; }
.hero h1 { margin: 0 0 8px; font-size: clamp(1.6rem, 3vw, 2.2rem); font-weight: 700; }
.description { margin: 0; font-size: 0.95rem; color: rgba(255,255,255,0.85); }

.visual-card {
    width: 100%; border-radius: 20px; padding: 28px 24px;
    background: rgba(255,255,255,0.12); text-align: center; backdrop-filter: blur(6px);
}
.visual-title { letter-spacing: 0.15em; font-size: 0.72rem; margin-bottom: 8px; opacity: 0.8; }
.visual-number { font-size: 3.2rem; font-weight: 700; margin: 0; line-height: 1.1; }
.visual-caption { margin-top: 6px; font-size: 0.85rem; color: rgba(255,255,255,0.8); }

// ---- Search section (keyword mode) ----
.search-section {
    margin-bottom: 24px;
}
.search-form { display: flex; gap: 10px; margin-bottom: 12px; }
.search-input {
    flex: 1; border: 1px solid #d1d5db; border-radius: 12px; padding: 12px 18px;
    font-size: 0.95rem; outline: none; background: #fff;
    &:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,0.12); }
}
.search-btn {
    border: none; border-radius: 12px; padding: 12px 24px; background: #2563eb; color: #fff;
    font-weight: 600; cursor: pointer; font-size: 0.95rem;
    &:hover { background: #1d4ed8; }
}
.clear-btn {
    border: 1px solid #d1d5db; border-radius: 12px; padding: 12px 18px; background: #fff;
    color: #6b7280; cursor: pointer; font-size: 0.9rem;
    &:hover { background: #f3f4f6; }
}

.facet-row {
    display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-bottom: 6px;
}
.facet-label { font-weight: 600; color: #6b7280; font-size: 0.82rem; flex-shrink: 0; }
.facet-tag {
    padding: 4px 12px; border-radius: 999px; background: #eff6ff; color: #2563eb;
    font-size: 0.8rem; cursor: pointer; border: none; font-weight: 500;
    &:hover { background: #dbeafe; }
}

// ---- Gallery section ----
.gallery-section {
    background: #fff; border-radius: 20px; padding: 28px 32px;
    box-shadow: 0 8px 28px rgba(15, 23, 42, 0.06);
}
.section-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px;
    h2 { margin: 0; font-size: 1.25rem; color: #111827; }
    .subtitle { color: #6b7280; margin-top: 2px; font-size: 0.88rem; }
}
.gallery-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.pagination-wrap { margin-top: 24px; display: flex; justify-content: center; }

// ---- State ----
.state { border-radius: 16px; padding: 48px; text-align: center; background: #f8fafc; color: #475569;
    &--loading { color: #2563eb; }
    &--error { background: #fee2e2; color: #b91c1c; }
    button { margin-top: 16px; }
}

.primary-btn, .ghost-btn { border: none; border-radius: 999px; padding: 10px 20px; cursor: pointer; font-weight: 600; transition: all 0.2s; }
.primary-btn { background: #2563eb; color: #fff; &:hover { background: #1d4ed8; } }
.ghost-btn { background: rgba(37,99,235,0.08); color: #2563eb; &:hover { background: rgba(37,99,235,0.15); } }

// ---- Route results ----
.route-results {
    margin-top: 24px; padding-top: 20px; border-top: 1px solid #e5e7eb;
}
.route-head { margin: 0 0 10px; font-size: 0.95rem; color: #374151; }
.route-list { display: flex; flex-wrap: wrap; gap: 8px; }
.route-item {
    display: flex; align-items: center; gap: 8px;
    padding: 8px 14px; border-radius: 10px; background: #f8fafc;
    border: 1px solid #e2e8f0; cursor: pointer;
    &:hover { background: #f1f5f9; }
}
.route-number { font-weight: 700; color: #1e40af; font-size: 0.9rem; white-space: nowrap; }
.route-stops { color: #6b7280; font-size: 0.8rem; }

// ---- Media ----
@media (max-width: 900px) {
    .hero { padding: 28px; gap: 16px; flex-direction: column; text-align: center;
        &__visual { flex: 0 0 auto; width: 100%; max-width: 200px; }
    }
    .gallery-section { padding: 20px; }
}
@media (max-width: 600px) {
    .gallery-grid { grid-template-columns: 1fr; }
    .search-form { flex-direction: column; }
}
</style>

