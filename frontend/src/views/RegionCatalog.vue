<template>
    <div class="page region-catalog">
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Region</p>
                    <h1>按地区查看公交公司</h1>
                    <p class="subtitle">覆盖 {{ cityCount }} 个地级市 / {{ totalCompanies }} 家公交企业</p>
                </div>
            </header>

            <div v-if="loading" class="state state--loading">正在加载地区数据...</div>
            <div v-else-if="!provinceTree.length" class="state state--empty">暂无地区数据</div>

            <div v-else class="split-layout">
                <!-- Sidebar: province → city tree -->
                <aside class="split-sidebar">
                    <p class="sidebar-label">地区</p>
                    <nav class="sidebar-nav">
                        <div v-for="prov in provinceTree" :key="prov.id" class="tree-group">
                            <button
                                :class="['tree-prov', { open: expandedProvinceIds.includes(prov.id) }]"
                                @click="toggleProvince(prov.id)"
                            >
                                <span class="tp-arrow">{{ expandedProvinceIds.includes(prov.id) ? '▾' : '▸' }}</span>
                                <span class="tp-name">{{ prov.name }}</span>
                                <span class="tp-count">{{ prov.cities.length }}</span>
                            </button>
                            <div v-if="expandedProvinceIds.includes(prov.id)" class="tree-cities">
                                <button
                                    v-for="city in prov.cities" :key="city.id"
                                    :class="['tree-city', { active: city.id === activeCityId }]"
                                    @click="selectCity(city)"
                                >
                                    <span class="tc-name">{{ city.name }}</span>
                                    <span class="tc-count">{{ city.companyCount || 0 }}</span>
                                </button>
                            </div>
                        </div>
                    </nav>
                </aside>

                <!-- Main: companies -->
                <section class="split-main">
                    <template v-if="activeCity">
                        <header class="main-header">
                            <h2>{{ activeCity.name }}</h2>
                            <p class="main-meta">{{ activeCity.companyCount || 0 }} 家公交公司</p>
                        </header>

                        <div v-if="activeCity.companies?.length" class="company-grid">
                            <article
                                v-for="company in activeCity.companies" :key="company.id"
                                class="company-card"
                                @click="goCompany(company.id)"
                            >
                                <img
                                    :src="company.thumbnailUrl || placeholderLogo"
                                    :alt="company.name" loading="lazy" decoding="async"
                                />
                                <div class="cc-info">
                                    <p class="company-name">{{ company.name }}</p>
                                    <p class="company-meta">车型 {{ company.modelsCount ?? '—' }}</p>
                                </div>
                            </article>
                        </div>
                        <p v-else class="empty-meta">暂无公司数据</p>
                    </template>

                    <div v-else class="main-empty">
                        <p>← 从左侧选择一个城市查看详情</p>
                    </div>
                </section>
            </div>
        </main>
    </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import placeholderBus from '@/assets/images/placeholder-bus.png';

const store = useStore();
const route = useRoute();
const router = useRouter();

const activeCityId = ref(null);
const placeholderLogo = placeholderBus;
const expandedProvinceIds = ref([]);

// Raw data
const allRegions = computed(() => store.state.regions.list || []);
const catalog = computed(() => store.state.regions.catalog || []);
const loading = computed(() => store.state.regions.loading || store.state.regions.catalogLoading);

// Build catalog map: cityId → { companies, companyCount }
const catalogByCityId = computed(() => {
    const map = {};
    catalog.value.forEach((r) => {
        if (r?.companies?.length) {
            map[r.id] = { companies: r.companies, companyCount: r.companies.length };
        }
    });
    return map;
});

// Build province tree
const provinceTree = computed(() => {
    const provinces = allRegions.value.filter((r) => r.level === 1 || r.regionType === 'PROVINCE');
    return provinces.map((prov) => {
        const cities = allRegions.value
            .filter((r) => r.provinceId === prov.id && r.id !== prov.id)
            .map((city) => ({
                ...city,
                companyCount: catalogByCityId.value[city.id]?.companyCount || 0,
                companies: catalogByCityId.value[city.id]?.companies || []
            }));
        // 直辖市：没有下属城市，但自身有公司数据，将自身作为"城市"条目
        if (cities.length === 0) {
            const selfCatalog = catalogByCityId.value[prov.id];
            if (selfCatalog) {
                cities.push({
                    ...prov,
                    companyCount: selfCatalog.companyCount || 0,
                    companies: selfCatalog.companies || []
                });
            }
        }
        return { ...prov, cities };
    }).filter((prov) => prov.cities.length > 0);
});

const cityCount = computed(() => {
    let n = 0;
    provinceTree.value.forEach((p) => { n += p.cities.length; });
    return n;
});

const totalCompanies = computed(() => {
    let n = 0;
    provinceTree.value.forEach((p) => {
        p.cities.forEach((c) => { n += c.companyCount || 0; });
    });
    return n;
});

const activeCity = computed(() => {
    if (!activeCityId.value) return null;
    for (const prov of provinceTree.value) {
        const city = prov.cities.find((c) => c.id === activeCityId.value);
        if (city) return city;
    }
    return null;
});

const toggleProvince = (id) => {
    const idx = expandedProvinceIds.value.indexOf(id);
    if (idx >= 0) {
        expandedProvinceIds.value.splice(idx, 1);
    } else {
        // Collapse all other provinces — only one open at a time
        expandedProvinceIds.value = [id];
    }
};

const selectCity = (city) => {
    if (activeCityId.value === city.id) return; // no-op if already selected
    activeCityId.value = city.id;
    // Sync URL silently (no data reload — just for deep linking)
    router.replace({ name: 'RegionCatalog', params: { regionId: city.id } }).catch(() => {});
};

const goCompany = (companyId) => {
    router.push({ name: 'CompanyCatalog', params: { companyId } });
};

// Expand province containing the active city
const expandProvinceForCity = (cityId) => {
    for (const prov of provinceTree.value) {
        if (prov.cities.some((c) => c.id === cityId)) {
            if (!expandedProvinceIds.value.includes(prov.id)) {
                expandedProvinceIds.value.push(prov.id);
            }
            return;
        }
    }
};

// Route sync (only when coming from external navigation, not from selectCity)
watch(() => route.params.regionId, (id) => {
    const num = id ? Number(id) : null;
    if (num && num !== activeCityId.value) {
        expandProvinceForCity(num);
        activeCityId.value = num;
    }
});

onMounted(async () => {
    // Only fetch if not already loaded (prevents flash on re-mount)
    const tasks = [];
    if (!store.state.regions.list.length) tasks.push(store.dispatch('regions/loadRegions'));
    if (!store.state.regions.catalog.length) tasks.push(store.dispatch('regions/loadRegionCatalog'));
    if (tasks.length) await Promise.all(tasks);

    if (!activeCityId.value) {
        const routeId = route.params.regionId ? Number(route.params.regionId) : null;
        if (routeId) {
            expandProvinceForCity(routeId);
            activeCityId.value = routeId;
        }
    }
});
</script>

<style scoped lang="scss">
.page { min-height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }
.constrained {
    width: min(1200px, 100%); margin: 0 auto; flex: 1;
    padding: 32px 24px 72px;
}

// ---- Header ----
.catalog-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;
    h1 { margin: 0 0 6px; font-size: 1.6rem; font-weight: 700; color: #111827; }
}
.eyebrow { text-transform: uppercase; letter-spacing: 0.3em; font-size: 0.72rem; color: #9ca3af; margin: 0 0 4px; }
.subtitle { margin: 0; font-size: 0.9rem; color: #6b7280; }

// ---- Split layout ----
.split-layout { display: flex; gap: 20px; align-items: flex-start; }

// ---- Sidebar ----
.split-sidebar {
    width: 220px; flex-shrink: 0;
    background: #fff; border-radius: 14px; padding: 12px 0;
    box-shadow: 0 4px 16px rgba(15,23,42,0.04);
    position: sticky; top: 20px; max-height: calc(100vh - 60px); overflow-y: auto;
}
.sidebar-label { padding: 0 16px 8px; font-size: 0.72rem; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.08em; }
.sidebar-nav { display: flex; flex-direction: column; }

// Tree: province
.tree-group { border-bottom: 1px solid #f1f5f9; }
.tree-prov {
    display: flex; align-items: center; gap: 6px;
    width: 100%; padding: 10px 14px; border: none; background: transparent;
    cursor: pointer; font-size: 0.88rem; color: #1e293b; font-weight: 600;
    text-align: left; transition: background 0.12s;
    &:hover { background: #f8fafc; }
}
.tp-arrow { font-size: 0.7rem; color: #94a3b8; width: 14px; flex-shrink: 0; transition: transform 0.15s; }
.tp-name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tp-count {
    font-size: 0.68rem; min-width: 20px; height: 20px; border-radius: 10px;
    background: #e2e8f0; color: #64748b; display: flex; align-items: center; justify-content: center;
}

// Tree: city
.tree-cities { background: #fafbfc; }
.tree-city {
    display: flex; align-items: center; justify-content: space-between;
    width: 100%; padding: 8px 14px 8px 34px; border: none; background: transparent;
    cursor: pointer; font-size: 0.85rem; color: #475569; text-align: left;
    transition: background 0.12s;
    &:hover { background: #f1f5f9; }
    &.active { background: #eff6ff; color: #2563eb; font-weight: 600;
        .tc-count { background: #2563eb; color: #fff; }
    }
}
.tc-name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tc-count {
    font-size: 0.68rem; min-width: 20px; height: 20px; border-radius: 10px;
    background: #e2e8f0; color: #64748b; display: flex; align-items: center; justify-content: center;
}

// ---- Main ----
.split-main {
    flex: 1; min-width: 0;
    background: #fff; border-radius: 14px; padding: 24px;
    box-shadow: 0 4px 16px rgba(15,23,42,0.04); min-height: 400px;
}
.main-header { margin-bottom: 18px;
    h2 { margin: 0 0 2px; font-size: 1.2rem; color: #111827; }
}
.main-meta { margin: 0; font-size: 0.85rem; color: #6b7280; }
.main-empty { display: flex; align-items: center; justify-content: center; height: 300px; color: #9ca3af; font-size: 0.95rem; }

// Company grid
.company-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.company-card {
    display: flex; align-items: center; gap: 10px;
    padding: 10px; border-radius: 10px; background: #f8fafc;
    cursor: pointer; border: 1px solid transparent;
    transition: background 0.15s, border-color 0.15s;
    &:hover { background: #fff; border-color: #dbeafe; }
    img { width: 56px; height: 40px; object-fit: cover; border-radius: 6px; flex-shrink: 0; background: #e2e8f0; }
}
.cc-info { min-width: 0; }
.company-name { font-weight: 600; font-size: 0.88rem; margin: 0 0 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.company-meta { margin: 0; font-size: 0.75rem; color: #94a3b8; }
.empty-meta { color: #94a3b8; font-size: 0.9rem; }

// State
.state { text-align: center; padding: 64px; color: #94a3b8;
    &--loading { color: #2563eb; }
}

// Responsive
@media (max-width: 700px) {
    .split-layout { flex-direction: column; }
    .split-sidebar { width: 100%; position: static; max-height: none; }
    .company-grid { grid-template-columns: 1fr; }
}
</style>
