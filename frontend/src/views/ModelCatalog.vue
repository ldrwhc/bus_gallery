<template>
    <div class="page model-catalog">
        <AppHeader />

        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Model</p>
                    <h1>按车型查看运营公司</h1>
                    <p class="subtitle">
                        收录 {{ catalog.length }} 款车型 / {{ totalCompanies }} 家运营公司
                    </p>
                </div>

                <button v-if="selectedModelId" class="ghost-btn" type="button" @click="clearModelFilter">
                    返回全部车型
                </button>
            </header>

            <section v-if="loading" class="state state--loading">
                正在加载型号数据...
            </section>

            <section v-else-if="!filteredModels.length" class="state state--empty">
                暂无型号数据
            </section>

            <section v-else class="model-list">
                <article v-for="model in filteredModels" :key="model.id" class="model-block">
                    <div class="model-block__header">
                        <div>
                            <h2>{{ model.name }}</h2>
                            <p class="meta">
                                品牌：{{ model.brandName || '未知' }} · 公司 {{ model.companies?.length || 0 }}
                            </p>
                        </div>

                        <button class="ghost-btn ghost-btn--sm" type="button"
                            @click="router.push({ name: 'ModelCatalog', params: { modelId: model.id } })">
                            仅看该车型
                        </button>
                    </div>

                    <div class="company-grid">
                        <div v-for="company in model.companies || []" :key="company.id" class="company-pill">
                            <img :src="company.thumbnailUrl || placeholderLogo" :alt="company.name" />
                            <div class="company-pill__body">
                                <p class="company-name">{{ company.name }}</p>
                                <p class="meta">
                                    地区：{{ company.regionName || '未知' }}
                                </p>
                            </div>
                            <button class="text-btn" type="button" @click="goCompany(company.id)">
                                查看公司
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

const catalog = computed(() => store.state.models.catalog);
const loading = computed(() => store.state.models.catalogLoading);
const totalCompanies = computed(() =>
    catalog.value.reduce(
        (sum, model) => sum + (model.companies?.length || 0),
        0
    )
);

const selectedModelId = computed(() => {
    const id = route.params.modelId;
    if (!id) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
});

const filteredModels = computed(() => {
    if (!selectedModelId.value) return catalog.value;
    return catalog.value.filter(
        (model) => Number(model.id) === selectedModelId.value
    );
});

const placeholderLogo = placeholderBus;

const goCompany = (companyId) => {
    router.push({ name: 'CompanyCatalog', params: { companyId } });
};

const clearModelFilter = () => {
    router.push({ name: 'ModelCatalog' });
};

onMounted(() => {
    store.dispatch('models/loadModelCatalog');
});
</script>

<style scoped lang="scss">
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
    margin-bottom: 32px;
}

.eyebrow {
    letter-spacing: 0.25em;
    font-size: 0.75rem;
    color: #0f172a;
    margin-bottom: 8px;
    text-transform: uppercase;
}

.subtitle {
    color: #475569;
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

.meta {
    color: #6b7280;
}

.company-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
}

.company-pill {
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 14px;
    display: flex;
    gap: 12px;
    align-items: center;
    background: #f8fafc;

    img {
        width: 80px;
        height: 60px;
        object-fit: cover;
        border-radius: 12px;
    }
}

.company-name {
    font-weight: 600;
}

.text-btn {
    border: none;
    background: none;
    color: #2563eb;
    font-weight: 600;
    cursor: pointer;
    margin-left: auto;
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

.ghost-btn {
    border: 1px solid rgba(37, 99, 235, 0.3);
    border-radius: 999px;
    background: transparent;
    padding: 8px 18px;
    cursor: pointer;
    color: #1d4ed8;

    &--sm {
        padding: 6px 12px;
        font-size: 0.85rem;
    }

    &:hover {
        background: rgba(37, 99, 235, 0.1);
    }
}
</style>