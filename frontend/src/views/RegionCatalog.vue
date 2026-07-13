<template>
    <div class="page region-catalog">
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Region</p>
                    <h1>按地区查看公交公司</h1>
                    <p class="subtitle">覆盖 {{ cityCatalog.length }} 个地级市 / {{ totalCompanies }} 家公交企业</p>
                </div>
            </header>

            <div v-if="loading" class="state state--loading">正在加载地区数据...</div>
            <div v-else-if="!cityCatalog.length" class="state state--empty">暂无地区数据</div>

            <!-- Split layout: sidebar + main -->
            <div v-else class="split-layout">
                <!-- Sidebar: region list -->
                <aside class="split-sidebar">
                    <p class="sidebar-label">地级市</p>
                    <nav class="sidebar-nav">
                        <button
                            v-for="city in cityCatalog" :key="city.id"
                            :class="['sidebar-item', { active: city.id === activeRegionId }]"
                            @click="activeRegionId = city.id"
                        >
                            <span class="si-name">{{ city.name }}</span>
                            <span class="si-count">{{ city.companies?.length || 0 }}</span>
                        </button>
                    </nav>
                </aside>

                <!-- Main: companies of selected region -->
                <section class="split-main">
                    <template v-if="activeRegion">
                        <header class="main-header">
                            <h2>{{ activeRegion.name }}</h2>
                            <p class="main-meta">{{ activeRegion.companies?.length || 0 }} 家公交公司</p>
                        </header>

                        <div v-if="activeRegion.companies?.length" class="company-grid">
                            <article
                                v-for="company in activeRegion.companies" :key="company.id"
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

                    <!-- Default: first region or empty prompt -->
                    <div v-else class="main-empty">
                        <p>← 从左侧选择一个地区查看详情</p>
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

const activeRegionId = ref(null);
const catalog = computed(() => store.state.regions.catalog);

const isProvinceName = (name = '') => {
    const n = (name || '').trim();
    if (!n) return false;
    return n.endsWith('省') || n.includes('自治区') || n.endsWith('特别行政区') || n.endsWith('兵团');
};

const cityCatalog = computed(() =>
    catalog.value.filter((r) => r?.name && !isProvinceName(r.name) && (r.companies?.length || 0) > 0)
);

const loading = computed(() => store.state.regions.catalogLoading);

const totalCompanies = computed(() =>
    cityCatalog.value.reduce((sum, r) => sum + (r.companies?.length || 0), 0)
);

const activeRegion = computed(() =>
    cityCatalog.value.find((r) => r.id === activeRegionId.value) || null
);

const placeholderLogo = placeholderBus;

const goCompany = (companyId) => {
    router.push({ name: 'CompanyCatalog', params: { companyId } });
};

// Sync from route param or default to first city
watch(cityCatalog, (list) => {
    if (!list.length) return;
    const routeId = route.params.regionId ? Number(route.params.regionId) : null;
    if (routeId && list.some((r) => r.id === routeId)) {
        activeRegionId.value = routeId;
    } else if (!activeRegionId.value || !list.some((r) => r.id === activeRegionId.value)) {
        activeRegionId.value = list[0].id;
    }
}, { immediate: true });

watch(() => route.params.regionId, (id) => {
    const num = id ? Number(id) : null;
    if (num && cityCatalog.value.some((r) => r.id === num)) {
        activeRegionId.value = num;
    }
});

onMounted(() => {
    store.dispatch('regions/loadRegionCatalog');
});
</script>

<style scoped lang="scss">
.page { min-height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }
.constrained {
    width: min(1100px, 100%); margin: 0 auto; flex: 1;
    padding: 28px 20px 64px;
}

// ---- Header ----
.catalog-header {
    margin-bottom: 24px;
    .eyebrow { letter-spacing: 0.3em; font-size: 0.72rem; color: #9ca3af; text-transform: uppercase; margin: 0 0 4px; }
    h1 { margin: 0 0 4px; font-size: 1.5rem; font-weight: 700; color: #111827; }
    .subtitle { margin: 0; font-size: 0.88rem; color: #6b7280; }
}

// ---- Split layout ----
.split-layout { display: flex; gap: 20px; align-items: flex-start; }

// ---- Sidebar ----
.split-sidebar {
    width: 200px; flex-shrink: 0;
    background: #fff; border-radius: 14px; padding: 16px 0;
    box-shadow: 0 4px 16px rgba(15,23,42,0.04);
    position: sticky; top: 20px;
}
.sidebar-label { padding: 0 16px 10px; font-size: 0.75rem; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.08em; }
.sidebar-nav { display: flex; flex-direction: column; }
.sidebar-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 10px 16px; border: none; background: transparent; cursor: pointer;
    font-size: 0.9rem; color: #374151; text-align: left; width: 100%;
    transition: background 0.15s;
    &:hover { background: #f1f5f9; }
    &.active { background: #eff6ff; color: #2563eb; font-weight: 600;
        .si-count { background: #2563eb; color: #fff; }
    }
}
.si-name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.si-count {
    font-size: 0.72rem; min-width: 22px; height: 22px; border-radius: 11px;
    background: #f1f5f9; color: #64748b;
    display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

// ---- Main area ----
.split-main {
    flex: 1; min-width: 0;
    background: #fff; border-radius: 14px; padding: 24px;
    box-shadow: 0 4px 16px rgba(15,23,42,0.04);
    min-height: 400px;
}
.main-header { margin-bottom: 18px;
    h2 { margin: 0 0 2px; font-size: 1.25rem; color: #111827; }
}
.main-meta { margin: 0; font-size: 0.85rem; color: #6b7280; }
.main-empty { display: flex; align-items: center; justify-content: center; height: 300px; color: #9ca3af; font-size: 0.95rem; }

// ---- Company grid ----
.company-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;
}
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

// ---- State ----
.state { text-align: center; padding: 64px; color: #94a3b8;
    &--loading { color: #2563eb; }
}

// ---- Responsive ----
@media (max-width: 700px) {
    .split-layout { flex-direction: column; }
    .split-sidebar { width: 100%; position: static; padding: 12px 0; }
    .sidebar-nav { flex-direction: row; flex-wrap: wrap; padding: 0 8px; gap: 4px; }
    .sidebar-item { width: auto; padding: 8px 14px; border-radius: 8px;
        &.active { border-radius: 8px; }
    }
    .si-count { display: none; }
    .sidebar-label { padding-bottom: 6px; }
    .company-grid { grid-template-columns: 1fr; }
}
</style>

