<template>
    <div class="page route-catalog">
        <main class="constrained">
            <section class="hero">
                <h1>公交线路</h1>
                <p class="desc">按城市、公司、类型浏览公交线路，查看各线路运营车辆</p>
            </section>

            <section class="filter-bar">
                <el-select v-model="filters.regionId" placeholder="城市" clearable filterable
                           @change="reload">
                    <el-option v-for="r in regionOptions" :key="r.value" :label="r.label" :value="r.value" />
                </el-select>
                <el-select v-model="filters.companyId" placeholder="公司" clearable filterable
                           @change="reload">
                    <el-option v-for="c in companyOptions" :key="c.value" :label="c.label" :value="c.value" />
                </el-select>
                <el-input v-model="filters.keyword" placeholder="搜索线路号" clearable
                          @keyup.enter="reload" style="width:200px" />
                <el-radio-group v-model="filters.isActive" @change="reload" size="small">
                    <el-radio-button :value="true">运营中</el-radio-button>
                    <el-radio-button :value="undefined">全部</el-radio-button>
                </el-radio-group>
            </section>

            <section class="type-chips">
                <el-tag v-for="t in typeChips" :key="t.value"
                        :type="filters.routeType === t.value ? 'primary' : ''"
                        :effect="filters.routeType === t.value ? 'dark' : 'plain'"
                        style="cursor:pointer;margin:0 6px 6px 0"
                        @click="toggleType(t.value)">
                    {{ t.label }}
                </el-tag>
            </section>

            <div v-if="loading" class="state">加载中...</div>
            <div v-else-if="!routes.length" class="state">暂无线路数据</div>

            <div v-else>
                <div v-for="group in groupedRoutes" :key="group.city" class="city-group">
                    <div class="city-group__header">
                        <h2>{{ group.city }} <span class="count">({{ group.routes.length }})</span></h2>
                        <el-button v-if="group.routes.length > groupShowCount" text type="primary" size="small"
                            @click="toggleCity(group.city)">
                            {{ expandedCities[group.city] ? '收起' : `展开全部 ${group.routes.length} 条` }}
                        </el-button>
                    </div>
                    <div class="route-grid">
                        <div v-for="r in expandedCities[group.city] ? group.routes : group.routes.slice(0, groupShowCount)"
                             :key="r.id" class="route-card"
                             @click="$router.push({ name: 'RouteDetail', params: { routeId: r.id } })">
                            <div class="route-card__head">
                                <span class="route-number">{{ r.routeNumber }}</span>
                                <el-tag v-if="r.subType" size="small">{{ subTypeLabel(r.subType) }}</el-tag>
                                <el-tag size="small" :type="r.isActive ? 'success' : 'info'">
                                    {{ r.isActive ? '运营' : '停运' }}
                                </el-tag>
                            </div>
                            <p class="route-card__stops" v-if="!r.isLoop && r.startStop">
                                {{ r.startStop }} ↔ {{ r.endStop }}
                            </p>
                            <p class="route-card__stops" v-else-if="r.isLoop">环线</p>
                            <p class="route-card__meta">
                                <span v-if="r.company?.name">{{ r.company.name }}</span>
                                <span v-if="r.region?.name"> · {{ r.region.name }}</span>
                                <span> · {{ routeTypeLabel(r.routeType) }}</span>
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useStore } from 'vuex';
import { fetchRoutes } from '@/api/routes';

const store = useStore();
const routes = ref([]);
const loading = ref(false);

const filters = reactive({
    regionId: null,
    companyId: null,
    routeType: null,
    keyword: '',
    isActive: true
});

const regionOptions = computed(() =>
    (store.state.regions?.list || []).map(r => ({ value: r.id, label: r.name }))
);
const companyOptions = computed(() =>
    (store.state.companies?.list || []).map(c => ({ value: c.id, label: c.name }))
);

const SUB_TYPE_LABELS = { INTERVAL: '区间', BRANCH: '支线', EXPRESS: '快线', NIGHT: '夜班', DIRECT: '直达' };
const ROUTE_TYPE_LABELS = { REGULAR: '常规', BRT: '快速公交', AIRPORT: '机场', TOURIST: '旅游', COMMUNITY: '微循环', SUBWAY: '地铁接驳' };
const subTypeLabel = (v) => SUB_TYPE_LABELS[v] || v;
const routeTypeLabel = (v) => ROUTE_TYPE_LABELS[v] || v;

const groupShowCount = 8;
const expandedCities = reactive({});

const groupedRoutes = computed(() => {
    const map = {};
    routes.value.forEach(r => {
        const city = r.region?.name || '未知城市';
        if (!map[city]) map[city] = [];
        map[city].push(r);
    });
    return Object.entries(map).map(([city, list]) => ({ city, routes: list }));
});

const toggleCity = (city) => {
    expandedCities[city] = !expandedCities[city];
};

const typeChips = [
    { value: null, label: '全部' },
    { value: 'REGULAR', label: '常规' },
    { value: 'BRT', label: 'BRT' },
    { value: 'NIGHT', label: '夜班' },
    { value: 'EXPRESS', label: '快线' },
    { value: 'INTERVAL', label: '区间' },
    { value: 'AIRPORT', label: '机场' },
    { value: 'TOURIST', label: '旅游' }
];

const toggleType = (val) => {
    filters.routeType = filters.routeType === val ? null : val;
    reload();
};

const reload = async () => {
    loading.value = true;
    try {
        const params = {
            page: 1, size: 100,
            regionId: filters.regionId || undefined,
            companyId: filters.companyId || undefined,
            routeType: filters.routeType || undefined,
            keyword: filters.keyword || undefined,
            isActive: filters.isActive
        };
        const resp = await fetchRoutes(params);
        routes.value = Array.isArray(resp?.records) ? resp.records : (Array.isArray(resp) ? resp : []);
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    store.dispatch('regions/loadRegions');
    store.dispatch('companies/loadCompanies');
    reload();
});
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f5f7fb; }
.constrained { width: min(1200px, 100%); margin: 0 auto; padding: 32px 16px 72px; }
.hero { margin-bottom: 24px; h1 { margin: 0; } .desc { color: #6b7280; margin-top: 8px; } }
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 12px; }
.type-chips { margin-bottom: 20px; }
.route-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.route-card {
    background: #fff; border-radius: 16px; padding: 18px;
    box-shadow: 0 8px 24px rgba(15,23,42,.08); cursor: pointer;
    transition: transform .15s;
    &:hover { transform: translateY(-2px); }
}
.route-card__head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.route-number { font-size: 20px; font-weight: 700; color: #111827; }
.route-card__stops { color: #374151; margin: 0 0 8px; font-size: 14px; }
.route-card__meta { color: #6b7280; font-size: 13px; margin: 0; }
.city-group { margin-bottom: 28px; }
.city-group__header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px;
    h2 { margin: 0; font-size: 18px; color: #0f172a; }
    .count { color: #94a3b8; font-weight: 400; font-size: 14px; }
}
.state { text-align: center; padding: 48px; color: #94a3b8; }
</style>
