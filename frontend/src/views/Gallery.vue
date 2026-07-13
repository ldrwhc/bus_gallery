<template>
    <div class="page gallery-page">
        <main class="main-constrained">
            <!-- Unified catalog-style header -->
            <header class="gallery-header">
                <div>
                    <p class="gh-eyebrow">{{ isSearchMode ? 'Search' : 'Gallery' }}</p>
                    <h1>{{ isSearchMode ? '搜索结果' : '公交车辆图库' }}</h1>
                    <p v-if="!isSearchMode" class="gh-subtitle">
                        收录全国公交车辆的车牌、配置与上线资料 · 共 <strong>{{ pagination.total || '--' }}</strong> 条记录
                    </p>
                    <p v-else class="gh-subtitle">
                        关键词 <mark>{{ filters.keyword }}</mark>
                        · 找到 <strong>{{ pagination.total || 0 }}</strong> 辆车
                        <template v-if="routeResults.total"> · <strong>{{ routeResults.total }}</strong> 条线路</template>
                        <template v-if="searchFacets.regions?.total"> · 覆盖 <strong>{{ searchFacets.regions.total }}</strong> 个地区</template>
                    </p>
                </div>
                <button v-if="isSearchMode" class="ghost-btn" type="button" @click="handleResetFilters">清除搜索</button>
            </header>

            <!-- Search bar + facets (search mode) -->
            <section v-if="isSearchMode" class="search-section">
                <form class="search-form" @submit.prevent="handleSearchSubmit">
                    <input
                        v-model.trim="searchInput" type="text"
                        placeholder="搜索车牌 / 车型 / 公司 / 地区 / 配置"
                        class="search-input"
                    />
                    <button class="search-btn" type="submit">搜索</button>
                </form>

                <!-- Region chips (prominent, catalog-style) -->
                <div v-if="searchFacets.regions?.items?.length" class="facet-row facet-row--regions">
                    <span class="facet-label">地区</span>
                    <button v-for="item in searchFacets.regions.items" :key="'rg-'+item.id"
                            class="filter-chip" @click="appendFacet(item.title)">{{ item.title }}</button>
                </div>

                <!-- Brand chips -->
                <div v-if="searchFacets.brands?.items?.length" class="facet-row">
                    <span class="facet-label">品牌</span>
                    <button v-for="item in searchFacets.brands.items" :key="'b-'+item.id"
                            class="facet-tag" @click="appendFacet(item.title)">{{ item.title }}</button>
                </div>

                <!-- Company chips -->
                <div v-if="searchFacets.companies?.items?.length" class="facet-row">
                    <span class="facet-label">公司</span>
                    <button v-for="item in searchFacets.companies.items" :key="'c-'+item.id"
                            class="facet-tag" @click="appendFacet(item.title)">{{ item.title }}</button>
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

                <!-- Browse mode: normal grid -->
                <div v-else-if="!isSearchMode" class="gallery-grid">
                    <VehicleCard
                        v-for="item in gallery" :key="item?.vehicle?.id || item.vehicleId"
                        :vehicle="item.vehicle" :config="item.config" :images="item.images"
                        :variants="item.variants" :variant-count="item.variantCount"
                        @view-detail="openVehicleDetail"
                    />
                </div>

                <!--
                  Search mode: flat row cards  (C1+C4 hybrid)
                  Switch searchLayout below to swap between versions:
                    "c1c4"  — colored left bar + flat card  (current)
                    "c1"    — no bar, pure minimal
                    "c2"    — capsule tags
                    "c3"    — ultra-dense table
                    "c5"    — gradient border + slide arrow
                -->
                <div v-else class="search-row-list">
                    <div
                        v-for="item in gallery" :key="'sr-'+ (item?.vehicle?.id || item.vehicleId)"
                        class="search-row"
                        :style="{ '--bar-color': brandColor(item?.vehicle?.model?.brandName) }"
                        @click="openVehicleDetail(item?.vehicle?.id)"
                    >
                        <div class="sr-bar"></div>
                        <div class="sr-image">
                            <img v-if="item.images?.[0]?.thumbnailUrl"
                                 :src="item.images[0].thumbnailUrl"
                                 :alt="item.vehicle?.plateNumber"
                                 loading="lazy" decoding="async" />
                            <div v-else class="sr-noimg"></div>
                        </div>
                        <div class="sr-info">
                            <span class="sr-plate">{{ formatPlate(item.vehicle?.plateNumber) || '未上牌' }}</span>
                            <span class="sr-model">{{ item.vehicle?.model?.name || '—' }}</span>
                            <span class="sr-company">{{ item.vehicle?.company?.name || '—' }}</span>
                            <span class="sr-region">{{ item.vehicle?.region?.name || item.vehicle?.company?.regionName || '—' }}</span>
                        </div>
                        <span class="sr-arrow">→</span>
                    </div>
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

// ---- Display helpers (search row cards) ----
const BRAND_COLORS = {
    'BYD': '#1e40af', '宇通': '#0d9488', '中通': '#7c3aed',
    '金龙': '#dc2626', '海格': '#0891b2', '福田': '#ca8a04',
    '金旅': '#2563eb', '安凯': '#059669', '申沃': '#4f46e5',
    '青年': '#e11d48', '黄海': '#ea580c', '亚星': '#9333ea'
};

const brandColor = (brandName) => {
    if (!brandName) return '#94a3b8';
    for (const [key, color] of Object.entries(BRAND_COLORS)) {
        if (brandName.includes(key)) return color;
    }
    return '#64748b';
};

const formatPlate = (plate = '') => {
    const v = String(plate || '').trim();
    if (!v) return v;
    const m = v.match(/^(.{1,2})\s*(.{5,6})$/u);
    if (m) return `${m[1]} ${m[2]}`;
    if (v.length > 2) return `${v.slice(0, 2)} ${v.slice(2)}`;
    return v;
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

// ---- Unified gallery header ----
.gallery-header {
    display: flex; justify-content: space-between; align-items: flex-start;
    margin-bottom: 28px;

    mark { background: #fef08a; color: inherit; padding: 1px 6px; border-radius: 3px; }
    strong { color: #111827; }
}
.gh-eyebrow {
    letter-spacing: 0.3em; font-size: 0.72rem; color: #9ca3af;
    text-transform: uppercase; margin: 0 0 4px;
}
.gallery-header h1 {
    margin: 0 0 6px; font-size: 1.6rem; font-weight: 700; color: #111827;
}
.gh-subtitle { margin: 0; font-size: 0.9rem; color: #6b7280; }

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
.facet-label { font-weight: 600; color: #64748b; font-size: 0.82rem; flex-shrink: 0; min-width: 36px; }
.facet-tag {
    padding: 4px 12px; border-radius: 999px; background: #eff6ff; color: #2563eb;
    font-size: 0.8rem; cursor: pointer; border: none; font-weight: 500;
    &:hover { background: #dbeafe; }
}

// Region chips: matching RegionCatalog chip style
.facet-row--regions { margin-bottom: 10px; }
.filter-chip {
    border: 1px solid rgba(37,99,235,0.2); border-radius: 999px;
    padding: 5px 14px; background: transparent; color: #2563eb;
    cursor: pointer; font-size: 0.85rem; font-weight: 500;
    transition: all 0.2s;
    &:hover { background: #2563eb; color: #fff; border-color: #2563eb; }
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

// ---- Search row cards (C1+C4 hybrid: flat card + colored bar) ----
// To switch version: change the CSS block below according to the version comments
.search-row-list {
    display: flex; flex-direction: column; gap: 6px;
}

.search-row {
    display: flex; align-items: center; gap: 14px;
    padding: 10px 14px 10px 0;
    background: #fff; border-radius: 10px;
    box-shadow: 0 1px 3px rgba(15,23,42,0.04);
    cursor: pointer; position: relative; overflow: hidden;
    transition: transform 0.18s ease, box-shadow 0.18s ease;

    &:hover {
        transform: translateY(-1px);
        box-shadow: 0 3px 12px rgba(15,23,42,0.1);
        .sr-bar { width: 5px; }
        .sr-arrow { opacity: 1; transform: translateX(0); }
    }
}

// C4: left color bar
.sr-bar {
    width: 3px; height: 48px; border-radius: 0 3px 3px 0;
    background: var(--bar-color, #94a3b8); flex-shrink: 0;
    transition: width 0.18s ease;
}

// Image
.sr-image {
    width: 64px; height: 48px; border-radius: 4px; overflow: hidden;
    flex-shrink: 0; background: #e2e8f0;
    img { width: 100%; height: 100%; object-fit: cover; display: block; }
}
.sr-noimg { width: 100%; height: 100%; background: #cbd5e1; }

// Info columns — flattened inline
.sr-info {
    flex: 1; display: flex; align-items: center; gap: 0;
    min-width: 0; font-size: 0.88rem;
    // All children share baseline, separated by ·
    > * + *::before { content: '·'; margin: 0 10px; color: #cbd5e1; }
}
.sr-plate   { font-weight: 700; color: #0f172a; white-space: nowrap; }
.sr-model   { color: #475569; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sr-company { color: #64748b; white-space: nowrap; }
.sr-region  { color: #94a3b8; white-space: nowrap; }

// C1: hover arrow slides in
.sr-arrow {
    color: #94a3b8; font-size: 1rem; flex-shrink: 0; padding-right: 8px;
    opacity: 0; transform: translateX(-6px);
    transition: opacity 0.2s ease, transform 0.2s ease;
}

// ---- Version presets (uncomment to switch) ----
// .search-row { /* v-c1: pure minimal */ box-shadow: none; border-radius: 0; border-bottom: 1px solid #f1f5f9; &:hover { background: #f8fafc; } .sr-bar { display: none; } }
// .search-row { /* v-c2: capsules */ .sr-info > * { padding: 3px 10px; border-radius: 999px; } .sr-plate { background: #1e293b; color: #fff; } .sr-model { background: #eff6ff; color: #2563eb; } .sr-company { background: #f1f5f9; color: #475569; } .sr-region { background: #f8fafc; color: #94a3b8; } }
// .search-row { /* v-c3: ultra-dense */ padding: 6px 10px; gap: 8px; box-shadow: none; border-bottom: 1px solid #f1f5f9; .sr-bar { display: none; } .sr-image { width: 48px; height: 36px; } .sr-info { font-size: 0.82rem; } .sr-arrow { display: none; } }
// .search-row { /* v-c5: gradient border */ border: 1px solid transparent; border-image: linear-gradient(to right, #e2e8f0, transparent) 1; .sr-bar { display: none; } }

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

