<template>
    <div class="page brand-catalog">
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Brand</p>
                    <h1>按品牌浏览车型</h1>
                    <p class="subtitle">收录 {{ catalog.length }} 个品牌 / {{ totalModels }} 款车型</p>
                </div>
                <button v-if="selectedBrandId" class="ghost-btn catalog-back-btn" type="button" @click="clearBrandFilter">
                    返回全部品牌
                </button>
            </header>

            <section v-if="initialFilters.length" class="filter-bar">
                <p class="filter-label">首字母</p>
                <div class="chip-row">
                    <button
                        v-for="initial in initialFilters"
                        :key="initial"
                        type="button"
                        :class="['filter-chip', { active: initial === initialFilter }]"
                        @click="selectInitialFilter(initial)"
                    >
                        {{ initial }}
                    </button>
                </div>
            </section>

            <section v-if="loading" class="state state--loading">
                正在加载品牌...
            </section>

            <section v-else-if="!filteredBrands.length" class="state state--empty">
                暂无品牌数据
            </section>

            <section v-else class="brand-list">
                <article v-for="brand in filteredBrands" :key="brand.id" class="brand-card">
                    <div class="brand-card__header">
                        <div>
                            <h2>{{ brand.name }}</h2>
                            <p class="tag">车型 {{ brand.models?.length || 0 }}</p>
                        </div>
                        <button class="pill-btn" type="button" @click="viewBrand(brand.id)">
                            查看该品牌
                        </button>
                    </div>
                    <div class="model-grid">
                        <div v-for="model in brand.models || []" :key="model.id" class="model-card">
                            <router-link class="model-name" :to="{ name: 'ModelCatalog', params: { modelId: model.id } }">
                                {{ model.name }}
                            </router-link>
                            <div class="model-card__image">
                                <img :src="model.thumbnailUrl || placeholderLogo" :alt="model.name" loading="lazy" decoding="async" />
                            </div>
                            <router-link
                                v-if="modelCityMap[model.id]?.regionId"
                                class="city-link"
                                :to="{ name: 'RegionCatalog', params: { regionId: modelCityMap[model.id].regionId } }"
                            >
                                {{ modelCityMap[model.id].regionName }}
                            </router-link>
                            <p v-else class="city-link city-link--placeholder">运用城市待补充</p>
                        </div>
                    </div>
                </article>
            </section>
        </main>
    </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import placeholderBus from '@/assets/images/placeholder-bus.png';

const store = useStore();
const route = useRoute();
const router = useRouter();

const catalog = computed(() => store.state.brands.catalog);
const loading = computed(() => store.state.brands.catalogLoading);
const totalModels = computed(() => catalog.value.reduce((sum, brand) => sum + (brand.models?.length || 0), 0));

const selectedBrandId = computed(() => {
    const id = route.params.brandId;
    if (!id) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
});

const initialFilter = ref('');
const initialsFromCatalog = (brandName = '') => {
    const firstChar = brandName.trim().charAt(0).toUpperCase();
    if (!firstChar) return '#';
    if (/[A-Z]/.test(firstChar)) return firstChar;
    return '#';
};

const initialFilters = computed(() => {
    const set = new Set();
    catalog.value.forEach((brand) => set.add(initialsFromCatalog(brand.name || '')));
    return Array.from(set).sort();
});

const baseBrands = computed(() => {
    if (!selectedBrandId.value) return catalog.value;
    return catalog.value.filter((brand) => Number(brand.id) === selectedBrandId.value);
});

const filteredBrands = computed(() => {
    if (!initialFilter.value) return baseBrands.value;
    return baseBrands.value.filter((brand) => initialsFromCatalog(brand.name || '') === initialFilter.value);
});

const selectInitialFilter = (initial) => {
    initialFilter.value = initialFilter.value === initial ? '' : initial;
};

const placeholderLogo = placeholderBus;

const viewBrand = (brandId) => {
    router.push({ name: 'BrandCatalog', params: { brandId } });
};

const clearBrandFilter = () => {
    router.push({ name: 'BrandCatalog' });
};

watch(
    () => selectedBrandId.value,
    () => {
        initialFilter.value = '';
    }
);

const regionsById = computed(() => {
    const map = {};
    const regions = store.state.regions.list || [];
    regions.forEach((region) => {
        map[region.id] = region.name;
    });
    return map;
});

const modelCityMap = computed(() => {
    const map = {};
    const items = store.state.models.catalog || [];
    items.forEach((model) => {
        const companyWithRegion = model.companies?.find((company) => company.regionName || company.regionId);
        if (companyWithRegion) {
            map[model.id] = {
                regionName: companyWithRegion.regionName || regionsById.value[companyWithRegion.regionId] || '运用城市待补充',
                regionId: companyWithRegion.regionId
            };
        }
    });
    return map;
});

onMounted(() => {
    store.dispatch('brands/loadBrandCatalog');
    store.dispatch('models/loadModelCatalog');
    store.dispatch('regions/loadRegions');
});
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #eef2ff;
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
    box-shadow: 0 8px 24px rgba(99, 102, 241, 0.12);
}

.filter-label {
    margin: 0 0 8px;
    font-size: 0.9rem;
    color: #4c51bf;
}

.chip-row {
    display: flex;
    gap: 8px;
    overflow-x: auto;
}

.filter-chip {
    border: 1px solid rgba(99, 102, 241, 0.3);
    border-radius: 999px;
    padding: 6px 14px;
    background: transparent;
    color: #4c1d95;
    cursor: pointer;
    transition: all 0.2s;

    &.active,
    &:hover {
        background: #4c1d95;
        color: #fff;
        border-color: #4c1d95;
    }
}

.eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.25em;
    font-size: 0.75rem;
    color: #818cf8;
    margin-bottom: 8px;
}

.subtitle {
    color: #4c51bf;
}

.brand-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.brand-card {
    background: #fff;
    border-radius: 24px;
    padding: 28px;
    box-shadow: 0 10px 30px rgba(67, 56, 202, 0.16);
}

.brand-card__header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
}

.tag {
    margin-top: 6px;
    display: inline-flex;
    padding: 4px 12px;
    border-radius: 999px;
    background: rgba(79, 70, 229, 0.1);
    color: #4c1d95;
    font-size: 0.85rem;
}

.model-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 16px;
}

.model-card {
    border-radius: 18px;
    background: linear-gradient(180deg, #f8fafc, #eef2ff);
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    box-shadow: inset 0 0 0 1px rgba(99, 102, 241, 0.15);
}

.model-name,
.model-name:visited {
    font-weight: 600;
    color: #1e1b4b;
    text-decoration: none;
}

.model-card__image {
    border-radius: 12px;
    overflow: hidden;
    height: 120px;

    img {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }
}

.city-link,
.city-link:visited {
    color: #2563eb;
    text-decoration: none;
    font-weight: 600;
}

.city-link--placeholder {
    color: #94a3b8;
    font-weight: 500;
}

.pill-btn {
    border: 1px solid rgba(99, 102, 241, 0.28);
    border-radius: 999px;
    height: 30px;
    padding: 0 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    white-space: nowrap;
    background: rgba(99, 102, 241, 0.06);
    color: #4338ca;
    font-weight: 500;
    font-size: 0.78rem;
    line-height: 1;
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    &:hover {
        background: rgba(99, 102, 241, 0.12);
        border-color: rgba(99, 102, 241, 0.4);
        color: #3730a3;
    }
}

.state {
    border-radius: 18px;
    padding: 48px;
    text-align: center;
    background: #fff;
    color: #4338ca;

    &--loading {
        background: #e0e7ff;
    }

    &--empty {
        color: #94a3b8;
    }
}

.ghost-btn {
    border: 1px solid rgba(99, 102, 241, 0.28);
    border-radius: 999px;
    height: 30px;
    padding: 0 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    white-space: nowrap;
    background: rgba(99, 102, 241, 0.06);
    color: #4338ca;
    font-weight: 500;
    font-size: 0.78rem;
    line-height: 1;
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    &--sm {
        padding: 6px 12px;
        font-size: 0.8rem;
        height: 28px;
    }

    &:hover {
        background: rgba(99, 102, 241, 0.12);
        border-color: rgba(99, 102, 241, 0.4);
        color: #3730a3;
    }
}
</style>
