<template>
    <div class="page compare-page">
        <main class="constrained">
            <section class="hero">
                <h1>车型对比</h1>
                <p class="desc">选择车型，对比不同地区/公司/批次的配置参数</p>
            </section>

            <section class="selector-bar">
                <el-select
                    v-model="selectedModelIds"
                    multiple
                    filterable
                    placeholder="搜索并选择车型..."
                    clearable
                    collapse-tags
                    collapse-tags-tooltip
                    :max-collapse-tags="3"
                    size="large"
                    class="model-select"
                    :loading="modelsLoading"
                >
                    <el-option
                        v-for="m in filteredModelOptions"
                        :key="m.value"
                        :label="m.label"
                        :value="m.value"
                    />
                </el-select>
                <el-button
                    type="primary"
                    :disabled="selectedModelIds.length < 1"
                    @click="loadVehicles"
                    :loading="vehiclesLoading"
                >
                    加载配置
                </el-button>
            </section>

            <!-- Vehicle picker for each model -->
            <section v-if="modelVehicles.length" class="vehicle-pickers">
                <div v-for="mv in modelVehicles" :key="mv.modelId" class="vehicle-picker">
                    <div class="vehicle-picker__head">
                        <span class="vehicle-picker__model">{{ mv.modelName }}</span>
                        <span class="vehicle-picker__brand">{{ mv.brandName }}</span>
                        <button class="vehicle-picker__remove" @click="removeModel(mv.modelId)" title="移除">&times;</button>
                    </div>
                    <el-select
                        v-model="mv.selectedVehicleId"
                        placeholder="选择配置"
                        size="default"
                        class="vehicle-picker__select"
                        @change="onVehicleChange(mv.modelId)"
                    >
                        <el-option
                            v-for="v in mv.vehicles"
                            :key="v.vehicleId"
                            :label="vehicleLabel(v)"
                            :value="v.vehicleId"
                        />
                    </el-select>
                </div>
            </section>

            <section v-if="compareLoading" class="state state--loading">
                <el-icon class="is-loading" :size="20"><Loading /></el-icon>
                正在加载配置数据...
            </section>

            <section v-else-if="compareData.length === 0 && selectedModelIds.length > 0 && !vehiclesLoading" class="state state--empty">
                选择车型后点击「加载配置」，然后为每款车型选择具体配置
            </section>

            <!-- Desktop: comparison table -->
            <section v-else-if="compareData.length >= 1" class="compare-table-wrap">
                <table class="compare-table">
                    <thead>
                        <tr>
                            <th class="compare-table__row-label">配置项</th>
                            <th v-for="col in compareData" :key="col.modelId">
                                <div class="model-header">
                                    <button class="model-header__remove" @click="removeModel(col.modelId)" title="移除">&times;</button>
                                    <img
                                        :src="col.imageUrl"
                                        :alt="col.modelName"
                                        class="model-header__img"
                                        @error="onImageError($event)"
                                    />
                                    <span class="model-header__name">{{ col.modelName }}</span>
                                    <span class="model-header__sub" v-if="col.regionName">{{ col.regionName }}</span>
                                    <span class="model-header__sub" v-if="col.companyName">{{ col.companyName }}</span>
                                    <span class="model-header__sub" v-if="col.factoryDate">{{ formatDate(col.factoryDate) }}</span>
                                </div>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in specRows" :key="row.key">
                            <td class="compare-table__row-label">{{ row.label }}</td>
                            <td v-for="col in compareData" :key="col.modelId" class="compare-table__cell">
                                <template v-if="row.type === 'link' && col[row.key] && col[row.key + 'Id']">
                                    <router-link :to="resolveLink(row.linkTo, col[row.key + 'Id'])">
                                        {{ col[row.key] }}
                                    </router-link>
                                </template>
                                <template v-else-if="row.type === 'date'">
                                    {{ formatDate(col[row.key]) }}
                                </template>
                                <template v-else-if="row.type === 'bool'">
                                    <el-tag :type="col[row.key] ? 'success' : 'info'" size="small">
                                        {{ col[row.key] ? '是' : '否' }}
                                    </el-tag>
                                </template>
                                <template v-else>
                                    {{ col[row.key] || '—' }}
                                </template>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </section>

            <!-- Mobile: card layout -->
            <section v-if="compareData.length" class="compare-cards">
                <div v-for="col in compareData" :key="col.modelId" class="compare-card">
                    <div class="compare-card__header">
                        <img :src="col.imageUrl" :alt="col.modelName" class="compare-card__img" @error="onImageError($event)" />
                        <div>
                            <h3>{{ col.modelName }}</h3>
                            <p>{{ [col.regionName, col.companyName, col.factoryDate ? formatDate(col.factoryDate) : ''].filter(Boolean).join(' · ') || col.brandName }}</p>
                        </div>
                        <button class="compare-card__remove" @click="removeModel(col.modelId)">&times;</button>
                    </div>
                    <dl class="compare-card__specs">
                        <div v-for="row in specRows" :key="row.key" class="compare-card__spec">
                            <dt>{{ row.label }}</dt>
                            <dd>
                                <template v-if="row.type === 'link' && col[row.key] && col[row.key + 'Id']">
                                    <router-link :to="resolveLink(row.linkTo, col[row.key + 'Id'])">
                                        {{ col[row.key] }}
                                    </router-link>
                                </template>
                                <template v-else-if="row.type === 'date'">
                                    {{ formatDate(col[row.key]) }}
                                </template>
                                <template v-else-if="row.type === 'bool'">
                                    <el-tag :type="col[row.key] ? 'success' : 'info'" size="small">
                                        {{ col[row.key] ? '是' : '否' }}
                                    </el-tag>
                                </template>
                                <template v-else>
                                    {{ col[row.key] || '—' }}
                                </template>
                            </dd>
                        </div>
                    </dl>
                </div>
            </section>
        </main>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { fetchModels, fetchModelVehicles } from '@/api/models';
import { formatDate } from '@/utils/formatters';
import { Loading } from '@element-plus/icons-vue';
import placeholderBus from '@/assets/images/placeholder-bus.png';

const allModels = ref([]);
const modelsLoading = ref(false);
const selectedModelIds = ref([]);
const vehiclesLoading = ref(false);
const compareLoading = ref(false);
const compareData = ref([]);

// modelVehicles: [{ modelId, modelName, brandName, vehicles: [{ vehicleId, label, ...fullData }], selectedVehicleId }]
const modelVehicles = ref([]);

const filteredModelOptions = computed(() =>
    allModels.value.map(m => ({
        label: `${m.name}  ${m.brand?.chnName || m.brand?.name || ''}`,
        value: m.id
    }))
);

const vehicleLabel = (v) => {
    const parts = [];
    if (v.regionName) parts.push(v.regionName);
    if (v.companyName) parts.push(v.companyName);
    if (v.factoryDate) parts.push(formatDate(v.factoryDate));
    if (v.plateNumber) parts.push(v.plateNumber);
    return parts.join(' · ') || '未知配置';
};

const specRows = [
    { key: 'brandName', label: '品牌', type: 'link', linkTo: 'BrandCatalog' },
    { key: 'regionName', label: '地区', type: 'link', linkTo: 'RegionCatalog' },
    { key: 'companyName', label: '公司', type: 'link', linkTo: 'CompanyCatalog' },
    { key: 'motor', label: '电机' },
    { key: 'engine', label: '发动机' },
    { key: 'fuelType', label: '燃料类型' },
    { key: 'transmissionSystem', label: '变速系统' },
    { key: 'stepType', label: '踏步' },
    { key: 'suspension', label: '悬挂' },
    { key: 'axle', label: '车桥' },
    { key: 'factoryDate', label: '出厂日期', type: 'date' },
    { key: 'launchDate', label: '上线日期', type: 'date' },
    { key: 'airConditioned', label: '空调', type: 'bool' }
];

const resolveLink = (routeName, id) => {
    if (!id) return '';
    const params = {};
    if (routeName === 'BrandCatalog') params.brandId = id;
    else if (routeName === 'CompanyCatalog') params.companyId = id;
    else if (routeName === 'RegionCatalog') params.regionId = id;
    else if (routeName === 'ModelCatalog') params.modelId = id;
    return { name: routeName, params };
};

const onImageError = (e) => { e.target.src = placeholderBus; };

const removeModel = (modelId) => {
    selectedModelIds.value = selectedModelIds.value.filter(id => id !== modelId);
    modelVehicles.value = modelVehicles.value.filter(mv => mv.modelId !== modelId);
    compareData.value = compareData.value.filter(c => c.modelId !== modelId);
};

const extractVehicleData = (vehicleResp, modelId) => {
    const vehicle = vehicleResp?.vehicle || vehicleResp || {};
    const config = vehicleResp?.vehicleConfig || {};
    const images = vehicleResp?.images || [];
    const imageUrl = images[0]?.thumbnailUrl || images[0]?.url || placeholderBus;

    return {
        vehicleId: vehicle?.id,
        modelId,
        modelName: vehicle?.model?.name || '',
        brandName: vehicle?.model?.brand?.chnName || vehicle?.model?.brand?.name || '',
        brandId: vehicle?.model?.brand?.id || null,
        plateNumber: vehicle?.plateNumber || null,
        motor: config?.motor || null,
        engine: config?.engine || null,
        transmissionSystem: config?.transmissionSystem || null,
        fuelType: config?.fuelType || null,
        stepType: config?.stepType || null,
        suspension: config?.suspension || null,
        axle: config?.axle || null,
        regionName: vehicle?.region?.name || null,
        regionId: vehicle?.region?.id || null,
        companyName: vehicle?.company?.name || null,
        companyId: vehicle?.company?.id || null,
        factoryDate: vehicle?.factoryDate || null,
        launchDate: vehicle?.launchDate || null,
        airConditioned: vehicle?.airConditioned ?? null,
        imageUrl
    };
};

const loadVehicles = async () => {
    if (!selectedModelIds.value.length) return;
    vehiclesLoading.value = true;
    modelVehicles.value = [];
    compareData.value = [];
    try {
        const results = await Promise.all(
            selectedModelIds.value.map(async (modelId) => {
                try {
                    const resp = await fetchModelVehicles(modelId);
                    const list = Array.isArray(resp?.records) ? resp.records
                        : Array.isArray(resp?.data) ? resp.data
                        : Array.isArray(resp) ? resp : [];
                    const vehicles = list.map(v => extractVehicleData(v, modelId));
                    return {
                        modelId,
                        modelName: vehicles[0]?.modelName || allModels.value.find(m => m.id === modelId)?.name || '—',
                        brandName: vehicles[0]?.brandName || allModels.value.find(m => m.id === modelId)?.brand?.chnName || allModels.value.find(m => m.id === modelId)?.brand?.name || '—',
                        vehicles,
                        selectedVehicleId: vehicles[0]?.vehicleId || null
                    };
                } catch {
                    const m = allModels.value.find(x => x.id === modelId);
                    return {
                        modelId,
                        modelName: m?.name || '—',
                        brandName: m?.brand?.chnName || m?.brand?.name || '—',
                        vehicles: [],
                        selectedVehicleId: null
                    };
                }
            })
        );
        modelVehicles.value = results;
        // Auto-build comparison from selected vehicles
        updateComparison();
    } finally {
        vehiclesLoading.value = false;
    }
};

const onVehicleChange = (modelId) => {
    updateComparison();
};

const updateComparison = () => {
    compareLoading.value = true;
    const result = [];
    for (const mv of modelVehicles.value) {
        if (!mv.selectedVehicleId) continue;
        const v = mv.vehicles.find(x => x.vehicleId === mv.selectedVehicleId);
        if (v) result.push(v);
    }
    compareData.value = result;
    compareLoading.value = false;
};

onMounted(async () => {
    modelsLoading.value = true;
    try {
        const data = await fetchModels();
        allModels.value = Array.isArray(data?.records) ? data.records
            : Array.isArray(data?.data) ? data.data
            : Array.isArray(data) ? data : [];
    } finally {
        modelsLoading.value = false;
    }
});
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f5f7fb; }
.constrained { width: min(1200px, 100%); margin: 0 auto; padding: 32px 16px 72px; }
.hero { margin-bottom: 24px; h1 { margin: 0; } .desc { color: #6b7280; margin-top: 8px; } }

.selector-bar {
    display: flex; gap: 12px; align-items: center;
    background: #fff; border-radius: 12px; padding: 16px 20px;
    box-shadow: 0 1px 4px rgba(15,23,42,.06); margin-bottom: 16px;
    .model-select { flex: 1 1 320px; min-width: 200px; }
}

// Vehicle picker row
.vehicle-pickers {
    display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 20px;
}
.vehicle-picker {
    background: #fff; border-radius: 12px; padding: 12px 16px;
    box-shadow: 0 1px 4px rgba(15,23,42,.06);
    min-width: 220px; flex: 1 1 240px;
    &__head {
        display: flex; align-items: center; gap: 8px; margin-bottom: 8px; position: relative;
    }
    &__model { font-weight: 700; color: #0f172a; font-size: 14px; }
    &__brand { font-size: 12px; color: #94a3b8; }
    &__remove {
        margin-left: auto; border: none; background: transparent; color: #94a3b8;
        font-size: 18px; cursor: pointer; flex-shrink: 0;
        &:hover { color: #ef4444; }
    }
    &__select { width: 100%; }
}

.state {
    text-align: center; padding: 64px 24px; border-radius: 16px; background: #fff;
    color: #94a3b8; font-size: 15px;
    &--loading { color: #2563eb; }
    &--empty { color: #94a3b8; }
}

// Desktop table
.compare-table-wrap {
    overflow-x: auto;
    background: #fff; border-radius: 16px;
    box-shadow: 0 4px 20px rgba(15,23,42,.06);
}
.compare-table {
    width: 100%; border-collapse: collapse; min-width: 600px; font-size: 13px;
    th, td {
        padding: 10px 14px; text-align: center; border-bottom: 1px solid #f1f5f9;
        vertical-align: middle; min-width: 130px;
    }
    &__row-label {
        text-align: left !important; font-weight: 600; color: #475569;
        background: #f8fafc; position: sticky; left: 0; z-index: 1;
        min-width: 80px !important; width: 90px;
    }
    &__cell { color: #1e293b; }
    thead th {
        background: #fff; position: sticky; top: 0; z-index: 2; padding: 16px 12px 12px;
    }
    tbody tr:nth-child(even) td { background: #fafbfc; }
}

.model-header {
    display: flex; flex-direction: column; align-items: center; gap: 4px; position: relative;
    &__img {
        width: 140px; height: 94px; object-fit: cover; border-radius: 10px; background: #f1f5f9;
    }
    &__name { font-weight: 700; color: #0f172a; font-size: 14px; }
    &__sub { font-size: 11px; color: #94a3b8; }
    &__remove {
        position: absolute; top: -10px; right: -2px;
        border: none; background: #ef4444; color: #fff;
        width: 20px; height: 20px; border-radius: 50%;
        cursor: pointer; font-size: 13px; line-height: 1;
        display: flex; align-items: center; justify-content: center;
        &:hover { background: #dc2626; }
    }
}

// Mobile cards
.compare-cards { display: none; flex-direction: column; gap: 16px; }

.compare-card {
    background: #fff; border-radius: 16px; box-shadow: 0 2px 12px rgba(15,23,42,.06); overflow: hidden;
    &__header {
        display: flex; align-items: center; gap: 12px; padding: 16px;
        background: #f8fafc; border-bottom: 1px solid #e2e8f0; position: relative;
        h3 { margin: 0; font-size: 15px; }
        p { margin: 4px 0 0; color: #94a3b8; font-size: 12px; }
    }
    &__img {
        width: 80px; height: 56px; object-fit: cover; border-radius: 8px; flex-shrink: 0;
    }
    &__remove {
        position: absolute; top: 10px; right: 12px;
        border: none; background: transparent; color: #94a3b8;
        font-size: 22px; cursor: pointer;
        &:hover { color: #ef4444; }
    }
    &__specs { padding: 12px 16px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; }
    &__spec {
        dt { color: #94a3b8; font-size: 11px; margin-bottom: 2px; }
        dd { margin: 0; font-size: 14px; color: #0f172a; }
    }
}

@media (max-width: 768px) {
    .compare-table-wrap { display: none; }
    .compare-cards { display: flex; }
    .vehicle-pickers { flex-direction: column; }
}
</style>
