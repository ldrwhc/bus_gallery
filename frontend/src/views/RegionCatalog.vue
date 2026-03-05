<template>
    <div class="page region-catalog">
        <AppHeader />
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Region</p>
                    <h1>按地区查看公交公司</h1>
                    <p class="subtitle">
                        覆盖 {{ catalog.length }} 个地级市 / {{ totalCompanies }} 家公交企业
                    </p>
                </div>
                <button v-if="selectedRegionId" class="ghost-btn" type="button" @click="clearRegionFilter">
                    返回全部地区
                </button>
            </header>
            <section v-if="loading" class="state state--loading">
                正在加载地区数据...
            </section>
            <section v-else-if="!regionsToRender.length" class="state state--empty">
                暂无地区数据
            </section>
            <section v-else class="region-list">
                <article v-for="region in regionsToRender" :key="region.id" class="region-card">
                    <header class="region-card__header">
                        <div>
                            <h2>{{ region.name }}</h2>
                            <p class="meta">
                                共 {{ region.companies?.length || 0 }} 家公交公司
                            </p>
                        </div>
                        <button class="ghost-btn ghost-btn--sm" type="button"
                            @click="router.push({ name: 'RegionCatalog', params: { regionId: region.id } })">
                            仅看本地区
                        </button>
                    </header>
                    <div v-if="region.companies?.length" class="company-grid">
                        <article v-for="company in region.companies || []" :key="company.id" class="company-card"
                            @click="goCompany(company.id)">
                            <img :src="company.thumbnailUrl || placeholderLogo" :alt="company.name" />
                            <div>
                                <p class="company-name">{{ company.name }}</p>
                                <p class="meta">
                                    车型 {{ company.modelsCount ?? '未知' }}
                                </p>
                            </div>
                        </article>
                    </div>
                    <p v-else class="empty-meta">暂无公司数据</p>
                </article>
            </section>
        </main>
        <AppFooter />
    </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import AppHeader from '@/components/Layout/AppHeader.vue';
import AppFooter from '@/components/Layout/AppFooter.vue';
import placeholderBus from '@/assets/images/placeholder-bus.png';
const store = useStore();
const route = useRoute();
const router = useRouter();
const catalog = computed(() => store.state.regions.catalog);
const loading = computed(() => store.state.regions.catalogLoading);
const totalCompanies = computed(() =>
    catalog.value.reduce(
        (sum, region) => sum + (region.companies?.length || 0),
        0
    )
);
const selectedRegionId = computed(() => {
    const id = route.params.regionId;
    if (!id) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
});
const regionsToRender = computed(() => {
    if (!selectedRegionId.value) return catalog.value;
    return catalog.value.filter(
        (region) => Number(region.id) === selectedRegionId.value
    );
});
const placeholderLogo = placeholderBus;
const clearRegionFilter = () => {
    router.push({ name: 'RegionCatalog' });
};
const goCompany = (companyId) => {
    router.push({ name: 'CompanyCatalog', params: { companyId } });
};
onMounted(() => {
    store.dispatch('regions/loadRegionCatalog');
});
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f7fb;
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
    margin-bottom: 32px;
}

.eyebrow {
    letter-spacing: 0.3em;
    font-size: 0.75rem;
    color: #9ca3af;
    text-transform: uppercase;
    margin-bottom: 8px;
}

.subtitle {
    color: #6b7280;
}

.region-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.region-card {
    background: #fff;
    border-radius: 24px;
    padding: 28px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.region-card__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18px;
}

.meta {
    color: #6b7280;
    font-size: 0.9rem;
}

.empty-meta {
    color: #94a3b8;
    font-style: italic;
}

.company-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
}

.company-card {
    background: #f8fafc;
    border-radius: 16px;
    padding: 12px;
    display: flex;
    gap: 12px;
    cursor: pointer;
    transition: transform 0.2s ease, background 0.2s;
    border: 1px solid transparent;

    &:hover {
        transform: translateY(-4px);
        background: #fff;
        border-color: #dbeafe;
    }

    img {
        width: 72px;
        height: 48px;
        object-fit: cover;
        border-radius: 10px;
    }

    .company-name {
        font-weight: 600;
        margin-bottom: 4px;
    }
}

.state {
    border-radius: 20px;
    padding: 48px;
    text-align: center;
    background: #fff;
    color: #475569;

    &--loading {
        background: #e0f2fe;
        color: #0369a1;
    }

    &--empty {
        color: #94a3b8;
    }
}

.ghost-btn {
    border: 1px solid rgba(15, 23, 42, 0.15);
    background: transparent;
    border-radius: 999px;
    padding: 8px 18px;
    cursor: pointer;
    color: #0f172a;
    transition: all 0.2s;

    &--sm {
        padding: 6px 14px;
        font-size: 0.85rem;
    }

    &:hover {
        background: rgba(15, 23, 42, 0.05);
    }
}
</style>