<template>
    <div class="page search-page">
        <main class="constrained">
            <section class="search-bar-section">
                <form class="search-form" @submit.prevent="doSearch">
                    <el-input v-model="keyword" placeholder="车牌 / 线路号 / 公司 / 车型 / 配置" size="large"
                              @keyup.enter="doSearch" />
                    <el-select v-model="scope" style="width:120px">
                        <el-option label="全部" value="all" />
                        <el-option label="车辆" value="vehicles" />
                        <el-option label="线路" value="routes" />
                    </el-select>
                    <el-button type="primary" :loading="loading" @click="doSearch">搜索</el-button>
                </form>
            </section>

            <p v-if="searchKeyword" class="result-summary">
                找到 {{ totalCount }} 条结果，关键词 <mark>{{ searchKeyword }}</mark>
            </p>

            <!-- UPGRADE(ES): Facet tags become dynamic ES aggregations -->
            <section v-if="result.brands?.items?.length" class="facet-section">
                <span class="facet-label">品牌：</span>
                <span v-for="item in result.brands.items" :key="'b-'+item.id" class="facet-tag"
                      @click="keyword = item.title; doSearch()">
                    {{ item.title }}
                </span>
            </section>

            <!-- Vehicle image cards — UPGRADE(ES): images loaded directly from ES _source -->
            <section v-if="result.vehicles?.items?.length" class="result-section">
                <div class="section-head">
                    <h2>🚎 车辆 ({{ result.vehicles.total }})</h2>
                    <el-button text type="primary" @click="viewAll('vehicles')">查看全部 →</el-button>
                </div>
                <div class="vehicle-image-grid">
                    <div v-for="item in result.vehicles.items" :key="'v-'+item.id" class="vehicle-image-card"
                         @click="$router.push({ name: 'Gallery', query: { keyword: searchKeyword } })">
                        <div class="card-image" :style="{ aspectRatio: '4/3' }">
                            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title"
                                 loading="lazy" decoding="async" />
                            <div v-else class="no-image">暂无图片</div>
                        </div>
                        <div class="card-info">
                            <strong v-html="highlight(item.title)"></strong>
                            <span v-if="item.subtitle" v-html="highlight(item.subtitle)"></span>
                        </div>
                    </div>
                </div>
            </section>

            <!-- Routes -->
            <section v-if="result.routes?.items?.length" class="result-section">
                <div class="section-head">
                    <h2>🚌 公交线路 ({{ result.routes.total }})</h2>
                    <el-button text type="primary" @click="viewAll('routes')">查看全部 →</el-button>
                </div>
                <div v-for="item in result.routes.items" :key="'r-'+item.id" class="result-row"
                     @click="$router.push({ name: 'RouteCatalog' })">
                    <strong v-html="highlight(item.title)"></strong>
                    <span v-if="item.subtitle" v-html="highlight(item.subtitle)"></span>
                </div>
            </section>

            <!-- Companies -->
            <section v-if="result.companies?.items?.length" class="result-section">
                <div class="section-head">
                    <h2>🏢 运营公司 ({{ result.companies.total }})</h2>
                    <el-button text type="primary" @click="viewAll('companies')">查看全部 →</el-button>
                </div>
                <div v-for="item in result.companies.items" :key="'c-'+item.id" class="result-row"
                     @click="$router.push({ name: 'CompanyCatalog', params: { companyId: item.id } })">
                    <strong v-html="highlight(item.title)"></strong>
                    <span v-if="item.subtitle" v-html="highlight(item.subtitle)"></span>
                </div>
            </section>

            <!-- Regions -->
            <section v-if="result.regions?.items?.length" class="result-section">
                <div class="section-head">
                    <h2>📍 地区 ({{ result.regions.total }})</h2>
                </div>
                <div v-for="item in result.regions.items" :key="'rg-'+item.id" class="result-row"
                     @click="$router.push({ name: 'RegionCatalog', params: { regionId: item.id } })">
                    <strong v-html="highlight(item.title)"></strong>
                </div>
            </section>

            <section v-if="!loading && totalCount === 0 && searchKeyword" class="state">
                未找到与 <mark>{{ searchKeyword }}</mark> 相关的结果
            </section>
        </main>
    </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { searchAll } from '@/api/search';

const route = useRoute();
const router = useRouter();

const keyword = ref('');
const scope = ref('all');
const loading = ref(false);
const searchKeyword = ref('');
const result = ref({});

const totalCount = computed(() => {
    let n = 0;
    if (result.value.vehicles) n += result.value.vehicles.total;
    if (result.value.routes) n += result.value.routes.total;
    if (result.value.companies) n += result.value.companies.total;
    if (result.value.regions) n += result.value.regions.total;
    if (result.value.brands) n += result.value.brands.total;
    return n;
});

const highlight = (text) => {
    if (!searchKeyword.value || !text) return text || '';
    const escaped = searchKeyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return String(text).replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>');
};

const doSearch = () => {
    const kw = keyword.value.trim();
    if (!kw) return;
    searchKeyword.value = kw;
    loadResults();
};

const loadResults = async () => {
    loading.value = true;
    try {
        result.value = await searchAll(searchKeyword.value, scope.value) || {};
    } finally {
        loading.value = false;
    }
};

const viewAll = (type) => {
    const kw = searchKeyword.value;
    const targets = {
        vehicles: { name: 'Gallery', query: { keyword: kw } },
        routes: { name: 'RouteCatalog' },
        companies: { name: 'CompanyCatalog', query: { keyword: kw } },
        regions: { name: 'RegionCatalog', query: { keyword: kw } }
    };
    router.push(targets[type] || { name: 'Gallery' });
};

onMounted(() => {
    if (route.query.keyword) {
        keyword.value = route.query.keyword;
        scope.value = route.query.scope || 'all';
        searchKeyword.value = route.query.keyword;
        loadResults();
    }
});

watch(() => route.query.keyword, (kw) => {
    if (kw) {
        keyword.value = kw;
        scope.value = route.query.scope || 'all';
        searchKeyword.value = kw;
        loadResults();
    }
});
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f5f7fb; }
.constrained { width: min(960px, 100%); margin: 0 auto; padding: 32px 16px 72px; }
.search-bar-section { margin-bottom: 20px; }
.search-form { display: flex; gap: 10px; align-items: center; }
.result-summary { color: #475569; margin-bottom: 16px; mark { background: #fef08a; padding: 0 4px; border-radius: 3px; } }

.facet-section {
    display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
    margin-bottom: 20px; padding: 10px 14px; background: #fff; border-radius: 12px;
    border: 1px solid #e2e8f0;
    .facet-label { font-weight: 600; color: #64748b; font-size: 13px; }
    .facet-tag {
        padding: 4px 12px; border-radius: 999px; background: #eff6ff; color: #2563eb;
        font-size: 13px; cursor: pointer; font-weight: 500;
        &:hover { background: #dbeafe; }
    }
}

.result-section { margin-bottom: 28px; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;
    h2 { margin: 0; font-size: 18px; }
}

.vehicle-image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
}

.vehicle-image-card {
    background: #fff; border-radius: 12px; overflow: hidden;
    cursor: pointer; border: 1px solid #e2e8f0;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
    &:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(15,23,42,0.1); }
    .card-image {
        background: #e2e8f0; position: relative; overflow: hidden;
        img { width: 100%; height: 100%; object-fit: cover; position: absolute; inset: 0; display: block; }
        .no-image { display: flex; align-items: center; justify-content: center; height: 100%; color: #94a3b8; font-size: 13px; }
    }
    .card-info {
        padding: 8px 10px;
        strong { display: block; font-size: 14px; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        span { display: block; font-size: 12px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        :deep(mark) { background: #fef08a; padding: 0 3px; border-radius: 2px; }
    }
}

.result-row {
    display: flex; align-items: center; gap: 10px; padding: 10px 14px;
    background: #fff; border-radius: 10px; margin-bottom: 6px;
    cursor: pointer; border: 1px solid #e2e8f0;
    &:hover { background: #f8fafc; }
    strong { color: #111827; }
    span { color: #6b7280; font-size: 13px; }
    :deep(mark) { background: #fef08a; padding: 0 3px; border-radius: 2px; }
}
.state { text-align: center; padding: 48px; color: #94a3b8; mark { background: #fef08a; padding: 0 3px; border-radius: 2px; } }
</style>
