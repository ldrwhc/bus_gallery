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

            <!-- ============ Detail View: Vehicle Table ============ -->
            <template v-else>
                <header class="detail-header">
                    <a class="back-link" @click.prevent="clearModelFilter">← 返回全部车型</a>
                    <div v-if="modelDetail">
                        <h1 class="detail-model-name">{{ modelDetail.name }}</h1>
                        <p class="detail-model-meta">
                            {{ modelDetail.brand?.chnName || modelDetail.brand?.name || '品牌待补全' }}
                            <template v-if="totalVehicleCount"> · {{ totalVehicleCount }} 辆车</template>
                            <template v-if="availableCompanies.length"> · {{ availableCompanies.length }} 家公司</template>
                        </p>
                    </div>
                </header>

                <!-- Filters -->
                <section v-if="availableCompanies.length > 1 || availableYears.length > 1" class="detail-filters">
                    <div v-if="availableCompanies.length > 1" class="filter-row">
                        <span class="filter-row__label">公司</span>
                        <div class="chip-row">
                            <button
                                :class="['filter-chip', { active: !selectedCompany }]"
                                @click="selectedCompany = ''"
                            >全部</button>
                            <button
                                v-for="c in availableCompanies" :key="c"
                                :class="['filter-chip', { active: c === selectedCompany }]"
                                @click="selectedCompany = selectedCompany === c ? '' : c"
                            >{{ c }}</button>
                        </div>
                    </div>
                    <div v-if="availableYears.length > 1" class="filter-row">
                        <span class="filter-row__label">年份</span>
                        <div class="chip-row">
                            <button
                                :class="['filter-chip', { active: !selectedYear }]"
                                @click="selectedYear = ''"
                            >全部</button>
                            <button
                                v-for="y in availableYears" :key="y"
                                :class="['filter-chip', { active: y === selectedYear }]"
                                @click="selectedYear = selectedYear === y ? '' : y"
                            >{{ y }}</button>
                        </div>
                    </div>
                </section>

                <!-- Loading -->
                <section v-if="vehiclesLoading && !allVehicles.length" class="state state--loading">
                    正在加载车辆数据...
                </section>

                <!-- Empty -->
                <section v-else-if="!filteredVehicleTotal" class="state state--empty">
                    暂无车辆数据
                </section>

                <!-- Vehicle Table -->
                <template v-else>
                    <!-- Pager top -->
                    <div class="pager-bar">
                        <button class="pager-btn" :disabled="vehiclesLoading || currentPage <= 1" @click="currentPage--">上一页</button>
                        <span class="pager-text">第 {{ currentPage }} 页 · 每页 {{ pageSize }} 条 · 共 {{ filteredVehicleTotal }} 条</span>
                        <button class="pager-btn" :disabled="vehiclesLoading || !hasNextPage" @click="currentPage++">下一页</button>
                    </div>

                    <div class="vehicle-table-wrap">
                        <template v-for="group in paginatedVehicleGroups" :key="group.companyName">
                            <h3 class="company-section-title">{{ group.companyName }}</h3>
                            <div class="vehicle-table">
                                <div class="vehicle-table__head">
                                    <span class="col-year">年份</span>
                                    <span class="col-plate">车牌</span>
                                    <span class="col-code">自编号</span>
                                    <span class="col-photo">照片</span>
                                </div>
                                <div v-for="row in group.rows" :key="row.key" class="vehicle-table__row">
                                    <span class="col-year">
                                        <span class="year-tag">{{ row.year }}</span>
                                    </span>
                                    <span class="col-plate">{{ row.plate }}</span>
                                    <span class="col-code">
                                        <span
                                            v-for="(code, ci) in row.customNumbers"
                                            :key="ci"
                                            class="code-item"
                                        >{{ code }}</span>
                                    </span>
                                    <span class="col-photo">
                                        <button
                                            v-for="rec in row.records"
                                            :key="rec.vehicleId"
                                            class="thumb-btn"
                                            type="button"
                                            @click="openVehicleDetail(rec.vehicleId)"
                                        >
                                            <img
                                                :src="rec.thumbnailUrl || placeholderLogo"
                                                :alt="row.plate"
                                                loading="lazy"
                                                decoding="async"
                                            />
                                        </button>
                                    </span>
                                </div>
                            </div>
                        </template>
                    </div>

                    <!-- Pager bottom -->
                    <div class="pager-bar pager-bar--bottom">
                        <button class="pager-btn" :disabled="vehiclesLoading || currentPage <= 1" @click="currentPage--">上一页</button>
                        <span class="pager-text">第 {{ currentPage }} 页 · 每页 {{ pageSize }} 条 · 共 {{ filteredVehicleTotal }} 条</span>
                        <button class="pager-btn" :disabled="vehiclesLoading || !hasNextPage" @click="currentPage++">下一页</button>
                    </div>
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
        if (model.brandName) {
            set.add(model.brandName);
        }
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

// ===================== Detail View State =====================
const allVehicles = ref([]);
const vehiclesLoading = ref(false);
const currentPage = ref(1);
const pageSize = 30;
const selectedCompany = ref('');
const selectedYear = ref('');

const formatYearValue = (date) => {
    if (!date) return '年份未知';
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return '年份未知';
    return `${parsed.getFullYear()}`;
};

// Group all vehicles by company → plate, collect years + custom numbers + thumbnails
const vehicleRows = computed(() => {
    if (!allVehicles.value.length) return [];

    // Group by company first
    const companyMap = new Map();
    allVehicles.value.forEach((record) => {
        const vehicle = record?.vehicle;
        if (!vehicle) return;
        const companyName = vehicle?.company?.name || vehicle?.companyName || '未归类公司';
        if (!companyMap.has(companyName)) {
            companyMap.set(companyName, []);
        }
        companyMap.get(companyName).push(record);
    });

    const result = [];
    for (const [companyName, records] of companyMap) {
        // Within a company, group by plate
        const plateMap = new Map();
        records.forEach((record) => {
            const vehicle = record?.vehicle;
            const plate = (vehicle?.plateNumber || '未上牌').trim();
            const key = plate;
            if (!plateMap.has(key)) {
                plateMap.set(key, []);
            }
            plateMap.get(key).push(record);
        });

        const rows = [];
        for (const [plate, entries] of plateMap) {
            const years = [...new Set(entries.map((e) => formatYearValue(e.vehicle?.launchDate)))].sort();
            const customNumbers = [...new Set(entries.map((e) => e.vehicle?.customNumber).filter(Boolean))];
            if (!customNumbers.length) customNumbers.push('—');

            const records_out = entries.map((e) => ({
                vehicleId: e.vehicle?.id,
                thumbnailUrl: e.images?.[0]?.thumbnailUrl || null
            })).filter((r) => r.vehicleId);

            rows.push({
                key: `${companyName}-${plate}`,
                plate,
                year: years.join(', '),
                customNumbers,
                records: records_out,
                _yearNums: years.map((y) => Number(y)).filter((n) => !Number.isNaN(n))
            });
        }

        result.push({ companyName, rows });
    }

    return result;
});

// Available filter options
const availableCompanies = computed(() => {
    const names = new Set();
    vehicleRows.value.forEach((g) => names.add(g.companyName));
    return Array.from(names).sort((a, b) => a.localeCompare(b, 'zh-CN'));
});

const availableYears = computed(() => {
    const years = new Set();
    vehicleRows.value.forEach((g) => {
        g.rows.forEach((row) => {
            row._yearNums.forEach((n) => years.add(n));
        });
    });
    return Array.from(years).sort((a, b) => b - a).map(String);
});

// Filtered rows
const filteredVehicleRows = computed(() => {
    return vehicleRows.value
        .map((group) => {
            const filtered = group.rows.filter((row) => {
                if (selectedCompany.value && group.companyName !== selectedCompany.value) return false;
                if (selectedYear.value && !row._yearNums.includes(Number(selectedYear.value))) return false;
                return true;
            });
            return filtered.length ? { ...group, rows: filtered } : null;
        })
        .filter(Boolean);
});

// Total count across all filtered rows
const filteredVehicleTotal = computed(() =>
    filteredVehicleRows.value.reduce((sum, g) => sum + g.rows.length, 0)
);

const totalVehicleCount = computed(() =>
    vehicleRows.value.reduce((sum, g) => sum + g.rows.length, 0)
);

// Pagination
const hasNextPage = computed(() => currentPage.value * pageSize < filteredVehicleTotal.value);

const paginatedVehicleGroups = computed(() => {
    let skipped = (currentPage.value - 1) * pageSize;
    const paged = [];
    for (const group of filteredVehicleRows.value) {
        if (skipped >= group.rows.length) {
            skipped -= group.rows.length;
            continue;
        }
        const slice = group.rows.slice(skipped);
        skipped = 0;
        if (slice.length) {
            paged.push({ companyName: group.companyName, rows: slice });
        }
        const taken = paged.reduce((s, g) => s + g.rows.length, 0);
        if (taken >= pageSize) break;
    }
    // Trim last group if exceeds page
    let taken = 0;
    const trimmed = [];
    for (const g of paged) {
        const remaining = pageSize - taken;
        if (remaining <= 0) break;
        trimmed.push({ companyName: g.companyName, rows: g.rows.slice(0, remaining) });
        taken += trimmed[trimmed.length - 1].rows.length;
    }
    return trimmed;
});

// ===================== Vehicle Detail Modal =====================
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

// ===================== Watchers =====================
watch(
    () => selectedModelId.value,
    async (id) => {
        selectedCompany.value = '';
        selectedYear.value = '';
        currentPage.value = 1;
        allVehicles.value = [];
        activeVehicleId.value = null;

        if (!id) {
            store.dispatch('models/loadModelCatalog');
            return;
        }
        await store.dispatch('models/loadModelDetail', { modelId: id });

        // Load vehicles for this model
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

// Reset page when filters change
watch([selectedCompany, selectedYear], () => {
    currentPage.value = 1;
});

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
    margin-bottom: 20px;
}

.back-link {
    color: #2563eb;
    text-decoration: none;
    font-size: 14px;
    display: inline-block;
    cursor: pointer;
    &:hover { text-decoration: underline; }
}

.detail-model-name {
    margin: 0;
    font-size: 1.5rem;
    font-weight: 700;
    color: #111827;
}

.detail-model-meta {
    margin: 4px 0 0;
    font-size: 0.88rem;
    color: #6b7280;
}

.detail-filters {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #fff;
    border-radius: 14px;
    box-shadow: 0 1px 6px rgba(15, 23, 42, 0.05);
}

.filter-row {
    display: flex;
    align-items: center;
    gap: 10px;
}

.filter-row__label {
    font-size: 0.8rem;
    font-weight: 600;
    color: #94a3b8;
    min-width: 36px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

/* ========== Pager ========== */
.pager-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 14px;
    border-radius: 12px;
    background: #fff;
    margin-bottom: 16px;
    box-shadow: 0 1px 6px rgba(15, 23, 42, 0.04);

    &--bottom {
        margin-top: 20px;
        margin-bottom: 0;
    }
}

.pager-btn {
    border: 1px solid #cbd5e1;
    background: #fff;
    border-radius: 999px;
    padding: 6px 16px;
    cursor: pointer;
    color: #1e293b;
    font-size: 0.85rem;
    transition: background 0.15s;

    &:hover:not(:disabled) {
        background: #f1f5f9;
    }

    &:disabled {
        cursor: not-allowed;
        opacity: 0.4;
    }
}

.pager-text {
    color: #475569;
    font-size: 0.85rem;
}

/* ========== Vehicle Table ========== */
.vehicle-table-wrap {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.company-section-title {
    font-size: 1rem;
    font-weight: 700;
    color: #1e293b;
    margin: 0;
    padding: 10px 16px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
    display: flex;
    align-items: center;
    gap: 8px;

    &::before {
        content: '🏢';
        font-size: 0.9rem;
    }
}

.vehicle-table {
    background: #fff;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 1px 6px rgba(15, 23, 42, 0.05);
}

.vehicle-table__head {
    display: grid;
    grid-template-columns: 80px 1fr 1fr auto;
    gap: 12px;
    padding: 10px 18px;
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    font-size: 0.78rem;
    font-weight: 600;
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.04em;
}

.vehicle-table__row {
    display: grid;
    grid-template-columns: 80px 1fr 1fr auto;
    gap: 12px;
    padding: 10px 18px;
    align-items: center;
    border-bottom: 1px solid #f1f5f9;

    &:last-child {
        border-bottom: none;
    }

    &:hover {
        background: #fafbff;
    }
}

.col-year {
    min-width: 0;
}

.col-plate {
    font-weight: 600;
    color: #1e293b;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.col-code {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    min-width: 0;
}

.code-item {
    color: #64748b;
    font-size: 0.85rem;
}

.col-photo {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    justify-content: flex-end;
}

.year-tag {
    display: inline-block;
    background: #eef2ff;
    color: #1d4ed8;
    padding: 2px 8px;
    border-radius: 8px;
    font-size: 0.82rem;
    font-weight: 600;
    white-space: nowrap;
}

.thumb-btn {
    border: none;
    padding: 0;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    background: #e2e8f0;
    transition: transform 0.15s;

    img {
        width: 80px;
        height: 54px;
        object-fit: cover;
        display: block;
    }

    &:hover {
        transform: scale(1.06);
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }
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

    .vehicle-table__head,
    .vehicle-table__row {
        grid-template-columns: 60px 1fr 80px auto;
        gap: 8px;
        padding: 8px 12px;
        font-size: 0.82rem;
    }

    .thumb-btn img {
        width: 60px;
        height: 42px;
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

    .vehicle-table__head,
    .vehicle-table__row {
        grid-template-columns: 50px 1fr auto;
        gap: 6px;
    }

    .col-code {
        display: none;
    }

    .thumb-btn img {
        width: 50px;
        height: 36px;
    }

    .pager-bar {
        flex-wrap: wrap;
        gap: 8px;
    }
}
</style>
