<template>
    <div class="page route-catalog">
        <main class="constrained">
            <section class="hero">
                <h1>公交线路</h1>
                <p class="desc">按城市、公司、类型浏览公交线路，查看各线路运营车辆</p>
            </section>

            <section class="filter-bar">
                <el-select v-model="filters.regionId" placeholder="城市" clearable filterable
                           @change="reload" size="default" class="filter-bar__select">
                    <el-option v-for="r in regionOptions" :key="r.value" :label="r.label" :value="r.value" />
                </el-select>
                <el-select v-model="filters.companyId" placeholder="公司" clearable filterable
                           @change="reload" size="default" class="filter-bar__select">
                    <el-option v-for="c in companyOptions" :key="c.value" :label="c.label" :value="c.value" />
                </el-select>
                <el-input v-model="filters.keyword" placeholder="搜索线路号" clearable
                          @keyup.enter="reload" class="filter-bar__search">
                    <template #append>
                        <el-button :icon="Search" @click="reload" style="border:none;background:transparent" />
                    </template>
                </el-input>
                <el-radio-group v-model="filters.activeFilter" @change="reload" size="small" class="filter-bar__radio">
                    <el-radio-button value="active">运营中</el-radio-button>
                    <el-radio-button value="all">全部</el-radio-button>
                </el-radio-group>
                <el-button class="filter-bar__advanced-btn"
                           @click="showAdvanced = !showAdvanced"
                           :type="showAdvanced ? 'primary' : ''">
                    高级筛选
                    <el-icon class="filter-bar__arrow">
                        <ArrowDown v-if="!showAdvanced" />
                        <ArrowUp v-else />
                    </el-icon>
                </el-button>
            </section>

            <section v-show="showAdvanced" class="advanced-filter">
                <div class="advanced-filter__inner">
                    <span class="advanced-filter__label">线路类型</span>
                    <el-tag v-for="t in typeChips" :key="t.value"
                            :type="filters.routeType === t.value ? 'primary' : ''"
                            :effect="filters.routeType === t.value ? 'dark' : 'plain'"
                            class="advanced-filter__chip"
                            @click="toggleType(t.value)">
                        {{ t.label }}
                    </el-tag>
                </div>
            </section>

            <div v-if="loading" class="state">加载中...</div>
            <div v-else-if="!routes.length" class="state">暂无线路数据</div>

            <div v-else>
                <div v-for="group in groupedRoutes" :key="group.city" class="city-group">
                    <div class="city-group__header" @click="toggleCity(group.city)">
                        <h2>
                            <el-icon class="city-group__arrow" :class="{ 'is-expanded': expandedCities[group.city] }">
                                <ArrowRight />
                            </el-icon>
                            {{ group.city }}
                            <span class="count">({{ group.routes.length }})</span>
                        </h2>
                        <el-button v-if="group.routes.length > groupShowCount && !isMobile" text type="primary" size="small"
                            @click.stop="toggleCity(group.city)">
                            {{ expandedCities[group.city] ? '收起' : `展开全部 ${group.routes.length} 条` }}
                        </el-button>
                    </div>
                    <div v-show="!isMobile || expandedCities[group.city]" class="route-grid">
                        <div v-for="r in (expandedCities[group.city] ? group.routes : group.routes.slice(0, groupShowCount))"
                             :key="r.id" class="route-card"
                             @click="onCardClick(r, $event)">
                            <div class="route-card__head">
                                <span class="route-number">{{ r.routeNumber }}</span><span v-if="r.region?.name" class="route-region">{{ r.region.name }}</span>
                                <el-tag v-if="r.subType" size="small">{{ subTypeLabel(r.subType) }}</el-tag>
                                <el-tag size="small" :type="r.isActive ? 'success' : 'info'">
                                    {{ r.isActive ? '运营' : '停运' }}
                                </el-tag>
                                <span v-if="hasAsymmetricStops(r)" class="asymmetric-toggle" @click="toggleDirection(r.id)" title="切换上下行">⇄</span>
                            </div>
                            <p class="route-card__stops" v-if="!r.isLoop && r.startStop">
                                <template v-if="hasAsymmetricStops(r)">
                                    <span class="stop-dir__tag" :class="stopDirection[r.id] ? 'stop-dir__tag--down' : 'stop-dir__tag--up'">{{ stopDirection[r.id] ? '下' : '上' }}</span>
                                    {{ stopDirection[r.id] ? (r.downStartStop || r.endStop) + ' → ' + (r.downEndStop || r.startStop) : r.startStop + ' → ' + r.endStop }}
                                </template>
                                <span v-else>{{ r.startStop }} ↔ {{ r.endStop }}</span>
                            </p>
                            <p class="route-card__stops" v-else-if="r.isLoop && r.startStop">{{ r.startStop }} ↔ {{ r.endStop }}</p>
                            <p class="route-card__meta">
                                <span v-if="r.company?.name">{{ r.company.name }}</span>
                                <span v-if="r.region?.name"> · {{ r.region.name }}</span>
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { fetchRoutes } from '@/api/routes';
import { ArrowDown, ArrowUp, ArrowRight, Search } from '@element-plus/icons-vue';

const store = useStore();
const router = useRouter();
const routes = ref([]);
const loading = ref(false);
const showAdvanced = ref(false);

const filters = reactive({
    regionId: null,
    companyId: null,
    routeType: null,
    keyword: '',
    activeFilter: 'all'
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
const hasAsymmetricStops = (r) => {
    const downStart = r.downStartStop || '';
    const downEnd = r.downEndStop || '';
    if (!downStart && !downEnd) return false;
    return downStart !== (r.startStop || '') || downEnd !== (r.endStop || '');
};

// Toggle stop direction per asymmetric route card
const stopDirection = reactive({});
const toggleDirection = (routeId) => {
    stopDirection[routeId] = !stopDirection[routeId];
};

const onCardClick = (r, event) => {
    if (event.target.closest('.asymmetric-toggle')) return;
    router.push({ name: 'RouteDetail', params: { routeId: r.id } });
};

const groupShowCount = 10;
const expandedCities = reactive({});

// Mobile detection
const isMobile = ref(false);
let resizeTimer = null;
const checkMobile = () => {
    isMobile.value = window.innerWidth <= 680;
};
onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile); });
onUnmounted(() => { window.removeEventListener('resize', checkMobile); });

// Sort route numbers numerically: "1路" < "2路" < "10路" < "694路" < "快1路"
const sortRouteNumber = (a, b) => {
    const parse = (s) => {
        const m = s.match(/^(\D*)(\d+)/);
        if (m) return { prefix: m[1], num: parseInt(m[2], 10) };
        return { prefix: s, num: 0 };
    };
    const pa = parse(a.routeNumber);
    const pb = parse(b.routeNumber);
    if (pa.prefix !== pb.prefix) return pa.prefix.localeCompare(pb.prefix, 'zh');
    return pa.num - pb.num;
};

const groupedRoutes = computed(() => {
    const map = {};
    routes.value.forEach(r => {
        const city = r.region?.name || '未知城市';
        if (!map[city]) map[city] = [];
        map[city].push(r);
    });
    // Sort within each city
    return Object.entries(map).map(([city, list]) => ({
        city,
        routes: [...list].sort(sortRouteNumber)
    }));
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
            page: 1, size: 200,
            regionId: filters.regionId || undefined,
            companyId: filters.companyId || undefined,
            routeType: filters.routeType || undefined,
            keyword: filters.keyword || undefined,
            isActive: filters.activeFilter === 'active' ? true : undefined
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
.page { min-height: 100vh; background: #f5f7fb; user-select: none; }
.constrained { width: min(1200px, 100%); margin: 0 auto; padding: 32px 16px 72px; }
.hero { margin-bottom: 24px; h1 { margin: 0; } .desc { color: #6b7280; margin-top: 8px; } }

// Filter bar — polished single-row card
.filter-bar {
    display: flex; gap: 12px; align-items: center; flex-wrap: nowrap;
    background: #fff; border-radius: 12px; padding: 12px 16px;
    box-shadow: 0 1px 4px rgba(15,23,42,.06); margin-bottom: 16px;
}
.filter-bar__select { width: 150px; flex-shrink: 0; }
.filter-bar__search { flex: 1 1 200px; max-width: 360px; }
.filter-bar__radio { flex-shrink: 0; }
.filter-bar__advanced-btn { flex-shrink: 0; font-weight: 500; }
.filter-bar__arrow { margin-left: 4px; }

// Advanced filter
.advanced-filter {
    background: #fff; border-radius: 12px; padding: 14px 18px;
    margin-bottom: 16px; box-shadow: 0 4px 16px rgba(15,23,42,.06);
}
.advanced-filter__inner { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.advanced-filter__label { font-size: 13px; color: #6b7280; margin-right: 8px; white-space: nowrap; }
.advanced-filter__chip { cursor: pointer; margin: 0 4px 4px 0; }

// Route grid
.route-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 10px;
    > * { min-width: 0; }
}

// Route card
.route-card {
    background: #fff; border-radius: 10px; padding: 10px 12px;
    box-shadow: 0 2px 8px rgba(15,23,42,.06); cursor: pointer;
    transition: transform .15s, box-shadow .15s; overflow: hidden;
    &:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(15,23,42,.1); }
}
.route-card__head { display: flex; align-items: center; gap: 4px; margin-bottom: 4px; flex-wrap: wrap; }
.route-number { font-size: 15px; font-weight: 700; color: #111827; }
.route-card__stops { color: #374151; margin: 0 0 4px; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stop-dir { display: flex; align-items: center; gap: 4px; }
.stop-dir__tag {
    flex-shrink: 0; font-size: 10px; font-weight: 700; padding: 0 3px; border-radius: 3px;
    line-height: 15px; display: inline-block;
}
.stop-dir__tag--up   { background: #dbeafe; color: #1d4ed8; }
.stop-dir__tag--down { background: #fce7f3; color: #be185d; }
.asymmetric-toggle {
    color: #94a3b8; font-size: 11px; margin-left: 3px; cursor: pointer;
    display: inline-block; transition: color .15s, transform .15s; flex-shrink: 0;
    &:hover { color: #2563eb; transform: scale(1.2); }
}
.route-card__meta { color: #6b7280; font-size: 11px; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

// City group
.city-group { margin-bottom: 24px; }
.city-group__header {
    display: flex; align-items: center; gap: 10px; margin-bottom: 10px; cursor: pointer;
    h2 { margin: 0; font-size: 17px; color: #0f172a; display: flex; align-items: center; gap: 4px; }
    .count { color: #94a3b8; font-weight: 400; font-size: 13px; }
}
.city-group__arrow {
    font-size: 14px; transition: transform .2s;
    &.is-expanded { transform: rotate(90deg); }
}
.state { text-align: center; padding: 48px; color: #94a3b8; }

// Responsive
@media (max-width: 1100px) { .route-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 900px) { .route-grid { grid-template-columns: repeat(3, 1fr); } }

// Mobile: single column, all cities collapsed by default
@media (max-width: 680px) {
    .route-grid {
        grid-template-columns: 1fr;
        gap: 8px;
    }
    .route-card { padding: 10px 12px; }
    .city-group__header {
        padding: 8px 0;
        h2 { font-size: 15px; }
    }
}
</style>
