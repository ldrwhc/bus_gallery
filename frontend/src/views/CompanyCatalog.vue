<template>
    <div class="page company-catalog">
        <AppHeader />

        <main class="catalog-main constrained">
            <header class="catalog-header">
                <div>
                    <p class="eyebrow">Company</p>
                    <h1>按公司查看车型</h1>
                    <p class="subtitle">
                        共 {{ catalog.length }} 家公司 / {{ modelsCount }} 款车型
                    </p>
                </div>

                <button v-if="selectedCompanyId" class="ghost-btn" type="button" @click="clearCompanyFilter">
                    返回全部公司
                </button>
            </header>

            <section v-if="loading" class="state state--loading">
                正在加载公司数据...
            </section>

            <section v-else-if="!filteredCompanies.length" class="state state--empty">
                暂无公司数据
            </section>

            <section v-else class="company-list">
                <article v-for="company in filteredCompanies" :key="company.id" class="company-block">
                    <div class="company-block__header">
                        <div>
                            <h2>{{ company.name }}</h2>
                            <p class="meta">
                                所在地区：{{ company.regionName || '未知' }}
                            </p>
                        </div>
                        <p class="meta">车型 {{ company.models?.length || 0 }} 款</p>
                    </div>

                    <div v-if="company.models?.length" class="model-grid">
                        <div v-for="model in company.models" :key="model.id" class="model-card">
                            <img :src="model.thumbnailUrl || placeholderLogo" :alt="model.name" />
                            <div class="model-card__body">
                                <p class="model-name">{{ model.name }}</p>
                                <p class="meta">{{ model.brandName || '品牌未知' }}</p>
                            </div>
                            <button class="text-btn" type="button" @click="goModel(model.id)">
                                查看型号
                            </button>
                        </div>
                    </div>

                    <p v-else class="empty-meta">暂无车型数据</p>
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

const catalog = computed(() => store.state.companies.catalog);
const loading = computed(() => store.state.companies.catalogLoading);
const modelsCount = computed(() =>
    catalog.value.reduce(
        (sum, company) => sum + (company.models?.length || 0),
        0
    )
);

const selectedCompanyId = computed(() => {
    const id = route.params.companyId;
    if (!id) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
});

const filteredCompanies = computed(() => {
    if (!selectedCompanyId.value) return catalog.value;
    return catalog.value.filter(
        (company) => Number(company.id) === selectedCompanyId.value
    );
});

const placeholderLogo = placeholderBus;

const goModel = (modelId) => {
    router.push({ name: 'ModelCatalog', params: { modelId } });
};

const clearCompanyFilter = () => {
    router.push({ name: 'CompanyCatalog' });
};

onMounted(() => {
    store.dispatch('companies/loadCompanyCatalog');
});
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f8fafc;
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
    text-transform: uppercase;
    letter-spacing: 0.25em;
    font-size: 0.75rem;
    color: #9ca3af;
    margin-bottom: 8px;
}

.subtitle {
    color: #6b7280;
}

.company-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.company-block {
    background: #fff;
    border-radius: 24px;
    padding: 28px;
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.company-block__header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 18px;
}

.meta {
    color: #94a3b8;
}

.model-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 16px;
}

.model-card {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    background: #fdfdfd;
    display: flex;
    flex-direction: column;
    gap: 12px;
    transition: all 0.2s;

    &:hover {
        border-color: #bfdbfe;
        box-shadow: 0 10px 30px rgba(59, 130, 246, 0.15);
    }

    img {
        width: 100%;
        height: 120px;
        object-fit: cover;
        border-radius: 12px;
    }
}

.model-name {
    font-weight: 600;
}

.text-btn {
    border: none;
    background: none;
    color: #2563eb;
    font-weight: 600;
    cursor: pointer;
    text-align: left;
}

.empty-meta {
    color: #cbd5f5;
    font-style: italic;
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
    border: 1px solid rgba(99, 102, 241, 0.3);
    background: transparent;
    border-radius: 24px;
    padding: 8px 18px;
    cursor: pointer;
    color: #4c1d95;

    &:hover {
        background: rgba(79, 70, 229, 0.1);
    }
}
</style>