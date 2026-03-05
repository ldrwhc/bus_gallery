<template>
    <div class="page brand-catalog">
        <AppHeader />
        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Brand</p>
                    <h1>按品牌浏览车型</h1>
                    <p class="subtitle">
                        收录 {{ catalog.length }} 个品牌 / {{ totalModels }} 款车型
                    </p>
                </div>
                <button v-if="selectedBrandId" class="ghost-btn" type="button" @click="clearBrandFilter">
                    返回全部品牌
                </button>
            </header>
            <section v-if="loading" class="state state--loading">
                正在加载品牌数据...
            </section>
            <section v-else-if="!filteredBrands.length" class="state state--empty">
                暂无品牌数据
            </section>
            <section v-else class="brand-list">
                <article v-for="brand in filteredBrands" :key="brand.id" class="brand-card">
                    <div class="brand-card__header">
                        <div>
                            <h2>{{ brand.name }}</h2>
                            <p class="meta">
                                国别：{{ brand.country || '未知' }} · 车型 {{ brand.models?.length || 0 }}
                            </p>
                        </div>
                        <button class="ghost-btn ghost-btn--sm" type="button"
                            @click="router.push({ name: 'BrandCatalog', params: { brandId: brand.id } })">
                            仅看该品牌
                        </button>
                    </div>
                    <div class="model-grid">
                        <div v-for="model in brand.models || []" :key="model.id" class="model-card">
                            <img :src="model.thumbnailUrl || placeholderLogo" :alt="model.name" />
                            <div class="model-card__body">
                                <p class="model-name">{{ model.name }}</p>
                                <p class="meta">
                                    长度 {{ model.length || '—' }} m
                                </p>
                            </div>
                            <button class="text-btn" type="button" @click="goModel(model.id)">
                                查看型号
                            </button>
                        </div>
                    </div>
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
const catalog = computed(() => store.state.brands.catalog);
const loading = computed(() => store.state.brands.catalogLoading);
const totalModels = computed(() =>
    catalog.value.reduce(
        (sum, brand) => sum + (brand.models?.length || 0),
        0
    )
);
const selectedBrandId = computed(() => {
    const id = route.params.brandId;
    if (!id) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
});
const filteredBrands = computed(() => {
    if (!selectedBrandId.value) return catalog.value;
    return catalog.value.filter(
        (brand) => Number(brand.id) === selectedBrandId.value
    );
});
const placeholderLogo = placeholderBus;
const goModel = (modelId) => {
    router.push({ name: 'ModelCatalog', params: { modelId } });
};
const clearBrandFilter = () => {
    router.push({ name: 'BrandCatalog' });
};
onMounted(() => {
    store.dispatch('brands/loadBrandCatalog');
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
    margin-bottom: 32px;
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

.meta {
    color: #64748b;
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

    img {
        width: 100%;
        height: 110px;
        border-radius: 12px;
        object-fit: cover;
    }
}

.text-btn {
    border: none;
    background: none;
    color: #4c1d95;
    font-weight: 600;
    cursor: pointer;
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
    border: 1px solid rgba(99, 102, 241, 0.4);
    border-radius: 999px;
    background: transparent;
    padding: 8px 18px;
    color: #4c1d95;
    cursor: pointer;

    &--sm {
        padding: 6px 12px;
        font-size: 0.8rem;
    }

    &:hover {
        background: rgba(99, 102, 241, 0.1);
    }
}
</style>