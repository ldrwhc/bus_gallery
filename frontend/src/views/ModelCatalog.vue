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

                <section v-else class="model-group-grid">
                    <article v-for="model in filteredModels" :key="model.id" class="model-group">
                        <div class="model-group__head"
                            @click="router.push({ name: 'ModelCatalog', params: { modelId: model.id } })">
                            <div class="model-group__info">
                                <h2 class="model-group__name">{{ model.name }}</h2>
                                <p class="model-group__brand">{{ brandDisplayMap[model.brandName] || model.brandName || '品牌待补全' }}</p>
                            </div>
                            <span class="model-group__arrow">→</span>
                        </div>
                        <div v-if="model.companies?.length" class="company-dual-grid">
                            <div v-for="company in model.companies" :key="company.id" class="company-dual-card"
                                @click="router.push({ name: 'CompanyCatalog', params: { companyId: company.id } })">
                                <img :src="company.thumbnailUrl || placeholderLogo" :alt="company.name" loading="lazy" decoding="async" />
                                <div class="company-dual-card__body">
                                    <p class="company-dual-card__name">{{ company.name }}</p>
                                    <p class="company-dual-card__region">{{ company.regionName || regionsById[company.regionId] || '地区待补全' }}</p>
                                </div>
                            </div>
                        </div>
                        <p v-else class="model-group__empty">暂无运营公司</p>
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

                <!-- ===== Scoped: single-company fleet page ===== -->
                <template v-if="isCompanyScoped">
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
                                        <button class="year-menu-btn" type="button" title="查看该年份车辆列表"
                                            @click.stop="openVehicleList(year)">⋮</button>
                                    </div>
                                </div>
                                <div v-for="row in configTable.rows" :key="row.label" class="config-row">
                                    <div class="config-row__label">{{ row.label }}</div>
                                    <div v-for="year in configTable.years" :key="year" class="config-row__cell">
                                        <template v-if="!row.cells[year].length">—</template>
                                        <span v-for="item in row.cells[year]" :key="item.value" class="config-chip">
                                            {{ item.value }}<strong>{{ item.count }}</strong>
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
                    <section class="photo-section">
                        <h2 class="section-title">车辆照片</h2>
                        <div v-if="!photoTable.years.length" class="state state--empty">暂无照片数据</div>
                        <div v-else class="photo-table-wrap">
                            <div class="photo-table-scroll">
                                <div class="photo-year-header">
                                    <div class="photo-year-header__spacer">线路</div>
                                    <div v-for="year in photoTable.years" :key="year" class="photo-year-header__cell">{{ year }}</div>
                                </div>
                                <div v-for="route in photoTable.routes" :key="route.key" class="photo-row">
                                    <div class="photo-row__route">
                                        <router-link v-if="route.routeId" :to="{ name: 'RouteDetail', params: { routeId: route.routeId } }" class="route-link">{{ route.routeNumber }}</router-link>
                                        <span v-else class="route-link route-link--plain">{{ route.routeNumber }}</span>
                                    </div>
                                    <div v-for="year in photoTable.years" :key="year" class="photo-row__cell">
                                        <button v-for="img in (route.cells[year] || [])" :key="img.id" class="photo-thumb" type="button" @click="openVehicleDetail(img.vehicleId)">
                                            <img :src="img.thumbnailUrl || placeholderLogo" :alt="route.routeNumber" loading="lazy" decoding="async" />
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
                </template>

                <!-- ===== Non-scoped: City-grouped company cards ===== -->
                <template v-else>
                    <div v-if="!cityGroupedCards.length" class="state state--empty">暂无运营公司</div>
                    <section v-for="city in cityGroupedCards" :key="city.regionName" class="city-section">
                        <h3 class="city-section__title">{{ city.regionName }}</h3>
                        <div class="company-card-grid">
                            <article v-for="card in city.cards" :key="card.companyId" class="company-card"
                                @click="router.push({ name: 'ModelCatalog', params: { modelId: selectedModelId }, query: { companyId: card.companyId } })">
                                <div class="company-card__image">
                                    <img :src="card.previewImage || placeholderLogo" :alt="card.companyName" loading="lazy" decoding="async" />
                                    <span class="company-card__badge">{{ card.vehicleCount }}辆</span>
                                </div>
                                <div class="company-card__body">
                                    <div class="company-card__head">
                                        <img v-if="card.thumbnailUrl" :src="card.thumbnailUrl" class="company-card__logo" />
                                        <span class="company-card__name">{{ card.companyName }}</span>
                                    </div>
                                    <div class="company-card__years">
                                        <span v-for="yi in card.yearItems" :key="yi.year" class="year-chip-sm">
                                            {{ yi.year }} <strong>{{ yi.count }}</strong>
                                        </span>
                                    </div>
                                    <div v-for="row in card.configRows" :key="row.label" class="info-line">
                                        <span class="info-label">{{ row.label }}</span>
                                        <span class="info-values">
                                            <span v-for="item in row.items" :key="item.value" class="info-chip-sm">
                                                {{ item.value }}
                                                <strong>{{ item.count }}</strong>
                                            </span>
                                        </span>
                                    </div>
                                </div>
                            </article>
                        </div>
                    </section>
                </template>
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
                            <button class="pager-btn" type="button"
                                :disabled="vehicleListLoading || vehicleListPage <= 1"
                                @click="loadVehicleListPage(vehicleListPage - 1)">上一页</button>
                            <span class="pager-text">第 {{ vehicleListPage }} 页 · 每页 {{ vehicleListPageSize }} 条 · 共 {{ vehicleListTotal }} 条</span>
                            <button class="pager-btn" type="button"
                                :disabled="vehicleListLoading || !vehicleListHasNext"
                                @click="loadVehicleListPage(vehicleListPage + 1)">下一页</button>
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
                                        <td><span class="year-tag">{{ row.year }}</span></td>
                                        <td><span class="plate">{{ row.plate }}</span></td>
                                        <td>
                                            <div class="multi-value">
                                                <span v-for="record in row.records" :key="`${row.key}-code-${record.id}`" class="code">{{ record.customNumber }}</span>
                                            </div>
                                        </td>
                                        <td>
                                            <div class="thumb-list">
                                                <button v-for="record in row.records" :key="`${row.key}-thumb-${record.id}`"
                                                    class="vehicle-overlay__thumb" type="button"
                                                    @click="openVehicleDetail(record.vehicle?.id)">
                                                    <img :src="resolveListImage(record.images) || placeholderLogo"
                                                        :alt="record.vehicle?.plateNumber || '缩略图'" loading="lazy" decoding="async" />
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
    if (!isCompanyScoped.value || !displayVehicles.value.length) return '';
    const first = displayVehicles.value[0];
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
    // Group vehicles by year, collect per-config counts
    const yearMap = new Map(); // year -> Map<fieldKey, Map<value, count>>
    displayVehicles.value.forEach((r) => {
        const year = extractYear(r?.vehicle?.launchDate) || '年份未知';
        if (!yearMap.has(year)) yearMap.set(year, new Map());
        const fieldMap = yearMap.get(year);
        CONFIG_FIELDS.forEach((field) => {
            const val = field.get(r);
            if (!val) return;
            if (!fieldMap.has(field.key)) fieldMap.set(field.key, new Map());
            const countMap = fieldMap.get(field.key);
            countMap.set(val, (countMap.get(val) || 0) + 1);
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
            const countMap = fieldMap?.get(field.key);
            cells[year] = countMap
                ? Array.from(countMap.entries()).map(([value, count]) => ({ value, count }))
                : [];
        });
        const hasData = Object.values(cells).some((arr) => arr.length > 0);
        return { label: field.label, cells, hasData };
    }).filter((row) => row.hasData);

    return { years, rows };
});

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

            // Route: image.routeNumber > image.routeId match > vehicle.routes fallback
            let routeNumber = img?.routeNumber?.trim() || '';
            const routeId = img?.routeId || null;
            if (!routeNumber && routeId && record?.vehicle?.routes) {
                const match = record.vehicle.routes.find((r) => Number(r.routeId) === Number(routeId));
                if (match?.routeNumber) routeNumber = match.routeNumber.trim();
            }
            if (!routeNumber) routeNumber = '未归类';
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

// ===== Non-scoped: City-grouped company cards =====
const cityGroupedCards = computed(() => {
    if (!displayVehicles.value.length) return [];

    // Group by region then company
    const regionMap = new Map();
    displayVehicles.value.forEach((r) => {
        const vehicle = r?.vehicle;
        if (!vehicle) return;
        const company = vehicle?.company;
        const region = company?.region || vehicle?.region;
        const regionName = region?.name || company?.regionName || vehicle?.regionName || '地区未知';
        const cid = company?.id || vehicle?.companyId;
        if (!cid) return;

        if (!regionMap.has(regionName)) {
            regionMap.set(regionName, new Map());
        }
        const companyMap = regionMap.get(regionName);
        const key = String(cid);
        if (!companyMap.has(key)) {
            companyMap.set(key, {
                companyId: cid,
                companyName: company?.name || vehicle?.companyName || '未归类',
                thumbnailUrl: company?.thumbnailUrl || null,
                regionName,
                vehicleCount: 0,
                vehicles: []
            });
        }
        const entry = companyMap.get(key);
        entry.vehicleCount++;
        entry.vehicles.push(r);
        if (!entry.thumbnailUrl) {
            entry.thumbnailUrl = company?.thumbnailUrl || null;
        }
    });

    const allCards = [];
    for (const [regionName, companyMap] of regionMap) {
        const cards = Array.from(companyMap.values())
            .sort((a, b) => b.vehicleCount - a.vehicleCount)
            .map((entry) => {
            const vehicles = entry.vehicles;

            // Preview image from first vehicle with an image
            let previewImage = null;
            for (const r of vehicles) {
                const imgs = Array.isArray(r?.images) ? r.images : [];
                if (imgs.length && (imgs[0]?.thumbnailUrl || imgs[0]?.url)) {
                    previewImage = imgs[0].thumbnailUrl || imgs[0].url;
                    break;
                }
            }

            // Production years with counts
            const yearCountMap = new Map();
            vehicles.forEach((r) => {
                const y = extractYear(r?.vehicle?.launchDate) || '年份未知';
                yearCountMap.set(y, (yearCountMap.get(y) || 0) + 1);
            });
            const yearItems = Array.from(yearCountMap.entries())
                .map(([year, count]) => ({ year, count }))
                .sort((a, b) => {
                    const na = Number(a.year), nb = Number(b.year);
                    if (Number.isNaN(na)) return 1;
                    if (Number.isNaN(nb)) return -1;
                    return nb - na;
                });

            // Config rows: per spec, show value + year/count breakdown
            const configRows = [];
            CONFIG_FIELDS.forEach((field) => {
                // value -> Map<year, count>
                const valueMap = new Map();
                vehicles.forEach((r) => {
                    const val = field.get(r);
                    if (!val) return;
                    const year = extractYear(r?.vehicle?.launchDate) || '年份未知';
                    if (!valueMap.has(val)) valueMap.set(val, new Map());
                    const ymap = valueMap.get(val);
                    ymap.set(year, (ymap.get(year) || 0) + 1);
                });
                if (!valueMap.size) return;
                const items = Array.from(valueMap.entries()).map(([value, ymap]) => {
                    const years = Array.from(ymap.entries())
                        .map(([year, count]) => ({ year, count }))
                        .sort((a, b) => {
                            const na = Number(a.year), nb = Number(b.year);
                            if (Number.isNaN(na)) return 1;
                            if (Number.isNaN(nb)) return -1;
                            return nb - na;
                        });
                    const totalCount = years.reduce((s, y) => s + y.count, 0);
                    // Only show year breakdown if there are multiple years or it differs from value count
                    const showYears = years.length > 1 || (years.length === 1 && years[0].count !== totalCount);
                    return {
                        value,
                        count: totalCount,
                        years: showYears ? years : []
                    };
                });
                configRows.push({ label: field.label, items });
            });

            return {
                companyId: entry.companyId,
                companyName: entry.companyName,
                thumbnailUrl: entry.thumbnailUrl,
                vehicleCount: entry.vehicleCount,
                previewImage,
                yearItems,
                configRows
            };
        });
        allCards.push({ regionName, cards });
    }
    return allCards.sort((a, b) => a.regionName.localeCompare(b.regionName, 'zh-CN'));
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

// ===== Vehicle List Overlay =====
const vehicleListVisible = ref(false);
const vehicleListTitle = ref('');
const vehicleListItems = ref([]);
const vehicleListPage = ref(1);
const vehicleListTotal = ref(0);
const vehicleListHasNext = ref(false);
const vehicleListLoading = ref(false);
const vehicleListPageSize = 30;
const vehicleListYear = ref('');

const groupedVehicleRows = computed(() => {
    if (!vehicleListItems.value.length) return [];
    const map = new Map();
    vehicleListItems.value.forEach((record, index) => {
        const plate = record?.vehicle?.plateNumber?.trim() || '未上牌';
        if (!map.has(plate)) map.set(plate, []);
        map.get(plate).push({ record, index });
    });
    return Array.from(map.entries()).map(([plate, entries], groupIndex) => {
        const years = [...new Set(entries.map(({ record }) => extractYear(record?.vehicle?.launchDate) || '年份未知'))].join(', ');
        return {
            key: `${plate}-${groupIndex}`,
            plate,
            year: years,
            records: entries.map(({ record }) => ({
                id: record?.vehicle?.id || `${plate}-${Math.random()}`,
                customNumber: record?.vehicle?.customNumber || '无自编号',
                vehicle: record?.vehicle,
                images: record?.images || []
            }))
        };
    });
});

const resolveListImage = (images = []) => {
    const first = images?.[0];
    if (!first) return null;
    return first.thumbnailUrl || null;
};

const openVehicleList = (year) => {
    vehicleListYear.value = year;
    vehicleListTitle.value = `${modelDetail.value?.name || '车型'} · ${year} 年`;
    vehicleListPage.value = 1;
    vehicleListTotal.value = 0;
    vehicleListHasNext.value = false;
    vehicleListItems.value = [];
    vehicleListVisible.value = true;
    loadVehicleListPage(1).catch((error) => console.error(error));
};

const closeVehicleList = () => {
    vehicleListVisible.value = false;
    vehicleListItems.value = [];
    vehicleListYear.value = '';
};

const loadVehicleListPage = async (targetPage) => {
    const page = Math.max(1, Number(targetPage) || 1);
    vehicleListLoading.value = true;
    try {
        let filtered = displayVehicles.value.filter((record) => {
            const vehicle = record?.vehicle;
            if (!vehicle) return false;
            const year = extractYear(vehicle?.launchDate);
            return year === vehicleListYear.value;
        });

        // Sort by customNumber ascending
        filtered.sort((a, b) => {
            const ca = a?.vehicle?.customNumber || '';
            const cb = b?.vehicle?.customNumber || '';
            return ca.localeCompare(cb, 'zh-CN', { numeric: true });
        });

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

/* ========== List View: 2-col Model Groups + Dual Company Grid ========== */
.model-group-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.model-group { border: 1px solid #e2e8f0; border-radius: 14px; padding: 18px; background: #fff; }
.model-group__head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; cursor: pointer; }
.model-group__info { flex: 1; min-width: 0; }
.model-group__name { margin: 0; font-size: 0.95rem; font-weight: 700; color: #111827; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.model-group__brand { margin: 2px 0 0; font-size: 0.75rem; color: #94a3b8; }
.model-group__arrow { font-size: 0.9rem; color: #94a3b8; flex-shrink: 0; }
.model-group__empty { color: #94a3b8; font-size: 0.8rem; text-align: center; padding: 8px; }

.company-dual-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.company-dual-card { display: flex; gap: 8px; align-items: center; padding: 8px 10px; border-radius: 10px; border: 1px solid #e2e8f0; cursor: pointer; transition: border-color 0.15s, background 0.15s; min-width: 0;
    &:hover { border-color: #2563eb; background: #fafbff; }
    img { width: 48px; height: 36px; object-fit: cover; border-radius: 6px; flex-shrink: 0; background: #e2e8f0; } }
.company-dual-card__body { flex: 1; min-width: 0; }
.company-dual-card__name { margin: 0; font-size: 0.82rem; font-weight: 600; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.company-dual-card__region { margin: 1px 0 0; font-size: 0.72rem; color: #94a3b8; }

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
    padding: 8px 10px; font-size: 0.85rem; color: #1e293b;
    display: flex; flex-wrap: wrap; align-items: flex-start; gap: 6px;
}
.config-chip {
    display: inline-flex; align-items: center; gap: 4px;
    padding: 3px 10px; border-radius: 999px;
    background: #f1f5f9; color: #334155; font-size: 0.82rem;
    white-space: nowrap;
    strong {
        background: #fff; padding: 1px 5px; border-radius: 6px;
        font-size: 0.72rem; color: #0f172a; min-width: 14px; text-align: center;
    }
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

/* ===== City-grouped Company Cards (non-scoped) ===== */
.city-section { margin-bottom: 28px; }
.city-section__title { margin: 0 0 12px; font-size: 1rem; font-weight: 700; color: #1e293b; }
.company-card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 14px;
}
.company-card {
    background: #fff; border-radius: 12px; overflow: hidden;
    box-shadow: 0 1px 4px rgba(15,23,42,0.06);
    cursor: pointer; transition: box-shadow 0.15s, transform 0.15s;
    &:hover { box-shadow: 0 4px 16px rgba(15,23,42,0.1); transform: translateY(-2px); }
}
.company-card__image {
    position: relative; width: 100%; aspect-ratio: 3/2; overflow: hidden; background: #e2e8f0;
    img { width: 100%; height: 100%; object-fit: cover; display: block; }
}
.company-card__badge {
    position: absolute; bottom: 8px; right: 8px;
    padding: 2px 8px; border-radius: 6px;
    background: rgba(0,0,0,0.55); color: #fff; font-size: 0.72rem; font-weight: 600;
}
.company-card__body { padding: 10px 14px 14px; }
.company-card__head { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.company-card__logo { width: 20px; height: 20px; border-radius: 4px; object-fit: cover; flex-shrink: 0; }
.company-card__name { font-size: 0.88rem; font-weight: 700; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.company-card__years { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px; }
.year-chip-sm {
    display: inline-flex; align-items: center; gap: 2px;
    padding: 1px 8px; border-radius: 6px;
    background: #00695c; color: #fff; font-size: 0.72rem; font-weight: 500;
    strong { font-weight: 700; opacity: 0.85; }
}
.info-line { display: flex; gap: 6px; margin-bottom: 3px; font-size: 0.75rem; line-height: 1.5; align-items: baseline; }
.info-label { color: #94a3b8; min-width: 36px; flex-shrink: 0; }
.info-values { display: flex; flex-wrap: wrap; gap: 3px; }
.info-chip-sm {
    display: inline-flex; align-items: center; gap: 2px;
    padding: 1px 6px; border-radius: 999px;
    background: #f1f5f9; color: #475569; font-size: 0.72rem; white-space: nowrap;
    strong {
        background: #fff; padding: 0 5px; border-radius: 4px;
        font-size: 0.68rem; color: #0f172a; font-weight: 600;
    }
}

/* ===== Year Menu Button ===== */
.year-menu-btn {
    border: none; background: rgba(0,0,0,0.08); color: #475569;
    width: 26px; height: 26px; border-radius: 50%;
    cursor: pointer; font-size: 1.1rem; line-height: 1;
    flex-shrink: 0; margin-left: 8px;
    transition: background 0.15s, color 0.15s;
    &:hover { background: #2563eb; color: #fff; }
}

/* ===== Vehicle List Overlay ===== */
.vehicle-overlay {
    position: fixed; inset: 0; background: rgba(15,23,42,0.65);
    backdrop-filter: blur(6px); display: flex; align-items: center;
    justify-content: center; padding: 24px; z-index: 120;
}
.vehicle-overlay__panel {
    width: min(900px, 100%); max-height: 90vh; background: #fff;
    border-radius: 28px; padding: 24px; display: flex; flex-direction: column;
    gap: 16px; box-shadow: 0 30px 60px rgba(15,23,42,0.3); overflow: hidden;
}
.vehicle-overlay__header { display: flex; justify-content: space-between; align-items: flex-start; }
.vehicle-overlay__eyebrow { text-transform: uppercase; letter-spacing: 0.2em; font-size: 0.75rem; color: #94a3b8; margin: 0 0 4px; }
.vehicle-overlay__pager { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 12px; border-radius: 12px; background: #f8fafc; }
.vehicle-overlay__content { flex: 1; overflow-y: auto; padding-right: 8px; }
.vehicle-overlay__table { width: 100%; border-collapse: collapse; font-size: 0.95rem;
    th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #e2e8f0; color: #475569; }
    th { font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.05em; color: #94a3b8; }
}
.vehicle-overlay__thumb { border: none; padding: 0; border-radius: 10px; overflow: hidden; cursor: pointer; background: #0f172a;
    img { width: 90px; height: 60px; object-fit: cover; display: block; transition: transform 0.2s; }
    &:hover img { transform: scale(1.05); }
}
.plate { font-weight: 600; color: #1f2937; }
.year-tag { background: #eef2ff; color: #1d4ed8; padding: 2px 8px; border-radius: 8px; font-size: 0.85rem; font-weight: 600; white-space: nowrap; }
.code { color: #94a3b8; }
.multi-value { display: flex; flex-wrap: wrap; gap: 8px; }
.thumb-list { display: flex; flex-wrap: wrap; gap: 8px; }
.close-btn { border: none; background: #f1f5f9; width: 36px; height: 36px; border-radius: 50%; font-size: 1.3rem; cursor: pointer; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.18s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ========== Responsive ========== */
@media (max-width: 900px) {
    .company-card-grid { grid-template-columns: 1fr; gap: 14px; }
    .config-year-header__cell { min-width: 140px; }
    .config-row__cell { min-width: 140px; }
    .photo-year-header__cell { min-width: 100px; }
    .photo-row__cell { min-width: 100px; }
    .photo-thumb img { width: 70px; height: 50px; }
}

@media (max-width: 560px) {
    .company-card-grid { grid-template-columns: 1fr; gap: 10px; }
    .company-card__image img { height: 160px; }
    .config-year-header__spacer { min-width: 70px; max-width: 70px; }
    .config-row__label { min-width: 70px; max-width: 70px; }
    .config-year-header__cell { min-width: 120px; }
    .config-row__cell { min-width: 120px; font-size: 0.8rem; }
    .photo-year-header__spacer { min-width: 60px; max-width: 60px; }
    .photo-row__route { min-width: 60px; max-width: 60px; }
    .photo-thumb img { width: 56px; height: 40px; }
}
</style>
