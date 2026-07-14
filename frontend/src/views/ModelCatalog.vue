<template>
    <div class="page model-catalog">
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Model</p>
                    <h1>按车型检索运营公司</h1>
                    <p class="subtitle">收录 {{ catalog.length }} 款车型 / {{ totalCompanies }} 家运营公司</p>
                </div>

                <button v-if="selectedModelId" class="ghost-btn catalog-back-btn" type="button" @click="clearModelFilter">
                    返回全部车型
                </button>
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
                <header class="detail-header">
                    <a class="back-link" @click.prevent="clearModelFilter">← 返回全部车型</a>
                    <div v-if="modelDetail" class="detail-headline">
                        <h1 class="detail-model-name">{{ modelDetail.name }}</h1>
                        <p class="detail-model-brand">{{ modelDetail.brand?.chnName || modelDetail.brand?.name || '品牌待补全' }}</p>
                    </div>
                </header>

                <section v-if="vehiclesLoading && !allVehicles.length" class="state state--loading">
                    正在加载车辆数据...
                </section>

                <template v-else>
                    <!-- Fleet Overview Card -->
                    <section class="fleet-overview">
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

                    <!-- Filter Chips -->
                    <section v-if="fleetCompanyNames.length > 1 || availableYears.length > 1" class="filter-bar fleet-filter-bar">
                        <div v-if="fleetCompanyNames.length > 1" class="filter-row">
                            <span class="filter-row__label">公司</span>
                            <div class="chip-row">
                                <button :class="['filter-chip', { active: !galleryCompanyFilter }]" @click="galleryCompanyFilter = ''">全部</button>
                                <button v-for="cn in fleetCompanyNames" :key="cn.name"
                                    :class="['filter-chip', { active: cn.name === galleryCompanyFilter }]"
                                    @click="galleryCompanyFilter = galleryCompanyFilter === cn.name ? '' : cn.name"
                                >{{ cn.name }}</button>
                            </div>
                        </div>
                        <div v-if="availableYears.length > 1" class="filter-row">
                            <span class="filter-row__label">年份</span>
                            <div class="chip-row">
                                <button :class="['filter-chip', { active: !galleryYearFilter }]" @click="galleryYearFilter = ''">全部</button>
                                <button v-for="y in availableYears" :key="y"
                                    :class="['filter-chip', { active: y === galleryYearFilter }]"
                                    @click="galleryYearFilter = galleryYearFilter === y ? '' : y"
                                >{{ y }}</button>
                            </div>
                        </div>
                    </section>

                    <!-- Photo Gallery -->
                    <section v-if="!filteredPhotoGroups.length && allVehicles.length" class="state state--empty">
                        暂无符合筛选条件的照片
                    </section>

                    <section v-for="group in filteredPhotoGroups" :key="group.companyName" class="photo-section">
                        <h3 class="company-section-title">{{ group.companyName }} <span class="photo-count">{{ group.photos.length }} 张</span></h3>
                        <div class="photo-grid">
                            <div v-for="photo in group.photos" :key="photo.key" class="photo-item" @click="openVehicleDetail(photo.vehicleId)">
                                <div class="photo-imgbox">
                                    <img :src="photo.thumbnailUrl || placeholderLogo" :alt="photo.plate" loading="lazy" decoding="async" />
                                </div>
                                <div class="photo-caption">
                                    <span class="photo-plate">{{ photo.plate }}</span>
                                    <span class="photo-year">{{ photo.year }}</span>
                                </div>
                            </div>
                        </div>
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
const galleryCompanyFilter = ref('');
const galleryYearFilter = ref('');

const formatYear = (date) => {
    if (!date) return '年份未知';
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return '年份未知';
    return `${parsed.getFullYear()}`;
};

// ---- Fleet Overview ----
const totalVehicleCount = computed(() => allVehicles.value.length);

const fleetCompanyNames = computed(() => {
    const map = new Map();
    allVehicles.value.forEach((r) => {
        const name = r?.vehicle?.company?.name || r?.vehicle?.companyName || '未归类';
        const id = r?.vehicle?.company?.id || r?.vehicle?.companyId;
        map.set(name, { name, id: id || null, count: (map.get(name)?.count || 0) + 1 });
    });
    return Array.from(map.values()).sort((a, b) => b.count - a.count);
});

const yearBreakdown = computed(() => {
    const map = new Map();
    allVehicles.value.forEach((r) => {
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
    allVehicles.value.forEach((r) => {
        const cn = r?.vehicle?.customNumber;
        if (cn && cn.trim()) codes.push(cn.trim());
    });
    if (!codes.length) return [];
    // Simple grouping: sort and show min~max if consecutive, else list top ranges
    codes.sort((a, b) => a.localeCompare(b, 'zh-CN', { numeric: true }));
    // Show first few and last few as ranges
    if (codes.length <= 3) return codes;
    return [`${codes[0]} ~ ${codes[codes.length - 1]}（${codes.length}个）`];
});

// ---- Photo Gallery ----
const photoData = computed(() => {
    if (!allVehicles.value.length) return [];

    const companyMap = new Map();
    allVehicles.value.forEach((record) => {
        const vehicle = record?.vehicle;
        if (!vehicle) return;
        const companyName = vehicle?.company?.name || vehicle?.companyName || '未归类公司';
        const companyId = vehicle?.company?.id || vehicle?.companyId;
        if (!companyMap.has(companyName)) {
            companyMap.set(companyName, { companyName, companyId: companyId || null, photos: [] });
        }
        const group = companyMap.get(companyName);
        const imgUrl = record.images?.[0]?.thumbnailUrl || null;
        const plate = (vehicle.plateNumber || '未上牌').trim();
        const year = formatYear(vehicle.launchDate);
        const yearNum = Number(year);
        group.photos.push({
            key: `${companyName}-${vehicle.id || Math.random()}`,
            vehicleId: vehicle.id,
            thumbnailUrl: imgUrl,
            plate,
            year,
            yearNum: Number.isNaN(yearNum) ? 9999 : yearNum,
            customNumber: vehicle.customNumber || ''
        });
    });

    // Sort photos within each company by year desc
    for (const [, group] of companyMap) {
        group.photos.sort((a, b) => b.yearNum - a.yearNum || a.plate.localeCompare(b.plate));
    }

    return Array.from(companyMap.values());
});

const availableYears = computed(() => {
    const years = new Set();
    photoData.value.forEach((g) => g.photos.forEach((p) => {
        if (p.year !== '年份未知') years.add(p.year);
    }));
    return Array.from(years).sort((a, b) => Number(b) - Number(a));
});

const filteredPhotoGroups = computed(() => {
    return photoData.value
        .map((group) => {
            const filtered = group.photos.filter((p) => {
                if (galleryCompanyFilter.value && group.companyName !== galleryCompanyFilter.value) return false;
                if (galleryYearFilter.value && p.year !== galleryYearFilter.value) return false;
                return true;
            });
            return filtered.length ? { ...group, photos: filtered } : null;
        })
        .filter(Boolean);
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
    () => selectedModelId.value,
    async (id) => {
        galleryCompanyFilter.value = '';
        galleryYearFilter.value = '';
        allVehicles.value = [];
        activeVehicleId.value = null;

        if (!id) {
            store.dispatch('models/loadModelCatalog');
            return;
        }
        await store.dispatch('models/loadModelDetail', { modelId: id });

        vehiclesLoading.value = true;
        try {
            const data = await store.dispatch('models/loadModelVehicles', {
                modelId: id,
                params: { size: 500 },
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

.filter-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
    &:last-child { margin-bottom: 0; }
}

.filter-row__label {
    font-size: 0.8rem;
    font-weight: 600;
    color: #94a3b8;
    min-width: 36px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
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

/* ---- Fleet Overview Card ---- */
.fleet-overview {
    margin-bottom: 24px;
}

.section-head {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 10px;
}

.section-title {
    margin: 0;
    font-size: 1.1rem;
    font-weight: 700;
    color: #1e293b;
}

.section-badge {
    font-size: 0.82rem;
    color: #64748b;
}

.overview-card {
    background: #fff;
    border-radius: 16px;
    padding: 20px 24px;
    box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
}

.overview-grid {
    display: flex;
    flex-direction: column;
    gap: 14px;
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

    &:hover {
        background: #dcfce7;
    }

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

.fleet-filter-bar {
    margin-bottom: 24px;
}

/* ---- Photo Gallery ---- */
.photo-section {
    margin-bottom: 32px;
}

.company-section-title {
    font-size: 1rem;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 12px;
    display: flex;
    align-items: center;
    gap: 8px;

    .photo-count {
        font-weight: 400;
        font-size: 0.82rem;
        color: #94a3b8;
    }
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

    .company-pill__body {
        min-width: 0;
    }

    .company-actions {
        margin-left: 0;
        align-self: flex-start;
    }

    .text-btn,
    .text-btn:visited {
        margin-left: 0;
        white-space: nowrap;
    }

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

    .company-pill img {
        display: none;
    }

    .company-pill__body {
        grid-column: 1 / 2;
    }

    .company-actions {
        grid-column: 2 / 3;
        grid-row: 1 / 2;
        justify-self: end;
    }

    .photo-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 6px;
    }

    .photo-caption {
        padding: 4px 6px;
    }

    .photo-plate {
        font-size: 0.75rem;
    }

    .overview-card {
        padding: 14px 16px;
    }

    .overview-item {
        flex-direction: column;
        gap: 4px;
    }
}
</style>
