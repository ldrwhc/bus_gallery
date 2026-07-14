<template>
    <div class="page model-catalog">
        <main class="catalog-main constrained">
            <!-- ========== List View Header ========== -->
            <header v-if="!selectedModelId" class="catalog-header">
                <div>
                    <p class="eyebrow">Model</p>
                    <h1>按车型检索运营公司</h1>
                    <p class="subtitle">收录 {{ catalog.length }} 款车型 / {{ totalCompanies }} 家运营公司</p>
                </div>
            </header>

            <!-- ========== Detail View Header ========== -->
            <header v-else class="detail-header">
                <a class="back-link" @click.prevent="clearModelFilter">← 返回全部车型</a>
                <div v-if="scopedCompanyName" class="detail-headline">
                    <p class="detail-company-label">公交公司</p>
                    <h1 class="detail-company-name">{{ scopedCompanyName }}</h1>
                    <p class="detail-model-sub">{{ modelDetail?.name || '车型加载中...' }}</p>
                </div>
                <div v-else-if="modelDetail" class="detail-headline">
                    <h1 class="detail-model-name">{{ modelDetail.name }}</h1>
                    <p class="detail-model-brand">{{ modelDetail.brand?.chnName || modelDetail.brand?.name || '品牌待补全' }}</p>
                </div>
            </header>

            <!-- ========== List View ========== -->
            <template v-if="!selectedModelId">
                <section v-if="brandFilters.length" class="filter-bar">
                    <p class="filter-label">品牌</p>
                    <div class="chip-row">
                        <button
                            v-for="brand in brandFilters"
                            :key="brand"
                            type="button"
                            :class="['filter-chip', { active: brand === brandFilter }]"
                            @click="selectBrandFilter(brand)"
                        >
                            {{ brand }}
                        </button>
                    </div>
                </section>

                <section v-if="loading" class="state state--loading">正在加载车型...</section>
                <section v-else-if="!filteredModels.length" class="state state--empty">暂无车型数据</section>

                <section v-else class="model-list">
                    <article v-for="model in filteredModels" :key="model.id" class="model-block">
                        <div class="model-block__header">
                            <div>
                                <h2>{{ model.name }}</h2>
                                <p class="tag">{{ brandDisplayMap[model.brandName] || model.brandName || '品牌待补全' }}</p>
                            </div>
                            <button
                                class="pill-btn"
                                type="button"
                                @click="router.push({ name: 'ModelCatalog', params: { modelId: model.id } })"
                            >
                                查看该车型
                            </button>
                        </div>

                        <div class="company-grid">
                            <div v-for="company in model.companies || []" :key="company.id" class="company-pill">
                                <img :src="company.thumbnailUrl || placeholderLogo" :alt="company.name" loading="lazy" decoding="async" />
                        <div class="company-pill__body">
                            <p class="company-name">{{ company.name }}</p>
                            <p class="company-region">
                                {{ company.regionName || regionsById[company.regionId] || '地区待补全' }}
                            </p>
                        </div>
                        <div class="company-actions">
                            <button
                                class="text-btn"
                                type="button"
                                @click="router.push({ name: 'CompanyCatalog', params: { companyId: company.id } })"
                            >
                                查看公司
                            </button>
                        </div>
                    </div>
                        </div>
                    </article>
                </section>
            </template>

            <!-- ========== Detail View: Fleet Page ========== -->
            <template v-else>
                <section v-if="vehiclesLoading && !allVehicles.length" class="state state--loading">
                    正在加载车辆数据...
                </section>

                <template v-else>
                    <!-- Fleet Config Card (company-scoped only, buspedia style) -->
                    <section v-if="isCompanyScoped" class="fleet-section">
                        <h2 class="section-title">车辆概况</h2>
                        <div class="fleet-config-card">
                            <div class="config-year-header">
                                <span v-for="yb in yearBreakdown" :key="yb.year" class="config-year-chip">
                                    {{ yb.year }} <strong>{{ yb.count }}</strong>
                                </span>
                            </div>
                            <div class="config-grid">
                                <div v-if="numberRangeText" class="config-row">
                                    <span class="config-label">自编号段</span>
                                    <span class="config-value">{{ numberRangeText }}</span>
                                </div>
                                <div class="config-row">
                                    <span class="config-label">投放年份</span>
                                    <span class="config-value">{{ yearBreakdown.map(y => y.year).join('、') || '—' }}</span>
                                </div>
                                <div class="config-row">
                                    <span class="config-label">车辆数量</span>
                                    <span class="config-value">{{ totalVehicleCount }} 辆</span>
                                </div>
                            </div>
                        </div>
                    </section>

                    <!-- Fleet Overview (non-scoped) -->
                    <section v-else class="fleet-overview">
                        <div class="section-head">
                            <h2 class="section-title">车队概况</h2>
                            <span class="section-badge">{{ totalVehicleCount }} 辆车 · {{ fleetCompanyNames.length }} 家公司</span>
                        </div>
                        <div class="overview-card">
                            <div class="overview-grid">
                                <div v-if="yearBreakdown.length" class="overview-item">
                                    <span class="overview-label">投放年份</span>
                                    <div class="overview-chips">
                                        <span v-for="yb in yearBreakdown" :key="yb.year" class="year-chip">
                                            {{ yb.year }} <strong>{{ yb.count }}</strong>
                                        </span>
                                    </div>
                                </div>
                                <div v-if="fleetCompanyNames.length" class="overview-item">
                                    <span class="overview-label">运营公司</span>
                                    <div class="overview-chips">
                                        <span v-for="cn in fleetCompanyNames" :key="cn.name" class="company-chip"
                                            @click="router.push({ name: 'CompanyCatalog', params: { companyId: cn.id } })"
                                        >{{ cn.name }} <strong>{{ cn.count }}</strong></span>
                                    </div>
                                </div>
                                <div v-if="numberRanges.length" class="overview-item">
                                    <span class="overview-label">自编号段</span>
                                    <div class="overview-chips">
                                        <span v-for="nr in numberRanges" :key="nr" class="range-chip">{{ nr }}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>

                    <!-- Photos: "使用线路" style -->
                    <section class="photo-gallery-section">
                        <h2 class="section-title">车辆照片</h2>
                        <div class="photo-grid">
                            <div v-for="photo in allPhotos" :key="photo.key" class="photo-item" @click="openVehicleDetail(photo.vehicleId)">
                                <div class="photo-imgbox">
                                    <img :src="photo.thumbnailUrl || placeholderLogo" :alt="photo.plate" loading="lazy" decoding="async" />
                                </div>
                                <div class="photo-caption">
                                    <span class="photo-plate">{{ photo.plate }}</span>
                                    <span class="photo-year">{{ photo.year }}</span>
                                </div>
                            </div>
                        </div>
                        <p v-if="!allPhotos.length" class="photo-empty">暂无车辆照片</p>
                    </section>
                </template>
            </template>
        </main>

        <VehicleDetailModal
            v-if="vehicleDetailVisible"
            :visible="vehicleDetailVisible"
            :detail="activeVehicleDetail"
            :loading="activeVehicleLoading"
            @close="closeVehicleDetail"
        />
    </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, defineAsyncComponent } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import placeholderBus from '@/assets/images/placeholder-bus.png';

const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));

const store = useStore();
const route = useRoute();
const router = useRouter();

const catalog = computed(() => store.state.models.catalog);
const loading = computed(() => store.state.models.catalogLoading);
const totalCompanies = computed(() => catalog.value.reduce((sum, model) => sum + (model.companies?.length || 0), 0));

const selectedModelId = computed(() => {
    const id = route.params.modelId;
    if (!id) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
});

const scopedCompanyId = computed(() => {
    const cid = route.query.companyId;
    if (!cid) return null;
    const numeric = Number(cid);
    return Number.isNaN(numeric) || numeric <= 0 ? null : numeric;
});

const isCompanyScoped = computed(() => Boolean(scopedCompanyId.value));

const brandFilters = computed(() => {
    const set = new Set();
    catalog.value.forEach((model) => {
        if (model.brandName) set.add(model.brandName);
    });
    return Array.from(set);
});

const brandFilter = ref('');

const baseModels = computed(() => {
    if (!selectedModelId.value) return catalog.value;
    return catalog.value.filter((model) => Number(model.id) === selectedModelId.value);
});

const filteredModels = computed(() => {
    if (!brandFilter.value) return baseModels.value;
    return baseModels.value.filter((model) => model.brandName === brandFilter.value);
});

const selectBrandFilter = (brandName) => {
    brandFilter.value = brandFilter.value === brandName ? '' : brandName;
};

const modelDetail = computed(() =>
    selectedModelId.value ? store.state.models.detailMap[selectedModelId.value] || null : null
);

const placeholderLogo = placeholderBus;

const brandDisplayMap = computed(() => {
    const brands = store.state.brands.list || [];
    const map = {};
    brands.forEach((b) => {
        if (b.name) map[b.name] = b.chnName || b.name;
    });
    return map;
});

const regionsById = computed(() => {
    const map = {};
    const regions = store.state.regions.list || [];
    regions.forEach((region) => {
        map[region.id] = region.name;
    });
    return map;
});

const clearModelFilter = () => {
    router.push({ name: 'ModelCatalog' });
};

// ===================== Detail View =====================
const allVehicles = ref([]);
const vehiclesLoading = ref(false);

const scopedCompanyName = computed(() => {
    if (!isCompanyScoped.value || !allVehicles.value.length) return '';
    const first = allVehicles.value[0];
    return first?.vehicle?.company?.name || first?.vehicle?.companyName || '';
});

const formatYear = (date) => {
    if (!date) return '年份未知';
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return '年份未知';
    return `${parsed.getFullYear()}`;
};

// ---- Filtered Vehicles (client-side safety net) ----
const displayVehicles = computed(() => {
    if (!isCompanyScoped.value) return allVehicles.value;
    const cid = scopedCompanyId.value;
    return allVehicles.value.filter((r) => {
        const vcid = r?.vehicle?.company?.id || r?.vehicle?.companyId;
        return Number(vcid) === cid;
    });
});

// ---- Fleet Overview Data ----
const totalVehicleCount = computed(() => displayVehicles.value.length);

const fleetCompanyNames = computed(() => {
    const map = new Map();
    displayVehicles.value.forEach((r) => {
        const name = r?.vehicle?.company?.name || r?.vehicle?.companyName || '未归类';
        const id = r?.vehicle?.company?.id || r?.vehicle?.companyId;
        map.set(name, { name, id: id || null, count: (map.get(name)?.count || 0) + 1 });
    });
    return Array.from(map.values()).sort((a, b) => b.count - a.count);
});

const yearBreakdown = computed(() => {
    const map = new Map();
    displayVehicles.value.forEach((r) => {
        const y = formatYear(r?.vehicle?.launchDate);
        map.set(y, (map.get(y) || 0) + 1);
    });
    return Array.from(map.entries())
        .map(([year, count]) => ({ year, count }))
        .sort((a, b) => {
            const na = Number(a.year), nb = Number(b.year);
            if (Number.isNaN(na) && Number.isNaN(nb)) return 0;
            if (Number.isNaN(na)) return 1;
            if (Number.isNaN(nb)) return -1;
            return nb - na;
        });
});

const numberRanges = computed(() => {
    const codes = [];
    displayVehicles.value.forEach((r) => {
        const cn = r?.vehicle?.customNumber;
        if (cn && cn.trim()) codes.push(cn.trim());
    });
    if (!codes.length) return [];
    codes.sort((a, b) => a.localeCompare(b, 'zh-CN', { numeric: true }));
    if (codes.length <= 3) return codes;
    return [`${codes[0]} ~ ${codes[codes.length - 1]}（${codes.length}个）`];
});

const numberRangeText = computed(() => {
    const codes = [];
    displayVehicles.value.forEach((r) => {
        const cn = r?.vehicle?.customNumber;
        if (cn && cn.trim()) codes.push(cn.trim());
    });
    if (!codes.length) return '';
    codes.sort((a, b) => a.localeCompare(b, 'zh-CN', { numeric: true }));
    if (codes.length === 1) return codes[0];
    // Find contiguous ranges
    const ranges = [];
    let start = codes[0], end = codes[0];
    for (let i = 1; i < codes.length; i++) {
        if (codes[i] === end || i > 0) {
            // Non-numeric comparison for codes
            end = codes[i];
        }
    }
    return `${codes[0]} ~ ${codes[codes.length - 1]}`;
});

// ---- Photos ----
const allPhotos = computed(() => {
    if (!displayVehicles.value.length) return [];
    const photos = [];
    displayVehicles.value.forEach((record) => {
        const vehicle = record?.vehicle;
        if (!vehicle) return;
        const imgUrl = record.images?.[0]?.thumbnailUrl || null;
        if (!imgUrl) return; // skip vehicles without photos
        photos.push({
            key: `${vehicle.id || Math.random()}`,
            vehicleId: vehicle.id,
            thumbnailUrl: imgUrl,
            plate: (vehicle.plateNumber || '未上牌').trim(),
            year: formatYear(vehicle.launchDate),
            companyName: vehicle?.company?.name || vehicle?.companyName || '',
            yearNum: Number(formatYear(vehicle.launchDate)) || 9999
        });
    });
    // Sort by year desc
    photos.sort((a, b) => b.yearNum - a.yearNum || a.plate.localeCompare(b.plate));
    return photos;
});

// ---- Vehicle Detail Modal ----
const activeVehicleId = ref(null);
const vehicleDetailVisible = computed(() => Boolean(activeVehicleId.value));
const activeVehicleDetail = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] || null : null
);
const activeVehicleLoading = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailLoadingMap[activeVehicleId.value] || false : false
);

const openVehicleDetail = async (vehicleId) => {
    if (!vehicleId) return;
    activeVehicleId.value = vehicleId;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', { vehicleId, force: true });
    } catch (error) {
        console.error(error);
    }
};

const closeVehicleDetail = () => {
    activeVehicleId.value = null;
};

// ---- Watcher ----
watch(
    () => [selectedModelId.value, scopedCompanyId.value],
    async ([modelId, companyId]) => {
        allVehicles.value = [];
        activeVehicleId.value = null;

        if (!modelId) {
            store.dispatch('models/loadModelCatalog');
            return;
        }
        await store.dispatch('models/loadModelDetail', { modelId });

        vehiclesLoading.value = true;
        try {
            const params = { size: 500 };
            if (companyId) params.companyId = companyId;
            const data = await store.dispatch('models/loadModelVehicles', {
                modelId,
                params,
                force: true
            });
            allVehicles.value = Array.isArray(data) ? data : [];
        } catch (error) {
            console.error(error);
            allVehicles.value = [];
        } finally {
            vehiclesLoading.value = false;
        }
    },
    { immediate: true }
);

onMounted(() => {
    store.dispatch('regions/loadRegions');
    store.dispatch('brands/loadBrands');
});
</script>

<style scoped lang="scss">
/* ========== Shared ========== */
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f1f5f9;
}

.constrained {
    width: min(1200px, 100%);
    margin: 0 auto;
    flex: 1;
    padding: 32px 24px 72px;
}

.catalog-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 24px;
    align-items: flex-start;
    gap: 12px;
    flex-wrap: wrap;
    h1 { margin: 0 0 6px; font-size: 1.6rem; font-weight: 700; color: #111827; }
}

.eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.3em;
    font-size: 0.72rem;
    color: #9ca3af;
    margin: 0 0 4px;
}

.subtitle {
    margin: 0;
    font-size: 0.9rem;
    color: #6b7280;
}

.ghost-btn {
    border: 1px solid rgba(37, 99, 235, 0.3);
    border-radius: 999px;
    height: 30px;
    padding: 0 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    white-space: nowrap;
    background: rgba(37, 99, 235, 0.06);
    color: #1d4ed8;
    font-weight: 500;
    font-size: 0.78rem;
    line-height: 1;
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    &:hover {
        background: rgba(37, 99, 235, 0.12);
        border-color: rgba(37, 99, 235, 0.42);
        color: #1e40af;
    }
}

.state {
    border-radius: 16px;
    padding: 48px;
    text-align: center;
    background: #fff;
    color: #475569;

    &--loading {
        background: #dbeafe;
        color: #1d4ed8;
    }

    &--empty {
        color: #94a3b8;
    }
}

.pill-btn {
    border: 1px solid rgba(37, 99, 235, 0.3);
    border-radius: 999px;
    height: 30px;
    padding: 0 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    white-space: nowrap;
    background: rgba(37, 99, 235, 0.06);
    color: #1d4ed8;
    font-weight: 500;
    font-size: 0.78rem;
    line-height: 1;
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    &:hover {
        background: rgba(37, 99, 235, 0.12);
        border-color: rgba(37, 99, 235, 0.42);
        color: #1e40af;
    }
}

.filter-bar {
    background: #fff;
    border-radius: 18px;
    padding: 16px 20px;
    margin-bottom: 24px;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.filter-label {
    margin: 0 0 8px;
    font-size: 0.9rem;
    color: #475569;
}

.chip-row {
    display: flex;
    gap: 8px;
    overflow-x: auto;
}

.filter-chip {
    border: 1px solid rgba(37, 99, 235, 0.2);
    border-radius: 999px;
    padding: 6px 16px;
    background: transparent;
    color: #2563eb;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;

    &.active,
    &:hover {
        background: #2563eb;
        color: #fff;
        border-color: #2563eb;
    }
}

/* ========== List View ========== */
.model-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.model-block {
    background: #fff;
    border-radius: 24px;
    padding: 28px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.model-block__header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 18px;
}

.tag {
    margin-top: 6px;
    display: inline-flex;
    padding: 4px 12px;
    border-radius: 999px;
    background: rgba(37, 99, 235, 0.1);
    color: #2563eb;
    font-size: 0.85rem;
}

.company-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
}

.company-pill {
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 14px 20px;
    display: flex;
    gap: 16px;
    align-items: center;
    background: #f8fafc;
    white-space: normal;
    justify-content: space-between;
    min-width: 0;

    img {
        width: 80px;
        height: 60px;
        object-fit: cover;
        border-radius: 12px;
    }
}

.company-pill__body {
    flex: 1 1 0;
    min-width: 0;
}

.company-name {
    font-weight: 600;
    white-space: normal;
    overflow-wrap: anywhere;
    line-height: 1.35;
}

.company-region {
    color: #475569;
    white-space: normal;
    line-height: 1.35;
}

.company-actions {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    margin-left: 8px;
}

.text-btn,
.text-btn:visited {
    border: none;
    background: none;
    color: #2563eb;
    font-weight: 600;
    cursor: pointer;
    margin-left: auto;
}

/* ========== Detail View ========== */
.detail-header {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 24px;
}

.back-link {
    color: #2563eb;
    text-decoration: none;
    font-size: 14px;
    display: inline-block;
    cursor: pointer;
    &:hover { text-decoration: underline; }
}

.detail-headline {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.detail-company-label {
    margin: 0;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.3em;
    color: #9ca3af;
}

.detail-company-name {
    margin: 0;
    font-size: 1.5rem;
    font-weight: 700;
    color: #111827;
}

.detail-model-sub {
    margin: 0;
    font-size: 1rem;
    color: #6b7280;
}

.detail-model-name {
    margin: 0;
    font-size: 1.6rem;
    font-weight: 700;
    color: #111827;
}

.detail-model-brand {
    margin: 0;
    font-size: 0.95rem;
    color: #6b7280;
}

/* ---- Section Title ---- */
.section-title {
    margin: 0 0 12px;
    font-size: 1.05rem;
    font-weight: 700;
    color: #1e293b;
}

/* ---- Fleet Config Card (buspedia style) ---- */
.fleet-section {
    margin-bottom: 28px;
}

.fleet-config-card {
    background: #fff;
    border-radius: 14px;
    padding: 18px 20px;
    box-shadow: 0 1px 6px rgba(15, 23, 42, 0.05);
}

.config-year-header {
    display: flex;
    gap: 8px;
    margin-bottom: 14px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f1f5f9;
    flex-wrap: wrap;
}

.config-year-chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 12px;
    border-radius: 8px;
    background: #00695c;
    color: #fff;
    font-size: 0.85rem;
    font-weight: 500;

    strong {
        background: rgba(255,255,255,0.2);
        padding: 1px 6px;
        border-radius: 6px;
        font-size: 0.78rem;
    }
}

.config-grid {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.config-row {
    display: flex;
    align-items: center;
    gap: 12px;
}

.config-label {
    font-size: 0.82rem;
    color: #94a3b8;
    min-width: 64px;
}

.config-value {
    font-size: 0.88rem;
    color: #334155;
    font-weight: 500;
}

/* ---- Fleet Overview (non-scoped) ---- */
.fleet-overview {
    margin-bottom: 28px;
}

.section-head {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 10px;
}

.section-badge {
    font-size: 0.82rem;
    color: #64748b;
    margin-left: 4px;
}

.overview-card {
    background: #fff;
    border-radius: 14px;
    padding: 18px 20px;
    box-shadow: 0 1px 6px rgba(15, 23, 42, 0.05);
}

.overview-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.overview-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
}

.overview-label {
    font-size: 0.82rem;
    color: #94a3b8;
    min-width: 64px;
    padding-top: 2px;
}

.overview-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.year-chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 12px;
    border-radius: 999px;
    background: #eef2ff;
    color: #1d4ed8;
    font-size: 0.85rem;
    font-weight: 500;

    strong {
        background: #fff;
        padding: 1px 6px;
        border-radius: 8px;
        font-size: 0.78rem;
        color: #0f172a;
    }
}

.company-chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 12px;
    border-radius: 999px;
    background: #f0fdf4;
    color: #15803d;
    font-size: 0.85rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.15s;

    &:hover { background: #dcfce7; }

    strong {
        background: #fff;
        padding: 1px 6px;
        border-radius: 8px;
        font-size: 0.78rem;
        color: #0f172a;
    }
}

.range-chip {
    display: inline-flex;
    padding: 4px 12px;
    border-radius: 999px;
    background: #fff7ed;
    color: #c2410c;
    font-size: 0.85rem;
    font-weight: 500;
}

/* ---- Photo Gallery ---- */
.photo-gallery-section {
    margin-top: 0;
}

.photo-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 12px;
}

.photo-item {
    cursor: pointer;
    border-radius: 12px;
    overflow: hidden;
    transition: transform 0.15s, box-shadow 0.15s;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }
}

.photo-imgbox {
    width: 100%;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    background: #e2e8f0;

    img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
    }
}

.photo-caption {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 10px;
    background: #fff;
    border: 1px solid #f1f5f9;
    border-top: none;
    border-radius: 0 0 12px 12px;
}

.photo-plate {
    font-size: 0.82rem;
    font-weight: 600;
    color: #1e293b;
}

.photo-year {
    font-size: 0.75rem;
    color: #94a3b8;
    background: #f8fafc;
    padding: 1px 8px;
    border-radius: 6px;
}

.photo-empty {
    text-align: center;
    color: #94a3b8;
    padding: 32px;
    background: #fff;
    border-radius: 14px;
}

/* ========== Responsive ========== */
@media (max-width: 900px) {
    .company-grid {
        grid-template-columns: 1fr;
    }

    .model-block {
        padding: 18px;
    }

    .model-block__header {
        flex-wrap: wrap;
        gap: 10px;
    }

    .company-pill {
        padding: 12px;
        gap: 10px;
        align-items: flex-start;
    }

    .company-pill img {
        width: 70px;
        height: 52px;
        flex: 0 0 auto;
    }

    .company-pill__body { min-width: 0; }
    .company-actions { margin-left: 0; align-self: flex-start; }
    .text-btn, .text-btn:visited { margin-left: 0; white-space: nowrap; }

    .photo-grid {
        grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
        gap: 8px;
    }
}

@media (max-width: 560px) {
    .company-pill {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 8px 10px;
    }

    .company-pill img { display: none; }
    .company-pill__body { grid-column: 1 / 2; }
    .company-actions { grid-column: 2 / 3; grid-row: 1 / 2; justify-self: end; }

    .photo-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 6px;
    }

    .photo-caption { padding: 4px 6px; }
    .photo-plate { font-size: 0.75rem; }

    .overview-card, .fleet-config-card { padding: 14px 16px; }
    .overview-item { flex-direction: column; gap: 4px; }
}
</style>
