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
                            <div v-for="(model, idx) in company.models" :key="model.id + '_' + idx" class="model-card">
                                <img :src="model.thumbnailUrl || placeholderLogo" :alt="model.name" loading="lazy" decoding="async" />
                                <div class="model-card__body">
                                    <p class="model-name">{{ model.name }}</p>
                                    <p class="tag">{{ resolveModelBrand(model) }}</p>
                                    <p v-if="model.batchYear" class="batch-tag">{{ model.batchYear }} 年 · {{ model.vehicleCount || 0 }} 辆</p>
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

                    <section v-if="detailLoading || summaryLoading || modelCardsLoading" class="state state--loading">
                        正在加载车型卡片...
                    </section>

                    <section v-else-if="!groupedVehicleTimeline.length" class="state state--empty">
                        暂无车型数据
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
                                            v-if="isAuthenticated"
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
                                    <p class="detail-card__caption">样本年份：{{ item.sampleYear || '未知' }}</p>
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
                        <div class="vehicle-overlay__pager">
                            <button
                                class="pager-btn"
                                type="button"
                                :disabled="vehicleListLoading || vehicleListPage <= 1"
                                @click="loadVehicleListPage(vehicleListPage - 1)"
                            >
                                上一页
                            </button>
                            <span class="pager-text">
                                第 {{ vehicleListPage }} 页 · 每页 {{ vehicleListPageSize }} 条 · 共 {{ vehicleListTotal }} 条
                            </span>
                            <button
                                class="pager-btn"
                                type="button"
                                :disabled="vehicleListLoading || !vehicleListHasNext"
                                @click="loadVehicleListPage(vehicleListPage + 1)"
                            >
                                下一页
                            </button>
                        </div>
                        <div class="vehicle-overlay__content">
                            <table class="vehicle-overlay__table">
                                <thead>
                                    <tr>
                                        <th>年份</th>
                                        <th>车牌</th>
                                        <th>自编号</th>
                                        <th>照片</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-if="vehicleListLoading">
                                        <td colspan="4">正在加载...</td>
                                    </tr>
                                    <tr v-else-if="!groupedVehicleRows.length">
                                        <td colspan="4">暂无车辆</td>
                                    </tr>
                                    <tr v-for="row in groupedVehicleRows" :key="row.key">
                                        <td>
                                            <span class="year-tag">{{ row.year }}</span>
                                        </td>
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
import { computed, ref, watch, onMounted, defineAsyncComponent } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import placeholderBus from '@/assets/images/placeholder-bus.png';
import { fetchVehicleGallery } from '@/api/vehicles';
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

const modelCardsLoading = ref(false);
const modelCardMap = ref({});
const companyVehicles = ref([]);

const detailLoading = computed(() =>
    selectedCompanyId.value ? store.state.companies.detailLoadingMap[selectedCompanyId.value] : false
);

const summaryLoading = computed(() =>
    selectedCompanyId.value ? store.state.companies.summaryLoadingMap[selectedCompanyId.value] : false
);

const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const vehicleListVisible = ref(false);
const vehicleListTitle = ref('');
const vehicleListItems = ref([]);
const vehicleListPage = ref(1);
const vehicleListTotal = ref(0);
const vehicleListHasNext = ref(false);
const vehicleListLoading = ref(false);
const vehicleListPageSize = 30;
const vehicleListModelId = ref(null);
const vehicleListYear = ref(null);
const activeVehicleId = ref(null);

const groupedVehicleRows = computed(() => {
    if (!vehicleListItems.value.length) return [];
    const map = new Map();
    vehicleListItems.value.forEach((detail, index) => {
        const plate = detail.vehicle?.plateNumber?.trim() || '未上牌';
        const key = plate;
        if (!map.has(key)) {
            map.set(key, []);
        }
        map.get(key).push({ detail, index });
    });
    return Array.from(map.entries()).map(([plate, entries], groupIndex) => {
        const years = [...new Set(entries.map(({ detail }) => formatYearValue(detail.vehicle?.launchDate)))].join(', ');
        return {
            key: `${plate}-${groupIndex}`,
            plate,
            year: years,
            records: entries.map(({ detail, index }) => ({
                id: detail.vehicle?.id || `${plate}-${index}`,
                customNumber: detail.vehicle?.customNumber || '无自编号',
                vehicle: detail.vehicle,
                images: detail.images || []
            }))
        };
    });
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

const brandDisplayMap = computed(() => {
    const brands = store.state.brands.list || [];
    const map = {};
    brands.forEach((b) => {
        if (b.name) map[b.name] = b.chnName || b.name;
    });
    return map;
});

const resolveModelBrand = (model) => {
    return brandDisplayMap.value[model.brandName] || model.brandName || brandNameMap.value[model.id] || '品牌待补充';
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

const modelCardsToken = ref(0);
const loadModelCards = async (companyId, summaries = []) => {
    const token = ++modelCardsToken.value;
    if (!companyId || !Array.isArray(summaries) || !summaries.length) {
        modelCardMap.value = {};
        companyVehicles.value = [];
        modelCardsLoading.value = false;
        return;
    }
    modelCardsLoading.value = true;

    // Bulk-fetch all vehicles for the company (paginated, up to 300)
    const allVehicles = [];
    let cursor = { lastLaunch: null, lastId: null };
    let hasMore = true;
    const MAX_PAGES = 10;
    let pageCount = 0;

    while (hasMore && pageCount < MAX_PAGES) {
        pageCount++;
        const resp = await fetchVehicleGallery({
            size: 30,
            companyId,
            ...(cursor.lastLaunch ? { lastLaunch: cursor.lastLaunch } : {}),
            ...(cursor.lastId ? { lastId: cursor.lastId } : {})
        });
        const records = Array.isArray(resp?.records) ? resp.records : [];
        allVehicles.push(...records);
        if (resp?.nextLaunch != null || resp?.nextId != null) {
            cursor = { lastLaunch: resp.nextLaunch, lastId: resp.nextId };
        } else {
            hasMore = false;
        }
    }

    if (token !== modelCardsToken.value) {
        return;
    }

    companyVehicles.value = allVehicles;

    // Build summary lookup for model name fallback
    const summaryMap = {};
    summaries.forEach(s => {
        summaryMap[Number(s.modelId)] = s;
    });

    // Group vehicles by modelId + launchYear
    const modelYearMap = {};
    allVehicles.forEach(record => {
        const vehicle = record?.vehicle;
        const modelId = vehicle?.model?.id;
        if (!modelId || !summaryMap[modelId]) return;
        const year = formatYearValue(vehicle?.launchDate);
        if (!modelYearMap[modelId]) modelYearMap[modelId] = {};
        if (!modelYearMap[modelId][year]) {
            modelYearMap[modelId][year] = {
                vehicleId: vehicle.id,
                coverImage: resolveImage(record.images),
                year
            };
        }
    });

    // Build modelCardMap: each model has multiple year entries
    const cards = {};
    summaries.forEach(summary => {
        const modelId = Number(summary.modelId);
        if (!modelId) return;
        const years = modelYearMap[modelId] || {};
        if (Object.keys(years).length === 0) {
            // No vehicles found for this model — still show it with placeholder
            years['年份未知'] = {
                vehicleId: null,
                coverImage: summary?.thumbnailUrl || null,
                year: '年份未知'
            };
        }
        cards[modelId] = {
            years,
            modelName: summary.modelName || '未命名车型',
            thumbnailUrl: summary.thumbnailUrl || null
        };
    });

    modelCardMap.value = cards;
    modelCardsLoading.value = false;
};

const groupedVehicleTimeline = computed(() => {
    if (!companyModelSummaries.value.length) return [];
    const yearMap = new Map();
    companyModelSummaries.value.forEach((summary) => {
        const modelId = Number(summary?.modelId || 0);
        if (!modelId) return;
        const card = modelCardMap.value[modelId];
        if (!card) return;
        const years = card.years || {};
        Object.entries(years).forEach(([year, data]) => {
            const yearBucket = yearMap.get(year) || new Map();
            if (!yearMap.has(year)) yearMap.set(year, yearBucket);
            const key = `${modelId}-${year}`;
            if (!yearBucket.has(key)) {
                yearBucket.set(key, {
                    modelId,
                    modelName: card.modelName || summary?.modelName || '未命名车型',
                    coverImage: data.coverImage || card.thumbnailUrl || summary?.thumbnailUrl || null,
                    sampleYear: year
                });
            }
        });
    });
    return Array.from(yearMap.entries())
        .sort((a, b) => {
            const aNum = Number(a[0]);
            const bNum = Number(b[0]);
            if (Number.isNaN(aNum) && Number.isNaN(bNum)) return 0;
            if (Number.isNaN(aNum)) return 1;
            if (Number.isNaN(bNum)) return -1;
            return bNum - aNum;
        })
        .map(([year, modelMap]) => ({
            year,
            items: Array.from(modelMap.values())
        }));
});

const getCardKey = (year, item) => `${year}-${item.modelId || item.modelName}`;

const loadVehicleListPage = async (targetPage) => {
    if (!selectedCompanyId.value || !vehicleListModelId.value) return;
    const page = Math.max(1, Number(targetPage) || 1);
    vehicleListLoading.value = true;
    try {
        let filtered = companyVehicles.value.filter(record => {
            const vehicle = record?.vehicle;
            if (!vehicle) return false;
            return vehicle.model?.id === vehicleListModelId.value;
        });

        // Sort by launch year ascending
        filtered.sort((a, b) => {
            const ya = formatYearValue(a?.vehicle?.launchDate);
            const yb = formatYearValue(b?.vehicle?.launchDate);
            const na = Number(ya), nb = Number(yb);
            if (!isNaN(na) && !isNaN(nb)) return na - nb;
            if (!isNaN(na)) return -1;
            if (!isNaN(nb)) return 1;
            return ya.localeCompare(yb);
        });

        // Client-side pagination
        const start = (page - 1) * vehicleListPageSize;
        const paged = filtered.slice(start, start + vehicleListPageSize);
        vehicleListItems.value = paged;
        vehicleListPage.value = page;
        vehicleListTotal.value = filtered.length;
        vehicleListHasNext.value = start + vehicleListPageSize < filtered.length;
    } finally {
        vehicleListLoading.value = false;
    }
};

const openVehicleList = (year, item) => {
    if (!item?.modelId) return;
    vehicleListTitle.value = item.modelName;
    vehicleListModelId.value = Number(item.modelId);
    vehicleListYear.value = null;
    vehicleListPage.value = 1;
    vehicleListTotal.value = 0;
    vehicleListHasNext.value = false;
    vehicleListItems.value = [];
    vehicleListVisible.value = true;
    loadVehicleListPage(1).catch((error) => {
        console.error(error);
    });
};

const closeVehicleList = () => {
    vehicleListVisible.value = false;
    vehicleListItems.value = [];
    vehicleListModelId.value = null;
    vehicleListYear.value = null;
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
        await store.dispatch('vehicles/loadVehicleDetail', { vehicleId, force: true });
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
    async (id) => {
        closeVehicleList();

        if (!id) {
            store.dispatch('companies/loadCompanyCatalog');
            modelCardMap.value = {};
            companyVehicles.value = [];
            modelCardsLoading.value = false;
            return;
        }
        try {
            await Promise.all([
                store.dispatch('companies/loadCompanyDetail', { companyId: id }),
                store.dispatch('companies/loadCompanyModelSummaries', { companyId: id })
            ]);
            await loadModelCards(id, companyModelSummaries.value);
        } catch (error) {
            modelCardsLoading.value = false;
            console.error(error);
        }
    },
    { immediate: true }
);

onMounted(() => {
    store.dispatch('models/loadModelCatalog');
    store.dispatch('regions/loadRegions');
    store.dispatch('brands/loadBrands');
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
}
.batch-tag {
    display: inline-flex;
    align-items: center;
    padding: 3px 10px;
    background: #f0fdf4;
    border-radius: 999px;
    font-size: 0.78rem;
    color: #15803d;
    margin-top: 6px;
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

.vehicle-overlay__pager {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 12px;
    background: #f8fafc;
}

.pager-btn {
    border: 1px solid #cbd5e1;
    background: #fff;
    border-radius: 999px;
    padding: 6px 14px;
    cursor: pointer;
    color: #1e293b;
}

.pager-btn:disabled {
    cursor: not-allowed;
    opacity: 0.45;
}

.pager-text {
    color: #475569;
    font-size: 0.9rem;
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

.year-tag {
    background: #eef2ff;
    color: #1d4ed8;
    padding: 2px 8px;
    border-radius: 8px;
    font-size: 0.85rem;
    font-weight: 600;
    white-space: nowrap;
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
