<template>
    <teleport to="body">
        <div v-if="visible" class="modal-backdrop" @click.self="handleClose">
            <div class="modal">
                <header class="modal__header">
                    <div>
                        <p class="modal__eyebrow">车辆详情</p>
                        <h2>{{ vehicleTitle }}</h2>
                    </div>
                    <button class="close-btn" type="button" @click="handleClose">×</button>
                </header>

                <div class="modal__content">
                    <section v-if="loading" class="modal__state">
                        正在加载车辆详情...
                    </section>

                    <section v-else-if="!vehicle" class="modal__state">
                        暂无车辆详情数据
                    </section>

                    <section v-else class="modal__body">
                        <div class="image-section">
                            <div v-if="hasImages" class="image-section__viewer">
                                <ImageCarousel :images="images" />
                            </div>
                            <p v-else class="image-section__empty">
                                暂无图片
                            </p>
                        </div>

                        <div class="info-section">
                            <h3>车辆信息</h3>
                            <div class="info-grid">
                                <div v-for="field in VEHICLE_INFO_FIELDS" :key="field.key" class="info-item">
                                    <span>{{ field.label }}</span>
                                    <strong>{{ formatVehicleField(field) }}</strong>
                                </div>
                            </div>
                        </div>

                        <div class="info-section">
                            <h3>车型配置</h3>
                            <div class="info-grid">
                                <div v-for="field in filteredConfigFields" :key="field.key" class="info-item">
                                    <span>{{ field.label }}</span>
                                    <strong>{{ formatConfigField(field) }}</strong>
                                </div>
                            </div>
                        </div>

                        <p class="remark" v-if="vehicle?.remark">
                            {{ vehicle.remark }}
                        </p>
                    </section>
                </div>
            </div>
        </div>
    </teleport>
</template>

<script setup>
import { computed, watch, onBeforeUnmount } from 'vue';
import ImageCarousel from './ImageCarousel.vue';
import { CONFIG_INFO_FIELDS, VEHICLE_INFO_FIELDS } from '@/utils/constants';
import { formatBoolean, formatDate } from '@/utils/formatters';

const props = defineProps({
    visible: Boolean,
    detail: {
        type: Object,
        default: () => null
    },
    loading: Boolean
});

const filteredConfigFields = computed(() => {
    const fuelTypeValue = formatConfigField({ key: 'fuelType' }) || '';
    return CONFIG_INFO_FIELDS.filter(field => {
        if (field.key === 'motor') {
            return fuelTypeValue.includes('电');
        }
        return true;
    });
});

const emit = defineEmits(['close']);

const BODY_LOCK_CLASS = 'vehicle-detail-modal-open';
const toggleBodyScroll = (shouldLock) => {
    if (typeof document === 'undefined') return;
    document.body.classList[shouldLock ? 'add' : 'remove'](BODY_LOCK_CLASS);
};

watch(
    () => props.visible,
    (val) => toggleBodyScroll(val),
    { immediate: true }
);

onBeforeUnmount(() => toggleBodyScroll(false));

const vehicle = computed(() => props.detail?.vehicle || null);
const config = computed(() => props.detail?.vehicleConfig || {});
const images = computed(() => props.detail?.images || []);
const hasImages = computed(() => (images.value?.length || 0) > 0);

const vehicleTitle = computed(
    () =>
        vehicle.value?.plateNumber ||
        vehicle.value?.model?.name ||
        vehicle.value?.company?.name ||
        '车辆详情'
);

const toSnakeCase = (value = '') =>
    value
        .replace(/([A-Z])/g, (_, char) => `_${char.toLowerCase()}`)
        .replace(/^_/, '');

const getValueByPath = (source, path) => {
    if (!source || !path) return undefined;
    return path.split('.').reduce((curr, segment) => {
        if (curr === undefined || curr === null) return undefined;
        if (curr[segment] !== undefined) return curr[segment];
        const snake = toSnakeCase(segment);
        return snake !== segment ? curr[snake] : undefined;
    }, source);
};

const getValueByPaths = (source, paths) => {
    const candidates = Array.isArray(paths) ? paths : [paths];
    for (const candidate of candidates) {
        const value = getValueByPath(source, candidate);
        if (value !== undefined && value !== null) {
            return value;
        }
    }
    if (source?.remark) {
        if (candidates.includes('model.brand.name') || candidates.includes('brandName')) {
            const brandMatch = source.remark.match(/品牌：([^\n]+)/);
            if (brandMatch) return brandMatch[1];
        }
        if (candidates.includes('model.name') || candidates.includes('modelName')) {
            const modelMatch = source.remark.match(/车型：([^\n]+)/);
            if (modelMatch) return modelMatch[1];
        }
    }
    return undefined;
};

const extractVehicleField = (key) => {
    if (!vehicle.value) return undefined;
    switch (key) {
        case 'brandName':
            return getValueByPaths(vehicle.value, [
                'model.brand.name',
                'model.brandName',
                'brand.name',
                'brandName'
            ]);
        case 'modelName':
            return getValueByPaths(vehicle.value, ['model.name', 'modelName']);
        case 'companyName':
            return getValueByPaths(vehicle.value, ['company.name', 'companyName']);
        case 'regionName':
            return getValueByPaths(vehicle.value, ['region.name', 'regionName']);
        case 'factoryDate':
            return getValueByPaths(vehicle.value, ['factoryDate', 'factory_date']);
        case 'launchDate':
            return getValueByPaths(vehicle.value, ['launchDate', 'launch_date']);
        case 'airConditioned':
            return getValueByPaths(vehicle.value, ['airConditioned', 'air_conditioned']);
        case 'source':
            return getValueByPaths(vehicle.value, ['source']);
        case 'plateNumber':
            return getValueByPaths(vehicle.value, ['plateNumber']);
        case 'customNumber':
            return getValueByPaths(vehicle.value, ['customNumber']);
        default:
            return getValueByPaths(vehicle.value, [key]);
    }
};

const formatVehicleField = (field) => {
    const value = extractVehicleField(field.key);
    if (field.type === 'date') {
        return value ? formatDate(value) : '—';
    }
    if (field.type === 'boolean') {
        return value === undefined || value === null
            ? '未知'
            : formatBoolean(value, '有', '无', '未知');
    }
    return value ?? '—';
};

const extractConfigField = (key) => {
    if (!config.value) return undefined;
    switch (key) {
        case 'brandName':
            return getValueByPaths(config.value, ['brand.name', 'brandName']) ||
                getValueByPaths(vehicle.value, ['remark']) && getValueByPaths(vehicle.value, ['model.brand.name', 'brandName']);
        case 'modelName':
            return getValueByPaths(config.value, ['model.name', 'modelName']) ||
                getValueByPaths(vehicle.value, ['model.name', 'modelName']);
        case 'motor':
            return getValueByPaths(config.value, ['motor', 'motorName', 'motorModel']) ||
                getValueByPaths(config.value, ['engine']);
        case 'axle':
            return getValueByPaths(config.value, ['axle', 'axleType', 'axleModel']);
        case 'fuelType':
            return getValueByPaths(config.value, ['fuelType']);
        case 'stepType':
            return getValueByPaths(config.value, ['stepType']);
        case 'suspension':
            return getValueByPaths(config.value, ['suspension']);
        default:
            return getValueByPaths(config.value, [key]);
    }
};

const formatConfigField = (field) => {
    const value = extractConfigField(field.key);
    if (field.type === 'boolean') {
        return value === undefined || value === null
            ? '—'
            : formatBoolean(value, '有', '无', '—');
    }
    if (field.key === 'fuelType' && value) {
        const fuelMap = {
            gasoline: '汽油',
            diesel: '柴油',
            electric: '电动',
            hybrid: '混动',
            gas: '燃气'
        };
        return fuelMap[value] || value;
    }
    return value ?? '—';
};

const handleClose = () => emit('close');
</script>

<style scoped lang="scss">
:global(body.vehicle-detail-modal-open) {
    overflow: hidden;
    margin: 0;
}

.modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.8);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    z-index: 100;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding: 24px;
    overflow-y: hidden;
}

.modal {
    width: min(95%, 880px);
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 28px;
    padding: 24px;
    box-shadow: 0 30px 60px rgba(15, 23, 42, 0.25);
    margin-top: 40px;
    margin-bottom: 40px;
    display: flex;
    flex-direction: column;
    max-height: calc(100vh - 80px);
    overflow: hidden;
}

.modal__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 12px;
    margin-bottom: 16px;
    flex-shrink: 0;
}

.modal__eyebrow {
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.2em;
    font-size: 0.75rem;
}

.close-btn {
    border: none;
    background: #f1f5f9;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    font-size: 1.4rem;
    cursor: pointer;
    flex-shrink: 0;
}

.modal__content {
    flex: 1;
    overflow-y: auto;
    padding-right: 0;
    -ms-overflow-style: none;
    scrollbar-width: none;
}

.modal__content::-webkit-scrollbar {
    display: none;
}

.modal__body {
    display: flex;
    flex-direction: column;
    gap: 24px;
    padding-bottom: 8px;
}

.image-section__viewer {
    width: 100%;
    background: #0f172a;
    border-radius: 22px;
    padding: 16px;
    min-height: clamp(220px, 35vh, 420px);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
}

.image-section__viewer :deep(.image-carousel__slide),
.image-section__viewer :deep(.carousel-slide) {
    display: flex;
    align-items: center;
    justify-content: center;
}

.image-section__viewer :deep(img) {
    max-width: 100%;
    max-height: clamp(220px, 35vh, 420px);
    width: auto;
    height: auto;
    object-fit: contain;
    border-radius: 18px;
    box-shadow: 0 18px 48px rgba(15, 23, 42, 0.45);
}

.image-section__empty {
    padding: 64px 0;
    text-align: center;
    color: #94a3b8;
    border: 1px dashed #dbe3f3;
    border-radius: 20px;
}

.info-section h3 {
    margin-bottom: 10px;
    color: #0f172a;
    font-size: 1rem;
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
}

.info-item {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    min-height: 72px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.info-item span {
    display: block;
    font-size: 0.8rem;
    color: #94a3b8;
}

.info-item strong {
    display: block;
    margin-top: 6px;
    font-size: 0.95rem;
    word-break: break-word;
}

.modal__state {
    padding: 60px 0;
    text-align: center;
    color: #475569;
}

.remark {
    border: 1px dashed #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    color: #475569;
    white-space: pre-line;
    font-size: 0.9rem;
    line-height: 1.5;
}
</style>