<template>
    <div class="page model-catalog">
        <main class="catalog-main constrained">
            <!-- ========== List View ========== -->
            <template v-if="!selectedModelId">
                <header class="catalog-header">
                    <div>
                        <p class="eyebrow">Model</p>
                        <h1>按车型检索运营公司</h1>
                        <p class="subtitle">收录 {{ catalog.length }} 款车型 / {{ totalCompanies }} 家运营公司</p>
                    </div>
                </header>

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
                            <button class="pill-btn" type="button"
                                @click="router.push({ name: 'ModelCatalog', params: { modelId: model.id } })">
                                查看该车型
                            </button>
                        </div>
                        <div class="company-grid">
                            <div v-for="company in model.companies || []" :key="company.id" class="company-pill">
                                <img :src="company.thumbnailUrl || placeholderLogo" :alt="company.name" loading="lazy" decoding="async" />
                                <div class="company-pill__body">
                                    <p class="company-name">{{ company.name }}</p>
                                    <p class="company-region">{{ company.regionName || regionsById[company.regionId] || '地区待补全' }}</p>
                                </div>
                                <div class="company-actions">
                                    <button class="text-btn" type="button"
                                        @click="router.push({ name: 'CompanyCatalog', params: { companyId: company.id } })">
                                        查看公司
                                    </button>
                                </div>
                            </div>
                        </div>
                    </article>
                </section>
            </template>

            <!-- ========== Detail View ========== -->
            <template v-else>
                <header class="detail-header">
                    <a class="back-link" @click.prevent="clearModelFilter">← 返回全部车型</a>
                    <div v-if="isCompanyScoped && scopedCompanyName" class="detail-headline">
                        <p class="detail-eyebrow">公交公司</p>
                        <h1 class="detail-company-name">{{ scopedCompanyName }}</h1>
                        <p class="detail-model-sub">{{ modelDetail?.name || '车型加载中...' }}</p>
                    </div>
                    <div v-else-if="modelDetail" class="detail-headline">
                        <h1 class="detail-model-name">{{ modelDetail.name }}</h1>
                        <p class="detail-model-brand">{{ modelDetail.brand?.chnName || modelDetail.brand?.name || '品牌待补全' }}</p>
                    </div>
                </header>

                <section v-if="vehiclesLoading && !allVehicles.length" class="state state--loading">
                    正在加载车辆数据...
                </section>

                <template v-else>
                    <!-- ===== Config Table ===== -->
                    <section class="fleet-section">
                        <h2 class="section-title">车辆概况</h2>
                        <div v-if="!configTable.years.length" class="state state--empty">暂无配置数据</div>
                        <div v-else class="config-table-wrap">
                            <div class="config-table-scroll">
                                <div class="config-year-header">
                                    <div class="config-year-header__spacer"></div>
                                    <div v-for="year in configTable.years" :key="year" class="config-year-header__cell">
                                        <span class="year-badge">{{ year }}</span>
                                        <span class="year-count-badge">{{ yearVehicleCount[year] || 0 }}</span>
                                    </div>
                                </div>
                                <div v-for="row in configTable.rows" :key="row.label" class="config-row">
                                    <div class="config-row__label">{{ row.label }}</div>
                                    <div v-for="year in configTable.years" :key="year" class="config-row__cell">
                                        {{ formatConfigCell(row.cells[year]) }}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>

                    <!-- ===== Photo Table ===== -->
                    <section class="photo-section">
                        <h2 class="section-title">车辆照片</h2>
                        <div v-if="!photoTable.years.length" class="state state--empty">暂无照片数据</div>
                        <div v-else class="photo-table-wrap">
                            <div class="photo-table-scroll">
                                <div class="photo-year-header">
                                    <div class="photo-year-header__spacer">线路</div>
                                    <div v-for="year in photoTable.years" :key="year" class="photo-year-header__cell">
                                        {{ year }}
                                    </div>
                                </div>
                                <div v-for="route in photoTable.routes" :key="route.key" class="photo-row">
                                    <div class="photo-row__route">
                                        <router-link v-if="route.routeId"
                                            :to="{ name: 'RouteDetail', params: { routeId: route.routeId } }"
                                            class="route-link">
                                            {{ route.routeNumber }}
                                        </router-link>
                                        <span v-else class="route-link route-link--plain">{{ route.routeNumber }}</span>
                                    </div>
                                    <div v-for="year in photoTable.years" :key="year" class="photo-row__cell">
                                        <button
                                            v-for="img in (route.cells[year] || [])"
                                            :key="img.id"
                                            class="photo-thumb"
                                            type="button"
                                            @click="openVehicleDetail(img.vehicleId)"
                                        >
                                            <img :src="img.thumbnailUrl || placeholderLogo" :alt="route.routeNumber" loading="lazy" decoding="async" />
                                        </button>
                                    </div>
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
import { normalizeFuelType } from '@/utils/fuel';

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

const extractYear = (date) => {
    if (!date) return null;
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return null;
    return `${parsed.getFullYear()}`;
};

const extractExifYear = (exif) => {
    if (!exif || typeof exif !== 'object') return null;
    const shotTime = exif['拍摄时间'] || exif['DateTimeOriginal'] || exif['Date/Time Original'] || '';
    if (!shotTime) return null;
    // Format: "2024-05-15 14:30:00" or similar
    const match = String(shotTime).match(/^(\d{4})/);
    return match ? match[1] : null;
};

// Client-side company filter
const displayVehicles = computed(() => {
    if (!isCompanyScoped.value) return allVehicles.value;
    const cid = scopedCompanyId.value;
    return allVehicles.value.filter((r) => {
        const vcid = r?.vehicle?.company?.id || r?.vehicle?.companyId;
        return Number(vcid) === cid;
    });
});

const yearVehicleCount = computed(() => {
    const map = {};
    displayVehicles.value.forEach((r) => {
        const year = extractYear(r?.vehicle?.launchDate) || '年份未知';
        map[year] = (map[year] || 0) + 1;
    });
    return map;
});

// ===== Config Table =====
const CONFIG_FIELDS = [
    { key: 'powertrain', label: '动力系统', get: (v) => v?.vehicleConfig?.engine || v?.vehicleConfig?.motor || '' },
    { key: 'fuelType', label: '燃料', get: (v) => normalizeFuelType(v?.vehicleConfig?.fuelType) || '' },
    { key: 'transmission', label: '变速箱', get: (v) => v?.vehicleConfig?.transmissionSystem || '' },
    { key: 'stepType', label: '踏步', get: (v) => v?.vehicleConfig?.stepType || '' },
    { key: 'suspension', label: '悬挂', get: (v) => v?.vehicleConfig?.suspension || '' },
    { key: 'axle', label: '车桥', get: (v) => v?.vehicleConfig?.axle || '' },
    { key: 'airConditioned', label: '空调', get: (v) => (v?.vehicle?.airConditioned === true ? '有' : v?.vehicle?.airConditioned === false ? '无' : '') }
];

const configTable = computed(() => {
    // Group vehicles by year, collect unique configs per field
    const yearMap = new Map(); // year -> Map<fieldKey, Set<values>>
    displayVehicles.value.forEach((r) => {
        const year = extractYear(r?.vehicle?.launchDate) || '年份未知';
        if (!yearMap.has(year)) {
            yearMap.set(year, new Map());
        }
        const fieldMap = yearMap.get(year);
        CONFIG_FIELDS.forEach((field) => {
            const val = field.get(r);
            if (!val) return;
            if (!fieldMap.has(field.key)) {
                fieldMap.set(field.key, new Set());
            }
            fieldMap.get(field.key).add(val);
        });
    });

    const years = Array.from(yearMap.keys()).sort((a, b) => {
        const na = Number(a), nb = Number(b);
        if (Number.isNaN(na) && Number.isNaN(nb)) return 0;
        if (Number.isNaN(na)) return 1;
        if (Number.isNaN(nb)) return -1;
        return nb - na;
    });

    const rows = CONFIG_FIELDS.map((field) => {
        const cells = {};
        years.forEach((year) => {
            const fieldMap = yearMap.get(year);
            cells[year] = fieldMap ? Array.from(fieldMap.get(field.key) || []) : [];
        });
        // Only include row if at least one cell has data
        const hasData = Object.values(cells).some((arr) => arr.length > 0);
        return { label: field.label, cells, hasData };
    }).filter((row) => row.hasData);

    return { years, rows };
});

const formatConfigCell = (values) => {
    if (!values || !values.length) return '—';
    return values.join(' / ');
};

// ===== Photo Table =====
const photoTable = computed(() => {
    // Collect all images with route + exif year
    const routeMap = new Map(); // key -> { routeNumber, routeId, yearMap: { year: [images] } }
    const yearSet = new Set();

    displayVehicles.value.forEach((record) => {
        const images = Array.isArray(record?.images) ? record.images : [];
        const vehicleId = record?.vehicle?.id;

        images.forEach((img) => {
            if (!img?.thumbnailUrl && !img?.url) return;
            const exifYear = extractExifYear(img?.exif) || '未知年份';
            yearSet.add(exifYear);

            const routeNumber = img?.routeNumber?.trim() || '未归类';
            const routeId = img?.routeId || null;
            const key = routeNumber;

            if (!routeMap.has(key)) {
                routeMap.set(key, { routeNumber, routeId, yearMap: new Map() });
            }
            const entry = routeMap.get(key);
            if (!entry.yearMap.has(exifYear)) {
                entry.yearMap.set(exifYear, []);
            }
            entry.yearMap.get(exifYear).push({
                id: img.id || `${vehicleId}-${Math.random()}`,
                thumbnailUrl: img.thumbnailUrl || img.url,
                vehicleId
            });
        });
    });

    const years = Array.from(yearSet).sort((a, b) => {
        const na = Number(a), nb = Number(b);
        if (Number.isNaN(na) && Number.isNaN(nb)) return 0;
        if (Number.isNaN(na)) return 1;
        if (Number.isNaN(nb)) return -1;
        return nb - na;
    });

    const routes = Array.from(routeMap.values())
        .map((entry) => {
            const cells = {};
            years.forEach((year) => {
                cells[year] = entry.yearMap.get(year) || [];
            });
            return {
                key: entry.routeNumber,
                routeNumber: entry.routeNumber,
                routeId: entry.routeId,
                cells
            };
        })
        // Sort: known routes first, then alphabetically; "未归类" last
        .sort((a, b) => {
            if (a.routeNumber === '未归类') return 1;
            if (b.routeNumber === '未归类') return -1;
            return a.routeNumber.localeCompare(b.routeNumber, 'zh-CN', { numeric: true });
        });

    return { years, routes };
});

// ===== Vehicle Detail Modal =====
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

// ===== Watcher =====
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
            const data = await store.dispatch('models/loadModelVehicles', { modelId, params, force: true });
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
.page { min-height: 100vh; display: flex; flex-direction: column; background: #f1f5f9; }
.constrained { width: min(1200px, 100%); margin: 0 auto; flex: 1; padding: 32px 24px 72px; }
.catalog-header { display: flex; justify-content: space-between; margin-bottom: 24px; align-items: flex-start; gap: 12px; flex-wrap: wrap;
    h1 { margin: 0 0 6px; font-size: 1.6rem; font-weight: 700; color: #111827; } }
.eyebrow { text-transform: uppercase; letter-spacing: 0.3em; font-size: 0.72rem; color: #9ca3af; margin: 0 0 4px; }
.subtitle { margin: 0; font-size: 0.9rem; color: #6b7280; }
.state { border-radius: 16px; padding: 48px; text-align: center; background: #fff; color: #475569;
    &--loading { background: #dbeafe; color: #1d4ed8; }
    &--empty { color: #94a3b8; } }
.pill-btn { border: 1px solid rgba(37,99,235,0.3); border-radius: 999px; height: 30px; padding: 0 12px; display: inline-flex; align-items: center; justify-content: center; white-space: nowrap; background: rgba(37,99,235,0.06); color: #1d4ed8; font-weight: 500; font-size: 0.78rem; cursor: pointer; transition: background 0.2s, border-color 0.2s, color 0.2s;
    &:hover { background: rgba(37,99,235,0.12); border-color: rgba(37,99,235,0.42); color: #1e40af; } }
.filter-bar { background: #fff; border-radius: 18px; padding: 16px 20px; margin-bottom: 24px; box-shadow: 0 8px 24px rgba(15,23,42,0.05); }
.filter-label { margin: 0 0 8px; font-size: 0.9rem; color: #475569; }
.chip-row { display: flex; gap: 8px; overflow-x: auto; }
.filter-chip { border: 1px solid rgba(37,99,235,0.2); border-radius: 999px; padding: 6px 16px; background: transparent; color: #2563eb; cursor: pointer; transition: all 0.2s; white-space: nowrap;
    &.active, &:hover { background: #2563eb; color: #fff; border-color: #2563eb; } }
.section-title { margin: 0 0 14px; font-size: 1.05rem; font-weight: 700; color: #1e293b; }

/* ========== List View ========== */
.model-list { display: flex; flex-direction: column; gap: 24px; }
.model-block { background: #fff; border-radius: 24px; padding: 28px; box-shadow: 0 12px 32px rgba(15,23,42,0.08); }
.model-block__header { display: flex; justify-content: space-between; margin-bottom: 18px; }
.tag { margin-top: 6px; display: inline-flex; padding: 4px 12px; border-radius: 999px; background: rgba(37,99,235,0.1); color: #2563eb; font-size: 0.85rem; }
.company-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.company-pill { border: 1px solid #e2e8f0; border-radius: 18px; padding: 14px 20px; display: flex; gap: 16px; align-items: center; background: #f8fafc; justify-content: space-between; min-width: 0;
    img { width: 80px; height: 60px; object-fit: cover; border-radius: 12px; } }
.company-pill__body { flex: 1 1 0; min-width: 0; }
.company-name { font-weight: 600; overflow-wrap: anywhere; line-height: 1.35; }
.company-region { color: #475569; line-height: 1.35; }
.company-actions { flex: 0 0 auto; display: flex; align-items: center; margin-left: 8px; }
.text-btn, .text-btn:visited { border: none; background: none; color: #2563eb; font-weight: 600; cursor: pointer; margin-left: auto; }

/* ========== Detail View ========== */
.detail-header { display: flex; flex-direction: column; gap: 8px; margin-bottom: 28px; }
.back-link { color: #2563eb; text-decoration: none; font-size: 14px; display: inline-block; cursor: pointer;
    &:hover { text-decoration: underline; } }
.detail-headline { display: flex; flex-direction: column; gap: 2px; }
.detail-eyebrow { margin: 0; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.3em; color: #9ca3af; }
.detail-company-name { margin: 0; font-size: 1.5rem; font-weight: 700; color: #111827; }
.detail-model-sub { margin: 0; font-size: 1rem; color: #6b7280; }
.detail-model-name { margin: 0; font-size: 1.6rem; font-weight: 700; color: #111827; }
.detail-model-brand { margin: 0; font-size: 0.95rem; color: #6b7280; }

/* ===== Fleet Section ===== */
.fleet-section { margin-bottom: 32px; }
.photo-section { margin-bottom: 32px; }

/* ===== Config Table ===== */
.config-table-wrap { background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 1px 8px rgba(15,23,42,0.06); }
.config-table-scroll { overflow-x: auto; }

.config-year-header {
    display: flex; align-items: stretch; min-width: fit-content;
    border-bottom: 1px solid #e2e8f0; background: #f8fafc;
}
.config-year-header__spacer {
    min-width: 90px; max-width: 90px; flex-shrink: 0;
    padding: 12px 14px; font-size: 0.78rem; color: #94a3b8; font-weight: 600;
    display: flex; align-items: center;
    position: sticky; left: 0; background: #f8fafc; z-index: 2;
}
.config-year-header__cell {
    min-width: 180px; flex: 1;
    padding: 10px 14px;
    display: flex; align-items: center; gap: 8px;
}
.year-badge {
    display: inline-flex; padding: 4px 12px; border-radius: 8px;
    background: #00695c; color: #fff; font-size: 0.88rem; font-weight: 600;
}
.year-count-badge {
    display: inline-flex; padding: 2px 8px; border-radius: 6px;
    background: rgba(0,105,92,0.1); color: #00695c; font-size: 0.78rem; font-weight: 600;
}

.config-row {
    display: flex; align-items: stretch; min-width: fit-content;
    border-bottom: 1px solid #f1f5f9;
    &:last-child { border-bottom: none; }
}
.config-row__label {
    min-width: 90px; max-width: 90px; flex-shrink: 0;
    padding: 10px 14px; font-size: 0.82rem; color: #64748b; font-weight: 500;
    display: flex; align-items: center;
    position: sticky; left: 0; background: #fff; z-index: 1;
    border-right: 1px solid #f1f5f9;
}
.config-row__cell {
    min-width: 180px; flex: 1;
    padding: 10px 14px; font-size: 0.85rem; color: #1e293b;
    display: flex; align-items: center;
}

/* ===== Photo Table ===== */
.photo-table-wrap { background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 1px 8px rgba(15,23,42,0.06); }
.photo-table-scroll { overflow-x: auto; }

.photo-year-header {
    display: flex; align-items: stretch; min-width: fit-content;
    border-bottom: 1px solid #e2e8f0; background: #f8fafc;
}
.photo-year-header__spacer {
    min-width: 80px; max-width: 80px; flex-shrink: 0;
    padding: 12px 14px; font-size: 0.78rem; color: #94a3b8; font-weight: 600;
    display: flex; align-items: center;
    position: sticky; left: 0; background: #f8fafc; z-index: 2;
}
.photo-year-header__cell {
    min-width: 140px; flex: 1;
    padding: 10px 14px;
    font-size: 0.85rem; font-weight: 600; color: #475569;
    display: flex; align-items: center;
}

.photo-row {
    display: flex; align-items: stretch; min-width: fit-content;
    border-bottom: 1px solid #f1f5f9;
    &:last-child { border-bottom: none; }
}
.photo-row__route {
    min-width: 80px; max-width: 80px; flex-shrink: 0;
    padding: 10px 14px;
    display: flex; align-items: flex-start;
    position: sticky; left: 0; background: #fff; z-index: 1;
    border-right: 1px solid #f1f5f9;
}
.route-link { color: #2563eb; text-decoration: none; font-weight: 600; font-size: 0.82rem;
    &:hover { text-decoration: underline; }
    &:visited { color: #2563eb; }
    &--plain { color: #94a3b8; cursor: default; }
}
.photo-row__cell {
    min-width: 140px; flex: 1;
    padding: 6px;
    display: flex; flex-wrap: wrap; gap: 4px;
    align-items: flex-start;
}
.photo-thumb {
    border: none; padding: 0; border-radius: 6px; overflow: hidden;
    cursor: pointer; background: #e2e8f0; transition: transform 0.15s; flex-shrink: 0;
    img { width: 100px; height: 70px; object-fit: cover; display: block; }
    &:hover { transform: scale(1.05); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
}

/* ========== Responsive ========== */
@media (max-width: 900px) {
    .company-grid { grid-template-columns: 1fr; }
    .model-block { padding: 18px; }
    .model-block__header { flex-wrap: wrap; gap: 10px; }
    .company-pill { padding: 12px; gap: 10px; align-items: flex-start; }
    .company-pill img { width: 70px; height: 52px; flex: 0 0 auto; }
    .company-pill__body { min-width: 0; }
    .company-actions { margin-left: 0; align-self: flex-start; }
    .text-btn, .text-btn:visited { margin-left: 0; white-space: nowrap; }
    .config-year-header__cell { min-width: 140px; }
    .config-row__cell { min-width: 140px; }
    .photo-year-header__cell { min-width: 100px; }
    .photo-row__cell { min-width: 100px; }
    .photo-thumb img { width: 70px; height: 50px; }
}

@media (max-width: 560px) {
    .company-pill { display: grid; grid-template-columns: 1fr auto; gap: 8px 10px; }
    .company-pill img { display: none; }
    .company-pill__body { grid-column: 1 / 2; }
    .company-actions { grid-column: 2 / 3; grid-row: 1 / 2; justify-self: end; }
    .config-year-header__spacer { min-width: 70px; max-width: 70px; }
    .config-row__label { min-width: 70px; max-width: 70px; }
    .config-year-header__cell { min-width: 120px; }
    .config-row__cell { min-width: 120px; font-size: 0.8rem; }
    .photo-year-header__spacer { min-width: 60px; max-width: 60px; }
    .photo-row__route { min-width: 60px; max-width: 60px; }
    .photo-thumb img { width: 56px; height: 40px; }
}
</style>
