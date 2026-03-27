<template>
    <div class="page company-catalog">
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Company</p>
                    <h1>按公司检索车型</h1>
                    <p class="subtitle">共 {{ catalog.length }} 家公司 / {{ modelsCount }} 种车型</p>
                </div>

                <button v-if="selectedCompanyId" class="ghost-btn catalog-back-btn" type="button" @click="clearCompanyFilter">
                    返回全部公司
                </button>
            </header>

            <template v-if="!selectedCompanyId">
                <section v-if="regionFilters.length" class="filter-bar">
                    <p class="filter-label">地区</p>
                    <div class="chip-row">
                        <button
                            v-for="region in regionFilters"
                            :key="region"
                            type="button"
                            :class="['filter-chip', { active: region === regionFilter }]"
                            @click="selectRegionFilter(region)"
                        >
                            {{ region }}
                        </button>
                    </div>
                </section>

                <section v-if="loading" class="state state--loading">
                    正在加载公司数据...
                </section>

                <section v-else-if="!filteredCompanies.length" class="state state--empty">
                    暂无公司数据
                </section>

                <section v-else class="company-list">
                    <article v-for="company in filteredCompanies" :key="company.id" class="company-block">
                        <div class="company-block__header">
                            <div>
                                <h2>{{ company.name }}</h2>
                                <p class="tag">{{ company.regionName || '地区待补充' }}</p>
                            </div>
                            <button class="text-btn" type="button" @click="viewCompany(company.id)">
                                查看详情
                            </button>
                        </div>

                        <div v-if="company.models?.length" class="model-grid">
                            <div v-for="model in company.models" :key="model.id" class="model-card">
                                <img :src="model.thumbnailUrl || placeholderLogo" :alt="model.name" loading="lazy" decoding="async" />
                                <div class="model-card__body">
                                    <p class="model-name">{{ model.name }}</p>
                                    <p class="tag">{{ resolveModelBrand(model) }}</p>
                                </div>
                                <button class="text-btn" type="button" @click="goModel(model.id)">
                                    查看车型
                                </button>
                            </div>
                        </div>

                        <p v-else class="empty-meta">暂无车型数据</p>
                    </article>
                </section>
            </template>

            <template v-else>
                <section v-if="companyDetail" class="company-detail">
                    <div class="detail-summary">
                        <div>
                            <p class="eyebrow">Company Detail</p>
                            <h1>{{ companyDetail.name }}</h1>
                            <p class="subtitle">
                                {{ companyRegionName }}
                            </p>
                        </div>
                    </div>

                    <section v-if="summaryLoading && !companyModelSummaries.length" class="state state--loading">
                        正在加载车型汇总...
                    </section>

                    <section v-else-if="!companyModelSummaries.length" class="state state--empty">
                        暂无车型汇总数据
                    </section>

                    <section v-else class="summary-grid">
                        <article
                            v-for="summary in companyModelSummaries"
                            :key="summary.modelId || summary.modelName"
                            class="summary-card"
                        >
                            <div class="summary-card__meta">
                                <p class="summary-card__title">{{ summary.modelName || '未命名车型' }}</p>
                            </div>
                            <button
                                class="text-btn"
                                type="button"
                                @click="openModelVehicles(summary)"
                            >
                                查看该车型车辆
                            </button>
                        </article>
                    </section>

                    <div class="filter-grid">
                        <div v-if="activeModelName" class="filter-line">
                            <span class="filter-label">当前车型</span>
                            <div class="chip-row">
                                <button type="button" class="filter-chip active" @click="clearModelFocus">
                                    {{ activeModelName }} · 清除
                                </button>
                            </div>
                        </div>
                        <div v-if="vehicleFilterOptions.years.length" class="filter-line">
                            <span class="filter-label">年份</span>
                            <div class="chip-row">
                                <button
                                    v-for="year in vehicleFilterOptions.years"
                                    :key="year"
                                    type="button"
                                    :class="['filter-chip', { active: vehicleFilters.year === year }]"
                                    @click="vehicleFilters.year = vehicleFilters.year === year ? '' : year"
                                >
                                    {{ year }}
                                </button>
                            </div>
                        </div>
                        <div v-if="vehicleFilterOptions.brands.length" class="filter-line">
                            <span class="filter-label">品牌</span>
                            <div class="chip-row">
                                <button
                                    v-for="brand in vehicleFilterOptions.brands"
                                    :key="brand"
                                    type="button"
                                    :class="['filter-chip', { active: vehicleFilters.brand === brand }]"
                                    @click="vehicleFilters.brand = vehicleFilters.brand === brand ? '' : brand"
                                >
                                    {{ brand }}
                                </button>
                            </div>
                        </div>
                    </div>

                    <section v-if="detailLoading || vehiclesLoading" class="state state--loading">
                        正在加载车辆明细...
                    </section>

                    <section v-else-if="!hasVehiclePayload" class="state state--empty">
                        点击上方车型卡片后，再按需加载车辆明细
                    </section>

                    <section v-else-if="!groupedVehicleTimeline.length" class="state state--empty">
                        暂无符合筛选条件的车辆数据
                    </section>

                    <section v-else class="timeline">
                        <article v-for="group in groupedVehicleTimeline" :key="group.year" class="timeline-group">
                            <header class="timeline-header">
                                <span class="timeline-year">{{ group.year }}</span>
                            </header>
                            <div class="detail-grid">
                                <div v-for="item in group.items" :key="getCardKey(group.year, item)" class="detail-card">
                                    <div class="detail-card__head">
                                        <router-link
                                            class="model-link"
                                            :to="{ name: 'ModelCatalog', params: { modelId: item.modelId } }"
                                        >
                                            {{ item.modelName }}
                                        </router-link>
                                        <button
                                            v-if="isAuthenticated && item.vehicles?.length"
                                            class="menu-btn"
                                            type="button"
                                            @click="openVehicleList(group.year, item)"
                                        >
                                            ⋮
                                        </button>
                                    </div>
                                    <div class="detail-card__image">
                                        <img :src="item.coverImage || placeholderLogo" :alt="item.modelName" loading="lazy" decoding="async" />
                                    </div>
                                    <p class="detail-card__caption">上线年份：{{ group.year }}</p>
                                </div>
                            </div>
                        </article>
                    </section>
                </section>
            </template>
        </main>

        <teleport to="body">
            <transition name="fade">
                <div v-if="vehicleListVisible" class="vehicle-overlay" @click.self="closeVehicleList">
                    <div class="vehicle-overlay__panel">
                        <header class="vehicle-overlay__header">
                            <div>
                                <p class="vehicle-overlay__eyebrow">车辆列表</p>
                                <h3>{{ vehicleListTitle }}</h3>
                            </div>
                            <button class="close-btn" type="button" @click="closeVehicleList">×</button>
                        </header>
                        <div class="vehicle-overlay__content">
                            <table class="vehicle-overlay__table">
                                <thead>
                                    <tr>
                                        <th>车牌</th>
                                        <th>自编号</th>
                                        <th>照片</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="row in groupedVehicleRows" :key="row.key">
                                        <td>
                                            <span class="plate">{{ row.plate }}</span>
                                        </td>
                                        <td>
                                            <div class="multi-value">
                                                <span
                                                    v-for="record in row.records"
                                                    :key="`${row.key}-code-${record.id}`"
                                                    class="code"
                                                >
                                                    {{ record.customNumber }}
                                                </span>
                                            </div>
                                        </td>
                                        <td>
                                            <div class="thumb-list">
                                                <button
                                                    v-for="record in row.records"
                                                    :key="`${row.key}-thumb-${record.id}`"
                                                    class="vehicle-overlay__thumb"
                                                    type="button"
                                                    @click="openVehicleDetail(record.vehicle?.id)"
                                                >
                                                    <img
                                                        :src="resolveImage(record.images) || placeholderLogo"
                                                        :alt="record.vehicle?.plateNumber || '车辆缩略图'"
                                                        loading="lazy"
                                                        decoding="async"
                                                    />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </transition>
        </teleport>

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
import { computed, ref, reactive, watch, onMounted, defineAsyncComponent } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import placeholderBus from '@/assets/images/placeholder-bus.png';
const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));

const store = useStore();
const route = useRoute();
const router = useRouter();

const catalog = computed(() => store.state.companies.catalog);
const loading = computed(() => store.state.companies.catalogLoading);
const modelsCount = computed(() => catalog.value.reduce((sum, company) => sum + (company.models?.length || 0), 0));

const selectedCompanyId = computed(() => {
    const id = route.params.companyId;
    if (!id) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
});

const placeholderLogo = placeholderBus;

const regionFilters = computed(() => {
    const set = new Set();
    catalog.value.forEach((company) => {
        if (company.regionName) {
            set.add(company.regionName);
        }
    });
    return Array.from(set);
});

const regionFilter = ref('');

const filteredCompanies = computed(() => {
    if (!regionFilter.value) return catalog.value;
    return catalog.value.filter((company) => company.regionName === regionFilter.value);
});

const selectRegionFilter = (regionName) => {
    regionFilter.value = regionFilter.value === regionName ? '' : regionName;
};

const viewCompany = (companyId) => {
    router.push({ name: 'CompanyCatalog', params: { companyId } });
};

const goModel = (modelId) => {
    router.push({ name: 'ModelCatalog', params: { modelId } });
};

const clearCompanyFilter = () => {
    router.push({ name: 'CompanyCatalog' });
};

const companyDetail = computed(() =>
    selectedCompanyId.value ? store.state.companies.detailMap[selectedCompanyId.value] || null : null
);

const companyModelSummaries = computed(() =>
    selectedCompanyId.value
        ? store.state.companies.modelSummariesByCompany[selectedCompanyId.value] || []
        : []
);

const companyVehicles = computed(() =>
    selectedCompanyId.value ? store.state.companies.vehiclesByCompany[selectedCompanyId.value] || [] : []
);

const hasVehiclePayload = computed(
    () =>
        Boolean(
            selectedCompanyId.value &&
                store.state.companies.vehiclesFetchedAtMap?.[selectedCompanyId.value]
        )
);

const activeModelId = ref(null);

const vehicleFilters = reactive({
    year: '',
    brand: ''
});

const vehicleFilterOptions = computed(() => {
    const years = new Set();
    const brands = new Set();
    companyVehicles.value.forEach((detail) => {
        const year = formatYearValue(detail.vehicle?.launchDate);
        if (year) years.add(year);
        const brandName =
            detail.vehicle?.model?.brand?.name ||
            detail.vehicleConfig?.brandName ||
            detail.vehicle?.brandName;
        if (brandName) brands.add(brandName);
    });
    return {
        years: Array.from(years),
        brands: Array.from(brands)
    };
});

const filteredCompanyVehicles = computed(() =>
    companyVehicles.value.filter((detail) => {
        const year = formatYearValue(detail.vehicle?.launchDate);
        const modelId = detail.vehicle?.model?.id || detail.vehicleConfig?.modelId || null;
        const brandName =
            detail.vehicle?.model?.brand?.name ||
            detail.vehicleConfig?.brandName ||
            detail.vehicle?.brandName;
        const matchYear = !vehicleFilters.year || vehicleFilters.year === year;
        const matchBrand = !vehicleFilters.brand || vehicleFilters.brand === brandName;
        const matchModel = !activeModelId.value || Number(activeModelId.value) === Number(modelId);
        return matchYear && matchBrand && matchModel;
    })
);

const detailLoading = computed(() =>
    selectedCompanyId.value ? store.state.companies.detailLoadingMap[selectedCompanyId.value] : false
);

const vehiclesLoading = computed(() =>
    selectedCompanyId.value ? store.state.companies.vehiclesLoadingMap[selectedCompanyId.value] : false
);

const summaryLoading = computed(() =>
    selectedCompanyId.value ? store.state.companies.summaryLoadingMap[selectedCompanyId.value] : false
);

const activeModelName = computed(() => {
    if (!activeModelId.value) return '';
    const hit = companyModelSummaries.value.find(
        (summary) => Number(summary.modelId) === Number(activeModelId.value)
    );
    return hit?.modelName || '';
});

const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const vehicleListVisible = ref(false);
const vehicleListTitle = ref('');
const vehicleListItems = ref([]);
const activeVehicleId = ref(null);

const groupedVehicleRows = computed(() => {
    if (!vehicleListItems.value.length) return [];
    const map = new Map();
    vehicleListItems.value.forEach((detail, index) => {
        const plate = detail.vehicle?.plateNumber?.trim() || '未上牌';
        if (!map.has(plate)) {
            map.set(plate, []);
        }
        map.get(plate).push({ detail, index });
    });
    return Array.from(map.entries()).map(([plate, entries], groupIndex) => ({
        key: `${plate}-${groupIndex}`,
        plate,
        records: entries.map(({ detail, index }) => ({
            id: detail.vehicle?.id || `${plate}-${index}`,
            customNumber: detail.vehicle?.customNumber || '无自编号',
            vehicle: detail.vehicle,
            images: detail.images || []
        }))
    }));
});

const resolveImage = (images = []) => {
    const first = images?.[0];
    if (!first) return null;
    return first.thumbnailUrl || null;
};

const formatYearValue = (date) => {
    if (!date) return '年份未知';
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return '年份未知';
    return `${parsed.getFullYear()}`;
};

const brandNameMap = computed(() => {
    const map = {};
    const modelsCatalog = store.state.models.catalog || [];
    modelsCatalog.forEach((model) => {
        map[model.id] = model.brandName;
    });
    return map;
});

const resolveModelBrand = (model) => {
    return model.brandName || brandNameMap.value[model.id] || '品牌待补充';
};

const regionsById = computed(() => {
    const map = {};
    const regions = store.state.regions.list || [];
    regions.forEach((region) => {
        map[region.id] = region.name;
    });
    return map;
});

const companyRegionName = computed(() => {
    const detail = companyDetail.value;
    if (!detail) return '地区待补充';
    if (detail.region?.name) {
        return detail.region.name;
    }
    if (detail.regionName) {
        return detail.regionName;
    }
    const regionId = detail.region?.id || detail.regionId;
    if (regionId && regionsById.value[regionId]) {
        return regionsById.value[regionId];
    }
    return '地区待补充';
});

const groupedVehicleTimeline = computed(() => {
    if (!filteredCompanyVehicles.value.length) return [];
    const yearMap = new Map();
    filteredCompanyVehicles.value.forEach((detail) => {
        const year = formatYearValue(detail.vehicle?.launchDate);
        const modelId = detail.vehicle?.model?.id || detail.vehicleConfig?.modelId || detail.vehicle?.id;
        const yearBucket = yearMap.get(year) || new Map();
        if (!yearMap.has(year)) {
            yearMap.set(year, yearBucket);
        }
        let entry = yearBucket.get(modelId);
        if (!entry) {
            entry = {
                modelId,
                modelName: detail.vehicle?.model?.name || detail.vehicleConfig?.modelName || '未命名车型',
                vehicles: [],
                coverImage: resolveImage(detail.images)
            };
            yearBucket.set(modelId, entry);
        }
        if (!entry.coverImage) {
            entry.coverImage = resolveImage(detail.images);
        }
        entry.vehicles.push(detail);
    });
    return Array.from(yearMap.entries())
        .sort((a, b) => Number(b[0]) - Number(a[0]))
        .map(([year, modelMap]) => ({
            year,
            items: Array.from(modelMap.values())
        }));
});

const getCardKey = (year, item) => `${year}-${item.modelId || item.modelName}`;

const openModelVehicles = async (summary) => {
    if (!selectedCompanyId.value || !summary?.modelId) return;
    activeModelId.value = Number(summary.modelId);
    try {
        await store.dispatch('companies/loadCompanyVehicles', {
            companyId: selectedCompanyId.value
        });
    } catch (error) {
        console.error(error);
    }
};

const clearModelFocus = () => {
    activeModelId.value = null;
};

const openVehicleList = (year, item) => {
    if (!Array.isArray(item.vehicles) || !item.vehicles.length) return;
    vehicleListTitle.value = `${item.modelName} · ${year}`;
    vehicleListItems.value = item.vehicles;
    vehicleListVisible.value = true;
};

const closeVehicleList = () => {
    vehicleListVisible.value = false;
    vehicleListItems.value = [];
};

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
        await store.dispatch('vehicles/loadVehicleDetail', vehicleId);
        vehicleListVisible.value = false;
    } catch (error) {
        console.error(error);
    }
};

const closeVehicleDetail = () => {
    activeVehicleId.value = null;
};

watch(
    () => selectedCompanyId.value,
    (id) => {
        vehicleFilters.year = '';
        vehicleFilters.brand = '';
        activeModelId.value = null;
        closeVehicleList();

        if (!id) {
            store.dispatch('companies/loadCompanyCatalog');
            return;
        }
        store.dispatch('companies/loadCompanyDetail', { companyId: id });
        store.dispatch('companies/loadCompanyModelSummaries', { companyId: id });
    },
    { immediate: true }
);

onMounted(() => {
    store.dispatch('models/loadModelCatalog');
    store.dispatch('regions/loadRegions');
});
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f8fafc;
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
    align-items: flex-start;
    margin-bottom: 24px;
}

.eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.25em;
    font-size: 0.75rem;
    color: #9ca3af;
    margin-bottom: 8px;
}

.subtitle {
    color: #6b7280;
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

    &.active,
    &:hover {
        background: #2563eb;
        color: #fff;
        border-color: #2563eb;
    }
}

.company-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.company-block {
    background: #fff;
    border-radius: 24px;
    padding: 28px;
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.company-block__header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 18px;
}

.tag {
    display: inline-flex;
    align-items: center;
    padding: 4px 12px;
    background: rgba(37, 99, 235, 0.1);
    border-radius: 999px;
    font-size: 0.85rem;
    color: #2563eb;
    margin-top: 6px;
}

.model-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 16px;
}

.model-card {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    background: #fdfdfd;
    display: flex;
    flex-direction: column;
    gap: 12px;
    transition: all 0.2s;

    &:hover {
        border-color: #bfdbfe;
        box-shadow: 0 10px 30px rgba(59, 130, 246, 0.15);
    }

    img {
        width: 100%;
        height: 120px;
        object-fit: cover;
        border-radius: 12px;
    }
}

.model-name {
    font-weight: 600;
}

.text-btn {
    border: none;
    background: none;
    color: #2563eb;
    font-weight: 600;
    cursor: pointer;
    text-align: left;
}

.text-btn:visited {
    color: #2563eb;
}

.empty-meta {
    color: #cbd5f5;
    font-style: italic;
}

.ghost-btn {
    border: 1px solid rgba(99, 102, 241, 0.3);
    background: transparent;
    border-radius: 24px;
    padding: 8px 18px;
    cursor: pointer;
    color: #4c1d95;

    &:hover {
        background: rgba(79, 70, 229, 0.1);
    }
}

.company-detail {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.detail-summary {
    margin-bottom: 16px;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px;
    margin-bottom: 16px;
}

.summary-card {
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 12px;
    background: #f8fafc;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.summary-card__title {
    margin: 0;
    font-weight: 600;
    color: #0f172a;
}

.filter-grid {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin: 8px 0 16px;
}

.filter-line {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.filter-label {
    font-weight: 600;
    color: #475569;
}

.chip-row {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.filter-chip {
    border: 1px solid rgba(37, 99, 235, 0.2);
    border-radius: 999px;
    padding: 6px 12px;
    background: #fff;
    color: #2563eb;
    cursor: pointer;
    transition: all 0.2s ease;

    &.active,
    &:hover {
        background: #2563eb;
        color: #fff;
        border-color: #2563eb;
    }
}

.timeline-group + .timeline-group {
    margin-top: 24px;
}

.timeline-header {
    margin-bottom: 12px;
}

.timeline-year {
    font-size: 1.1rem;
    font-weight: 600;
    color: #2563eb;
}

.detail-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
}

.detail-card {
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 12px;
    background: #f9fafb;
    position: relative;
}

.detail-card__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
}

.model-link,
.model-link:visited {
    color: #2563eb;
    text-decoration: none;
    font-weight: 600;
}

.menu-btn {
    border: none;
    background: rgba(15, 23, 42, 0.08);
    width: 28px;
    height: 28px;
    border-radius: 50%;
    cursor: pointer;
    font-size: 1.1rem;
    line-height: 1;
}

.detail-card__image {
    border-radius: 14px;
    overflow: hidden;
    margin-bottom: 8px;

    img {
        width: 100%;
        height: 140px;
        object-fit: cover;
        display: block;
    }
}

.detail-card__caption {
    margin: 0;
    font-size: 0.9rem;
    color: #475569;
}

.vehicle-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.65);
    backdrop-filter: blur(6px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    z-index: 120;
}

.vehicle-overlay__panel {
    width: min(900px, 100%);
    max-height: 90vh;
    background: #fff;
    border-radius: 28px;
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    box-shadow: 0 30px 60px rgba(15, 23, 42, 0.3);
    overflow: hidden;
}

.vehicle-overlay__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
}

.vehicle-overlay__eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.2em;
    font-size: 0.75rem;
    color: #94a3b8;
    margin-bottom: 4px;
}

.vehicle-overlay__content {
    flex: 1;
    overflow-y: auto;
    padding-right: 8px;
}

.vehicle-overlay__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.95rem;

    th,
    td {
        text-align: left;
        padding: 10px 12px;
        border-bottom: 1px solid #e2e8f0;
        color: #475569;
    }

    th {
        font-size: 0.85rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: #94a3b8;
    }
}

.vehicle-overlay__thumb {
    border: none;
    padding: 0;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    background: #0f172a;

    img {
        width: 90px;
        height: 60px;
        object-fit: cover;
        display: block;
        transition: transform 0.2s ease;
    }

    &:hover img {
        transform: scale(1.05);
    }
}

.plate {
    font-weight: 600;
    color: #1f2937;
}

.code {
    color: #94a3b8;
}

.multi-value {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.thumb-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.close-btn {
    border: none;
    background: #f1f5f9;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    font-size: 1.3rem;
    cursor: pointer;
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

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
