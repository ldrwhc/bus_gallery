<template>
    <div class="page brand-catalog">
        <main class="catalog-main constrained">
            <!-- ========== Detail View: single brand ========== -->
            <template v-if="selectedBrandId && currentBrand">
                <header class="detail-header">
                    <a class="back-link" @click.prevent="clearBrandFilter">← 返回全部品牌</a>
                </header>
                <div class="detail-headline">
                    <h1 class="detail-brand-name">{{ currentBrand.chnName || currentBrand.name }}</h1>
                    <p class="detail-brand-meta">{{ currentBrand.models?.length || 0 }} 款车型</p>
                </div>

                <section v-if="loading" class="state state--loading">正在加载车型...</section>
                <section v-else-if="!currentBrand.models?.length" class="state state--empty">暂无车型数据</section>

                <section v-else class="model-grid">
                    <div v-for="model in currentBrand.models" :key="model.id" class="model-card">
                        <router-link class="model-name" :to="{ name: 'ModelCatalog', params: { modelId: model.id } }">
                            {{ model.name }}
                        </router-link>
                        <div class="model-card__image">
                            <img :src="model.thumbnailUrl || placeholderLogo" :alt="model.name" loading="lazy" decoding="async" />
                        </div>
                        <div v-if="modelCityMap[model.id]?.length" class="city-links">
                            <template v-for="(city, index) in modelCityMap[model.id].slice(0, 2)" :key="`${model.id}-${city.regionId || city.regionName}`">
                                <router-link v-if="city.regionId" class="city-link"
                                    :to="{ name: 'RegionCatalog', params: { regionId: city.regionId } }">
                                    {{ city.regionName }}
                                </router-link>
                                <span v-else class="city-link city-link--placeholder">{{ city.regionName }}</span>
                                <span v-if="index < Math.min(modelCityMap[model.id].length, 2) - 1" class="city-sep">、</span>
                            </template>
                            <span v-if="modelCityMap[model.id].length > 2" class="city-more">等</span>
                        </div>
                        <p v-else class="city-link city-link--placeholder">运用城市待补充</p>
                    </div>
                </section>
            </template>

            <!-- ========== List View: all brands ========== -->
            <template v-else>
                <header class="catalog-header">
                    <div>
                        <p class="eyebrow">Brand</p>
                        <h1>按品牌浏览车型</h1>
                        <p class="subtitle">收录 {{ catalog.length }} 个品牌 / {{ totalModels }} 款车型</p>
                    </div>
                </header>

                <section v-if="initialFilters.length" class="filter-bar">
                    <p class="filter-label">首字母</p>
                    <div class="chip-row">
                        <button v-for="initial in initialFilters" :key="initial" type="button"
                            :class="['filter-chip', { active: initial === initialFilter }]"
                            @click="selectInitialFilter(initial)">{{ initial }}</button>
                    </div>
                </section>

                <section v-if="loading" class="state state--loading">正在加载品牌...</section>
                <section v-else-if="!filteredBrands.length" class="state state--empty">暂无品牌数据</section>

                <section v-else class="brand-list">
                    <article v-for="brand in filteredBrands" :key="brand.id" class="brand-card">
                        <div class="brand-card__header">
                            <div class="brand-card__title">
                                <router-link class="brand-name-link" :to="{ name: 'BrandCatalog', params: { brandId: brand.id } }">
                                    <h2>{{ brand.chnName || brand.name }}</h2>
                                </router-link>
                                <p class="tag">车型 {{ brand.models?.length || 0 }}</p>
                            </div>
                            <button class="pill-btn" type="button"
                                @click="toggleBrandExpand(brand.id)"
                                v-if="(brand.models?.length || 0) > 10">
                                {{ expandedBrandIds.has(brand.id) ? '收起' : `查看全部 ${brand.models.length} 款` }}
                            </button>
                        </div>
                        <div class="model-grid">
                            <div v-for="model in (expandedBrandIds.has(brand.id) ? brand.models : (brand.models || []).slice(0, 10))" :key="model.id" class="model-card">
                                <router-link class="model-name" :to="{ name: 'ModelCatalog', params: { modelId: model.id } }">
                                    {{ model.name }}
                                </router-link>
                                <div class="model-card__image">
                                    <img :src="model.thumbnailUrl || placeholderLogo" :alt="model.name" loading="lazy" decoding="async" />
                                </div>
                                <div v-if="modelCityMap[model.id]?.length" class="city-links">
                                    <template v-for="(city, index) in modelCityMap[model.id].slice(0, 2)" :key="`${model.id}-${city.regionId || city.regionName}`">
                                        <router-link v-if="city.regionId" class="city-link"
                                            :to="{ name: 'RegionCatalog', params: { regionId: city.regionId } }">
                                            {{ city.regionName }}
                                        </router-link>
                                        <span v-else class="city-link city-link--placeholder">{{ city.regionName }}</span>
                                        <span v-if="index < Math.min(modelCityMap[model.id].length, 2) - 1" class="city-sep">、</span>
                                    </template>
                                    <span v-if="modelCityMap[model.id].length > 2" class="city-more">等</span>
                                </div>
                                <p v-else class="city-link city-link--placeholder">运用城市待补充</p>
                            </div>
                        </div>
                    </article>
                </section>
            </template>
        </main>
    </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue';
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

const currentBrand = computed(() => {
    if (!selectedBrandId.value) return null;
    return catalog.value.find((b) => Number(b.id) === selectedBrandId.value) || null;
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

const baseBrands = computed(() => catalog.value);

const filteredBrands = computed(() => {
    if (!initialFilter.value) return baseBrands.value;
    return baseBrands.value.filter((brand) => initialsFromCatalog(brand.name || '') === initialFilter.value);
});

const selectInitialFilter = (initial) => {
    initialFilter.value = initialFilter.value === initial ? '' : initial;
};

const placeholderLogo = placeholderBus;

const expandedBrandIds = ref(new Set());

const toggleBrandExpand = (brandId) => {
    const next = new Set(expandedBrandIds.value);
    if (next.has(brandId)) {
        next.delete(brandId);
    } else {
        next.add(brandId);
    }
    expandedBrandIds.value = next;
};

const clearBrandFilter = () => {
    router.push({ name: 'BrandCatalog' });
};

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
        const regions = [];
        const unique = new Set();
        (model.companies || []).forEach((company) => {
            const regionId = company.regionId || null;
            const regionName = company.regionName || regionsById.value[regionId] || '';
            if (!regionName) return;
            const key = `${regionId || 'x'}-${regionName}`;
            if (unique.has(key)) return;
            unique.add(key);
            regions.push({ regionId, regionName });
        });
        map[model.id] = regions;
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
.page { min-height: 100vh; display: flex; flex-direction: column; background: #eef2ff; }
.constrained { width: min(1200px, 100%); margin: 0 auto; flex: 1; padding: 32px 24px 72px; }

/* ========== Detail View ========== */
.detail-header { margin-bottom: 4px; }
.back-link { color: #4338ca; text-decoration: none; font-size: 14px; cursor: pointer;
    &:hover { text-decoration: underline; } }
.detail-headline { margin-bottom: 24px; }
.detail-brand-name { margin: 4px 0 4px; font-size: 1.6rem; font-weight: 700; color: #111827; }
.detail-brand-meta { margin: 0; font-size: 0.9rem; color: #6b7280; }

/* ========== List View ========== */
.catalog-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px;
    h1 { margin: 0 0 6px; font-size: 1.6rem; font-weight: 700; color: #111827; } }
.eyebrow { text-transform: uppercase; letter-spacing: 0.3em; font-size: 0.72rem; color: #9ca3af; margin: 0 0 4px; }
.subtitle { margin: 0; font-size: 0.9rem; color: #6b7280; }
.filter-bar { background: #fff; border-radius: 18px; padding: 16px 20px; margin-bottom: 24px; box-shadow: 0 8px 24px rgba(99,102,241,0.12); }
.filter-label { margin: 0 0 8px; font-size: 0.9rem; color: #4c51bf; }
.chip-row { display: flex; gap: 8px; overflow-x: auto; }
.filter-chip { border: 1px solid rgba(99,102,241,0.3); border-radius: 999px; padding: 6px 14px; background: transparent; color: #4c1d95; cursor: pointer; transition: all 0.2s;
    &.active, &:hover { background: #4c1d95; color: #fff; border-color: #4c1d95; } }
.brand-list { display: flex; flex-direction: column; gap: 24px; }
.brand-card { background: #fff; border-radius: 24px; padding: 28px; box-shadow: 0 10px 30px rgba(67,56,202,0.16); }
.brand-card__header { display: flex; justify-content: space-between; margin-bottom: 16px; }
.brand-card__title { display: flex; align-items: center; gap: 10px; h2 { margin: 0; } }
.brand-name-link { text-decoration: none; color: inherit;
    &:hover h2 { color: #2563eb; } }
.tag { display: inline-flex; padding: 4px 12px; border-radius: 999px; background: rgba(79,70,229,0.1); color: #4c1d95; font-size: 0.85rem; }

/* ========== Shared: Model Grid ========== */
.model-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.model-card { border-radius: 18px; background: linear-gradient(180deg, #f8fafc, #eef2ff); padding: 12px; display: flex; flex-direction: column; gap: 12px; box-shadow: inset 0 0 0 1px rgba(99,102,241,0.15); }
.model-name, .model-name:visited { font-weight: 600; color: #1e1b4b; text-decoration: none; }
.model-card__image { border-radius: 12px; overflow: hidden; height: 120px;
    img { width: 100%; height: 100%; object-fit: cover; } }
.city-link, .city-link:visited { color: #2563eb; text-decoration: none; font-weight: 600; }
.city-links { display: flex; align-items: center; flex-wrap: wrap; }
.city-sep { color: #64748b; margin: 0 2px; }
.city-more { color: #64748b; margin-left: 2px; }
.city-link--placeholder { color: #94a3b8; font-weight: 500; }

.pill-btn { border: 1px solid rgba(99,102,241,0.28); border-radius: 999px; height: 30px; padding: 0 12px; display: inline-flex; align-items: center; justify-content: center; white-space: nowrap; background: rgba(99,102,241,0.06); color: #4338ca; font-weight: 500; font-size: 0.78rem; line-height: 1; cursor: pointer; transition: background 0.2s, border-color 0.2s, color 0.2s;
    &:hover { background: rgba(99,102,241,0.12); border-color: rgba(99,102,241,0.4); color: #3730a3; } }
.state { border-radius: 18px; padding: 48px; text-align: center; background: #fff; color: #4338ca;
    &--loading { background: #e0e7ff; }
    &--empty { color: #94a3b8; } }
.ghost-btn { border: 1px solid rgba(99,102,241,0.28); border-radius: 999px; height: 30px; padding: 0 12px; display: inline-flex; align-items: center; justify-content: center; white-space: nowrap; background: rgba(99,102,241,0.06); color: #4338ca; font-weight: 500; font-size: 0.78rem; line-height: 1; cursor: pointer;
    &:hover { background: rgba(99,102,241,0.12); border-color: rgba(99,102,241,0.4); color: #3730a3; } }
@media (max-width: 560px) { .model-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; } }
</style>
