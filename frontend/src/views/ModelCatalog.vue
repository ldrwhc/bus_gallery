<template>
    <div class="page model-catalog">
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Model</p>
                    <h1>按车型检索运营公司</h1>
                    <p class="subtitle">收录 {{ catalog.length }} 款车型 / {{ totalCompanies }} 家运营公司</p>
                </div>

                <button v-if="selectedModelId" class="ghost-btn" type="button" @click="clearModelFilter">
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
                                <p class="tag">{{ model.brandName || brandNameMap[model.id] || '品牌待补全' }}</p>
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

            <template v-else>
            <section class="model-detail" v-if="modelDetail">
                <div class="detail-summary">
                    <p class="eyebrow">Model Detail</p>
                    <h1>{{ modelDetail.name }}</h1>
                    <p class="subtitle">{{ modelDetail.brand?.name || brandNameMap[selectedModelId] || '品牌待补全' }}</p>
                </div>

                <div class="filter-grid">
                    <div v-if="filterOptions.year.length" class="filter-line">
                        <span class="filter-label">生产年份</span>
                        <div class="chip-row">
                            <button
                                v-for="year in filterOptions.year"
                                :key="year"
                                :class="['filter-chip', { active: filterSelection.year === year }]"
                                @click="filterSelection.year = filterSelection.year === year ? '' : year"
                                type="button"
                            >
                                {{ year }}
                            </button>
                        </div>
                    </div>
                    <div v-if="filterOptions.power.length" class="filter-line">
                        <span class="filter-label">动力系统</span>
                        <div class="chip-row">
                            <button
                                v-for="power in filterOptions.power"
                                :key="power"
                                :class="['filter-chip', { active: filterSelection.power === power }]"
                                @click="filterSelection.power = filterSelection.power === power ? '' : power"
                                type="button"
                            >
                                {{ power }}
                            </button>
                        </div>
                    </div>
                    <div v-if="filterOptions.fuel.length" class="filter-line">
                        <span class="filter-label">燃料</span>
                        <div class="chip-row">
                            <button
                                v-for="fuel in filterOptions.fuel"
                                :key="fuel"
                                :class="['filter-chip', { active: filterSelection.fuel === fuel }]"
                                @click="filterSelection.fuel = filterSelection.fuel === fuel ? '' : fuel"
                                type="button"
                            >
                                {{ fuel }}
                            </button>
                        </div>
                    </div>
                </div>

                <section v-if="detailLoading || vehiclesLoading" class="state state--loading">
                    正在加载运营公司...
                </section>

                    <section v-else-if="!companyGroups.length" class="state state--empty">
                        暂无车辆数据
                    </section>

                    <section v-else class="company-detail-grid">
                        <article
                            v-for="group in filteredCompanyGroups"
                            :key="group.companyId || group.companyName"
                            class="detail-card"
                        >
                            <div class="detail-card__title-row">
                                <router-link class="detail-card__title" :to="{ name: 'CompanyCatalog', params: { companyId: group.companyId } }">
                                    {{ group.companyName }}
                                </router-link>
                                <router-link
                                    class="detail-card__icon"
                                    :to="{ name: 'CompanyCatalog', params: { companyId: group.companyId } }"
                                    aria-label="查看公司"
                                >
                                    🚌
                                </router-link>
                            </div>
                            <p class="detail-card__region">{{ group.regionName }}</p>
                            <div class="detail-card__image">
                                <img :src="group.coverImage || placeholderLogo" :alt="group.companyName" loading="lazy" decoding="async" />
                            </div>
                            <ul class="info-list">
                                <li>
                                    <span>生产年份</span>
                                    <div class="badge-row">
                                        <span
                                            v-for="badge in group.info.badges.year"
                                            :key="badge.label"
                                            class="count-badge"
                                        >
                                            {{ badge.label }}
                                            <strong>{{ badge.count }}</strong>
                                        </span>
                                    </div>
                                </li>
                                <li>
                                    <span>动力系统</span>
                                    <div class="badge-row">
                                        <span
                                            v-for="badge in group.info.badges.power"
                                            :key="badge.label"
                                            class="count-badge"
                                        >
                                            {{ badge.label }}
                                            <strong>{{ badge.count }}</strong>
                                        </span>
                                    </div>
                                </li>
                                <li>
                                    <span>燃料</span>
                                    <div class="badge-row">
                                        <span
                                            v-for="badge in group.info.badges.fuel"
                                            :key="badge.label"
                                            class="count-badge"
                                        >
                                            {{ badge.label }}
                                            <strong>{{ badge.count }}</strong>
                                        </span>
                                    </div>
                                </li>
                                <li>
                                    <span>变速系统</span>
                                    <div class="badge-row">
                                        <span
                                            v-for="badge in group.info.badges.transmission"
                                            :key="badge.label"
                                            class="count-badge"
                                        >
                                            {{ badge.label }}
                                            <strong>{{ badge.count }}</strong>
                                        </span>
                                    </div>
                                </li>
                                <li>
                                    <span>踏步</span>
                                    <div class="badge-row">
                                        <span
                                            v-for="badge in group.info.badges.step"
                                            :key="badge.label"
                                            class="count-badge"
                                        >
                                            {{ badge.label }}
                                            <strong>{{ badge.count }}</strong>
                                        </span>
                                    </div>
                                </li>
                                <li>
                                    <span>悬挂</span>
                                    <div class="badge-row">
                                        <span
                                            v-for="badge in group.info.badges.suspension"
                                            :key="badge.label"
                                            class="count-badge"
                                        >
                                            {{ badge.label }}
                                            <strong>{{ badge.count }}</strong>
                                        </span>
                                    </div>
                                </li>
                                <li>
                                    <span>空调</span>
                                    <div class="badge-row">
                                        <span
                                            v-for="badge in group.info.badges.air"
                                            :key="badge.label"
                                            class="count-badge"
                                        >
                                            {{ badge.label }}
                                            <strong>{{ badge.count }}</strong>
                                        </span>
                                    </div>
                                </li>
                            </ul>
                        </article>
                    </section>
                </section>
            </template>
        </main>
    </div>
</template>

<script setup>
import { computed, ref, reactive, watch, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import placeholderBus from '@/assets/images/placeholder-bus.png';
import { formatFuelType, formatBoolean } from '@/utils/formatters';

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

const modelVehicles = computed(() =>
    selectedModelId.value ? store.state.models.vehiclesByModel[selectedModelId.value] || [] : []
);

const detailLoading = computed(() =>
    selectedModelId.value ? store.state.models.detailLoadingMap[selectedModelId.value] : false
);

const vehiclesLoading = computed(() =>
    selectedModelId.value ? store.state.models.vehiclesLoadingMap[selectedModelId.value] : false
);

const placeholderLogo = placeholderBus;

const formatYear = (date) => {
    if (!date) return '—';
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return '—';
    return `${parsed.getFullYear()}`;
};

const resolveImage = (images = []) => {
    const image = images?.[0];
    if (!image) return null;
    return image.thumbnailUrl || image.url || null;
};

const brandNameMap = computed(() => {
    const map = {};
    const models = catalog.value || [];
    models.forEach((model) => {
        map[model.id] = model.brandName;
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

const formatFuel = (value) => formatFuelType(value);

const formatAirConditioned = (value) => formatBoolean(value);

const aggregateInfo = (details) => {
    const makeMap = () => Object.create(null);
    const yearMap = makeMap();
    const powerMap = makeMap();
    const fuelMap = makeMap();
    const transmissionMap = makeMap();
    const stepMap = makeMap();
    const suspensionMap = makeMap();
    let airTrue = 0;
    let airFalse = 0;

    details.forEach((detail) => {
        const config = detail.vehicleConfig || {};
        const vehicle = detail.vehicle || {};
        const year = formatYear(vehicle.factoryDate || vehicle.launchDate);
        yearMap[year] = (yearMap[year] || 0) + 1;

        const power = config.engine || config.motor || '—';
        powerMap[power] = (powerMap[power] || 0) + 1;

        const fuel = formatFuel(config.fuelType);
        fuelMap[fuel] = (fuelMap[fuel] || 0) + 1;

        const transmission = config.transmissionSystem || '—';
        transmissionMap[transmission] = (transmissionMap[transmission] || 0) + 1;

        const step = config.stepType || '—';
        stepMap[step] = (stepMap[step] || 0) + 1;

        const suspension = config.suspension || '—';
        suspensionMap[suspension] = (suspensionMap[suspension] || 0) + 1;

        if (vehicle.airConditioned === false) airFalse += 1;
        else airTrue += 1;
    });

    const toBadges = (map) =>
        Object.entries(map).map(([label, count]) => ({
            label,
            count
        }));

    const airBadges = [];
    if (airTrue) airBadges.push({ label: '有空调', count: airTrue });
    if (airFalse) airBadges.push({ label: '无空调', count: airFalse });

    return {
        badges: {
            year: toBadges(yearMap),
            power: toBadges(powerMap),
            fuel: toBadges(fuelMap),
            transmission: toBadges(transmissionMap),
            step: toBadges(stepMap),
            suspension: toBadges(suspensionMap),
            air: airBadges
        }
    };
};

const companyGroups = computed(() => {
    if (!modelVehicles.value.length) return [];
    const map = new Map();
    modelVehicles.value.forEach((detail) => {
        const companyId = detail.vehicle?.company?.id || detail.vehicle?.companyId;
        const companyName = detail.vehicle?.company?.name || '未设置公司';
        const regionId =
            detail.vehicle?.company?.region?.id ||
            detail.vehicle?.company?.regionId ||
            detail.vehicle?.region?.id;
        const regionName =
            detail.vehicle?.company?.region?.name ||
            detail.vehicle?.company?.regionName ||
            detail.vehicle?.region?.name ||
            (regionId ? regionsById.value[regionId] : null) ||
            '地区待补全';

        let entry = map.get(companyId);
        if (!entry) {
            entry = {
                companyId,
                companyName,
                regionName,
                coverImage: resolveImage(detail.images),
                details: []
            };
            map.set(companyId, entry);
        }
        entry.details.push(detail);
        if (!entry.coverImage) {
            entry.coverImage = resolveImage(detail.images);
        }
    });
    return Array.from(map.values()).map((entry) => ({
        ...entry,
        info: aggregateInfo(entry.details),
        total: entry.details.length
    }));
});

const filterSelection = reactive({
    year: '',
    power: '',
    fuel: ''
});

const filterOptions = computed(() => {
    const opts = {
        year: new Set(),
        power: new Set(),
        fuel: new Set()
    };
    companyGroups.value.forEach((group) => {
        group.info.badges.year.forEach((b) => opts.year.add(b.label));
        group.info.badges.power.forEach((b) => opts.power.add(b.label));
        group.info.badges.fuel.forEach((b) => opts.fuel.add(b.label));
    });
    return {
        year: Array.from(opts.year),
        power: Array.from(opts.power),
        fuel: Array.from(opts.fuel)
    };
});

const filteredCompanyGroups = computed(() =>
    companyGroups.value.filter((group) => {
        const matchBadge = (selected, badges) =>
            !selected || badges.some((b) => b.label === selected);
        return (
            matchBadge(filterSelection.year, group.info.badges.year) &&
            matchBadge(filterSelection.power, group.info.badges.power) &&
            matchBadge(filterSelection.fuel, group.info.badges.fuel)
        );
    })
);

const clearModelFilter = () => {
    router.push({ name: 'ModelCatalog' });
};

watch(
    () => selectedModelId.value,
    (id) => {
        if (!id) return;
        store.dispatch('models/loadModelDetail', id);
        store.dispatch('models/loadModelVehicles', { modelId: id });
    },
    { immediate: true }
);

onMounted(() => {
    store.dispatch('models/loadModelCatalog');
    store.dispatch('regions/loadRegions');
});
</script>

<style scoped lang="scss">
/* existing styles kept largely unchanged, only text updated */
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
}

.model-detail {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.filter-grid {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin: 12px 0 16px;
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
    background: #fff;
    color: #2563eb;
    padding: 6px 12px;
    border-radius: 999px;
    cursor: pointer;
    transition: all 0.2s ease;

    &.active,
    &:hover {
        background: #2563eb;
        color: #fff;
        border-color: #2563eb;
    }
}

.company-detail-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
}

.detail-card {
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 16px;
    background: #f9fafb;
    position: relative;
}

.detail-card__title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.detail-card__title,
.detail-card__title:visited {
    font-weight: 600;
    color: #0f172a;
    text-decoration: none;
}

.detail-card__icon {
    text-decoration: none;
    font-size: 1.2rem;
    line-height: 1;
}

.detail-card__region {
    color: #64748b;
    margin-bottom: 8px;
}

.detail-card__image {
    border-radius: 14px;
    overflow: hidden;
    margin-bottom: 12px;

    img {
        width: 100%;
        height: 150px;
        object-fit: cover;
    }
}

.info-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px 16px;

    li {
        display: flex;
        flex-direction: column;
        font-size: 0.85rem;

        span {
            color: #94a3b8;
        }

        strong {
            color: #0f172a;
            margin-top: 2px;
        }
    }
}

.badge-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.count-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 10px;
    background: #eef2ff;
    color: #1d4ed8;
    border-radius: 12px;
    font-weight: 600;

    strong {
        color: #0f172a;
        background: #fff;
        padding: 2px 6px;
        border-radius: 8px;
        box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.15);
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
</style>
