<template>
    <teleport to="body">
        <div v-if="visible" class="modal-backdrop" @click.self="handleClose">
            <div class="modal">
                <header class="modal__header">
                    <div class="modal__headline">
                        <p class="modal__eyebrow">车辆详情</p>
                        <div class="title-row">
                            <h2>{{ vehicleTitle }}</h2>
                            <button
                                v-if="hasExif"
                                class="info-badge"
                                type="button"
                                title="查看 EXIF 信息"
                                @click="exifVisible = true"
                            >
                                i
                            </button>
                        </div>
                    </div>
                    <button class="close-btn" type="button" @click="handleClose">×</button>
                </header>

                <div class="modal__content">
                    <section v-if="loading" class="modal__state">正在加载车辆详情...</section>
                    <section v-else-if="!vehicle" class="modal__state">暂无车辆详情数据</section>

                    <section v-else class="modal__body">
                        <div class="image-section">
                            <div v-if="hasImages" class="image-section__viewer">
                                <ImageCarousel :images="images" />
                            </div>
                            <p v-else class="image-section__empty">暂无图片</p>
                        </div>

                        <div class="info-section" v-if="vehicleInfoCards.length">
                            <h3>车辆信息</h3>
                            <div class="info-grid">
                                <div v-for="field in vehicleInfoCards" :key="field.key" class="info-item">
                                    <span>{{ field.label }}</span>
                                    <strong>
                                        <router-link
                                            v-if="field.link && getVehicleLink(field)"
                                            class="info-link"
                                            :to="getVehicleLink(field)"
                                        >
                                            {{ field.value }}
                                        </router-link>
                                        <span v-else class="info-value">{{ field.value }}</span>
                                    </strong>
                                </div>
                            </div>
                        </div>

                        <div class="info-section" v-if="configInfoCards.length">
                            <h3>车型配置</h3>
                            <div class="info-grid">
                                <div v-for="field in configInfoCards" :key="field.key" class="info-item">
                                    <span>{{ field.label }}</span>
                                    <strong>
                                        <router-link
                                            v-if="field.link && getConfigLink(field)"
                                            class="info-link"
                                            :to="getConfigLink(field)"
                                        >
                                            {{ field.value }}
                                        </router-link>
                                        <span v-else class="info-value">{{ field.value }}</span>
                                    </strong>
                                </div>
                            </div>
                        </div>
                    </section>
                </div>
            </div>
        </div>
    </teleport>
    <teleport to="body">
        <transition name="fade">
            <div v-if="exifVisible" class="exif-overlay" @click.self="exifVisible = false">
                <div class="exif-modal">
                    <header class="exif-modal__header">
                        <div>
                            <p class="modal__eyebrow">EXIF</p>
                            <h3>拍摄参数</h3>
                        </div>
                        <button class="close-btn" type="button" @click="exifVisible = false">×</button>
                    </header>
                    <div v-if="exifEntries.length" class="exif-modal__body">
                        <dl>
                            <div v-for="[key, value] in exifEntries" :key="key" class="exif-row">
                                <dt>{{ key }}</dt>
                                <dd>{{ value }}</dd>
                            </div>
                        </dl>
                    </div>
                    <p v-else class="exif-empty">暂未解析到 EXIF 信息</p>
                </div>
            </div>
        </transition>
    </teleport>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue';
import ImageCarousel from './ImageCarousel.vue';
import { CONFIG_INFO_FIELDS, VEHICLE_INFO_FIELDS } from '@/utils/constants';
import { formatBoolean, formatFuelType, formatYearMonth } from '@/utils/formatters';

const props = defineProps({
    visible: Boolean,
    detail: {
        type: Object,
        default: () => null
    },
    loading: Boolean
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
    () => vehicle.value?.plateNumber || vehicle.value?.model?.name || vehicle.value?.company?.name || '车辆详情'
);

const exifSource = computed(() => {
    if (!images.value?.length) return null;
    return images.value.find((item) => item?.exif && Object.keys(item.exif).length);
});

const hasExif = computed(() => Boolean(exifSource.value));
const exifEntries = computed(() => {
    if (!exifSource.value?.exif) return [];
    return Object.entries(exifSource.value.exif);
});
const exifVisible = ref(false);

const toSnakeCase = (value = '') =>
    value
        .replace(/([A-Z])/g, (_, char) => _)
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
        const remark = source.remark;
        if (candidates.includes('model.brand.name') || candidates.includes('brandName')) {
            const brandMatch = remark.match(/品牌：([^\n]+)/);
            if (brandMatch) return brandMatch[1];
        }
        if (candidates.includes('model.name') || candidates.includes('modelName')) {
            const modelMatch = remark.match(/车型：([^\n]+)/);
            if (modelMatch) return modelMatch[1];
        }
    }
    return undefined;
};

const extractVehicleField = (key) => {
    if (!vehicle.value) return undefined;
    switch (key) {
        case 'brandName':
            return getValueByPaths(vehicle.value, ['model.brand.name', 'model.brandName', 'brand.name', 'brandName']);
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
        default:
            return getValueByPaths(vehicle.value, [key]);
    }
};

const formatVehicleField = (field, rawValue) => {
    if (field.type === 'date') {
        return rawValue ? formatYearMonth(rawValue) : '-';
    }
    if (field.type === 'boolean') {
        return formatBoolean(rawValue);
    }
    return rawValue ?? '-';
};

const rawFuelType = computed(() => {
    const fuel = config.value?.fuelType || config.value?.fuel_type || '';
    return String(fuel).toLowerCase();
});

const configFieldDefinitions = computed(() => {
    const fuel = rawFuelType.value;
    const hasElectric = fuel.includes('electric');
    const hasCombustion =
        fuel.includes('diesel') ||
        fuel.includes('gasoline') ||
        fuel.includes('oil') ||
        fuel.includes('lng') ||
        fuel.includes('cng') ||
        fuel.includes('gas');
    return CONFIG_INFO_FIELDS.filter((field) => {
        if (field.key === 'motor') {
            return hasElectric;
        }
        if (field.key === 'engine') {
            return hasCombustion;
        }
        return true;
    });
});

const extractConfigField = (key) => {
    if (!config.value) return undefined;
    switch (key) {
        case 'brandName':
            return (
                getValueByPaths(config.value, ['brand.name', 'brandName']) ||
                getValueByPaths(vehicle.value, ['model.brand.name', 'brandName'])
            );
        case 'modelName':
            return (
                getValueByPaths(config.value, ['model.name', 'modelName']) ||
                getValueByPaths(vehicle.value, ['model.name', 'modelName'])
            );
        default:
            return getValueByPaths(config.value, [key]);
    }
};

const formatConfigField = (field, rawValue) => {
    if (field.type === 'boolean') {
        return formatBoolean(rawValue);
    }
    if (field.key === 'fuelType') {
        return formatFuelType(rawValue);
    }
    if (field.type === 'date') {
        return rawValue ? formatYearMonth(rawValue) : '-';
    }
    return rawValue ?? '-';
};

const hasDisplayValue = (value, type) => {
    if (type === 'boolean') {
        return value !== null && value !== undefined;
    }
    if (value === null || value === undefined) return false;
    if (typeof value === 'string') {
        return value.trim().length > 0;
    }
    return true;
};

const vehicleInfoCards = computed(() =>
    VEHICLE_INFO_FIELDS.map((field) => {
        const raw = extractVehicleField(field.key);
        return {
            ...field,
            raw,
            value: formatVehicleField(field, raw)
        };
    }).filter((field) => hasDisplayValue(field.raw, field.type))
);

const configInfoCards = computed(() =>
    configFieldDefinitions.value
        .map((field) => {
            const raw = extractConfigField(field.key);
            return {
                ...field,
                raw,
                value: formatConfigField(field, raw)
            };
        })
        .filter((field) => hasDisplayValue(field.raw, field.type))
);

const getVehicleLink = (field) => {
    if (!field.link) return null;
    switch (field.link) {
        case 'region': {
            const id = vehicle.value?.region?.id;
            return id ? { name: 'RegionCatalog', params: { regionId: id } } : null;
        }
        case 'company': {
            const id = vehicle.value?.company?.id;
            return id ? { name: 'CompanyCatalog', params: { companyId: id } } : null;
        }
        default:
            return null;
    }
};

const getConfigLink = (field) => {
    if (!field.link) return null;
    switch (field.link) {
        case 'brand': {
            const id = config.value?.brandId || vehicle.value?.model?.brandId;
            return id ? { name: 'BrandCatalog', params: { brandId: id } } : null;
        }
        case 'model': {
            const id = config.value?.modelId || vehicle.value?.model?.id;
            return id ? { name: 'ModelCatalog', params: { modelId: id } } : null;
        }
        default:
            return null;
    }
};

const handleClose = () => {
    exifVisible.value = false;
    emit('close');
};
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
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    min-height: 100vh;
    overflow: hidden;
}

.modal {
    width: min(95%, 900px);
    background: rgba(255, 255, 255, 0.98);
    border-radius: 28px;
    padding: 24px;
    box-shadow: 0 30px 60px rgba(15, 23, 42, 0.25);
    display: flex;
    flex-direction: column;
    max-height: min(900px, calc(100vh - 48px));
    overflow-y: auto;
}

.modal__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 12px;
    margin-bottom: 16px;
}

.modal__headline {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.title-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.modal__eyebrow {
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.2em;
    font-size: 0.75rem;
    margin-bottom: 4px;
}

.info-badge {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: 1px solid rgba(148, 163, 184, 0.8);
    background: #fff;
    color: #64748b;
    font-weight: 600;
    font-size: 0.85rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s, border-color 0.2s;

    &:hover {
        background: rgba(148, 163, 184, 0.12);
        border-color: rgba(37, 99, 235, 0.5);
        color: #2563eb;
    }
}

.close-btn {
    border: none;
    background: #f1f5f9;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    font-size: 1.4rem;
    cursor: pointer;
}

.modal__content {
    flex: 1;
    overflow-y: auto;
    padding-right: 4px;
    scrollbar-width: none;
    -ms-overflow-style: none;
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
    background: radial-gradient(circle at top, rgba(15, 23, 42, 0.9), rgba(15, 23, 42, 0.98));
    border-radius: 24px;
    padding: clamp(12px, 2vw, 20px);
    min-height: clamp(240px, 40vh, 480px);
    box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.15);
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
    grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
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
    color: #0f172a;
}

.info-link,
.info-link:visited {
    color: #2563eb;
    text-decoration: none;
}

.info-value {
    color: #0f172a;
}

.modal__state {
    padding: 60px 0;
    text-align: center;
    color: #475569;
}

.exif-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.7);
    backdrop-filter: blur(6px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    z-index: 120;
}

.exif-modal {
    width: min(400px, 100%);
    background: #fff;
    border-radius: 24px;
    padding: 20px;
    box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.exif-modal__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
}

.exif-modal__body {
    max-height: 60vh;
    overflow-y: auto;
}

.exif-row {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 8px 0;
    border-bottom: 1px solid #f1f5f9;

    dt {
        font-weight: 600;
        color: #475569;
    }

    dd {
        margin: 0;
        color: #1f2937;
        text-align: right;
    }
}

.exif-empty {
    text-align: center;
    color: #94a3b8;
    margin: 0;
}
</style>
