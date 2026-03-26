<template>
    <div class="vehicle-card" @click="handleOpen">
        <div class="card-cover">
            <button v-if="hasPrev" class="nav-btn nav-btn--prev" type="button" @click.stop="prev">‹</button>
            <img :src="coverUrl" :alt="vehicle?.plateNumber || '车辆图片'" loading="lazy" decoding="async" />
            <button v-if="hasNext" class="nav-btn nav-btn--next" type="button" @click.stop="next">›</button>
            <span class="watermark-tag">BUS GALLERY</span>
            <span v-if="variantTotal > 1" class="badge">{{ variantTotal }}</span>
        </div>

        <div class="card-body">
            <h3 class="plate-number">
                {{ vehicle?.plateNumber || '未知车牌' }}
            </h3>

            <ul class="meta-list">
                <li>
                    <span class="label">车型：</span>
                    <span>{{ vehicle?.model?.name || '未知车型' }}</span>
                </li>
                <li>
                    <span class="label">公司：</span>
                    <span>{{ vehicle?.company?.name || '未知公司' }}</span>
                </li>
                <li>
                    <span class="label">地区：</span>
                    <span>{{ vehicle?.region?.name || resolveRegionName }}</span>
                </li>
                <li>
                    <span class="label">上线时间：</span>
                    <span>{{ formattedLaunchDate }}</span>
                </li>
            </ul>

        </div>
    </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { fetchVehiclesByPlate } from '@/api/vehicles';
import placeholderBus from '@/assets/images/placeholder-bus.png';
import { formatYearMonth } from '@/utils/formatters';

const plateVariantCache = new Map();
const plateVariantRequestMap = new Map();

const props = defineProps({
    vehicle: {
        type: Object,
        required: true
    },
    images: {
        type: Array,
        default: () => []
    },
    variants: {
        type: Array,
        default: () => []
    },
    variantCount: {
        type: Number,
        default: null
    }
});

const emit = defineEmits(['view-detail']);

const currentIndex = ref(0);
const remoteVariants = ref([]);

const normalizePlate = (plate) => String(plate || '').replace(/\s+/g, '').trim();

const normalizeVariantRecord = (item, fallbackIndex = 0) => {
    if (!item) {
        return null;
    }
    const vehicle = item.vehicle || item;
    if (!vehicle) {
        return null;
    }
    const images = Array.isArray(item.images) ? item.images : [];
    return {
        vehicle,
        vehicleConfig: item.vehicleConfig || item.config || null,
        images,
        __fallbackIndex: fallbackIndex
    };
};

const dedupeVariants = (source = []) => {
    const result = [];
    const seen = new Set();
    source.forEach((variant, index) => {
        const normalized = normalizeVariantRecord(variant, index);
        if (!normalized?.vehicle) {
            return;
        }
        const vehicleId = normalized.vehicle.id;
        const plate = normalizePlate(normalized.vehicle.plateNumber);
        const key = vehicleId != null ? `vid:${vehicleId}` : `plate:${plate}:idx:${index}`;
        if (seen.has(key)) {
            return;
        }
        seen.add(key);
        result.push(normalized);
    });
    return result;
};

const loadPlateVariants = async (plateNumber) => {
    const normalizedPlate = normalizePlate(plateNumber);
    if (!normalizedPlate) {
        remoteVariants.value = [];
        return;
    }

    const cached = plateVariantCache.get(normalizedPlate);
    if (cached) {
        remoteVariants.value = cached;
        return;
    }

    let pending = plateVariantRequestMap.get(normalizedPlate);
    if (!pending) {
        pending = fetchVehiclesByPlate(normalizedPlate)
            .then((resp) => {
                const list = Array.isArray(resp?.variants) ? resp.variants : [];
                const normalized = dedupeVariants(list);
                plateVariantCache.set(normalizedPlate, normalized);
                return normalized;
            })
            .catch(() => [])
            .finally(() => {
                plateVariantRequestMap.delete(normalizedPlate);
            });
        plateVariantRequestMap.set(normalizedPlate, pending);
    }

    const latest = await pending;
    remoteVariants.value = latest;
};

watch(
    () => props.vehicle?.plateNumber,
    (plate) => {
        loadPlateVariants(plate);
    },
    { immediate: true }
);

watch(
    () => [props.variants, props.variantCount, props.vehicle?.id],
    () => {
        currentIndex.value = 0;
    }
);

const localVariants = computed(() => {
    const source = Array.isArray(props.variants) && props.variants.length
        ? props.variants
        : [{ vehicle: props.vehicle, images: props.images }];
    return dedupeVariants(source);
});

const normalizedVariants = computed(() => dedupeVariants([
    ...localVariants.value,
    ...remoteVariants.value
]));

watch(
    () => normalizedVariants.value.length,
    (length) => {
        if (currentIndex.value >= length) {
            currentIndex.value = 0;
        }
    }
);

const variantTotal = computed(() => {
    const count = Number(props.variantCount);
    const localCount = Number.isFinite(count) && count > 0 ? count : 0;
    return Math.max(localCount, normalizedVariants.value.length);
});

const currentVariant = computed(() => normalizedVariants.value?.[currentIndex.value] || { vehicle: props.vehicle, images: props.images });
const vehicle = computed(() => currentVariant.value.vehicle || props.vehicle);
const variantImages = computed(() => currentVariant.value.images || props.images || []);

const coverUrl = computed(() => {
    const img = variantImages.value?.[0];
    return img?.thumbnailUrl || placeholderBus;
});

const hasPrev = computed(() => normalizedVariants.value.length > 1 && currentIndex.value > 0);
const hasNext = computed(() => normalizedVariants.value.length > 1 && currentIndex.value < normalizedVariants.value.length - 1);

const prev = () => {
    if (hasPrev.value) currentIndex.value -= 1;
};

const next = () => {
    if (hasNext.value) currentIndex.value += 1;
};

const handleOpen = () => {
    emit('view-detail', vehicle.value?.id || props.vehicle?.id);
};

const formattedLaunchDate = computed(() =>
    formatYearMonth(vehicle.value?.launchDate)
);

const resolveRegionName = computed(() => {
    if (vehicle.value?.remark) {
        const line = vehicle.value.remark
            .split('\n')
            .find((item) => item.startsWith('地区：'));
        if (line) {
            return line.replace('地区：', '').trim();
        }
    }
    return '未知地区';
});
</script>

<style scoped lang="scss">
.vehicle-card {
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 12px 30px rgba(15, 23, 42, 0.12);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    cursor: pointer;
    transition: transform 0.15s ease;
    width: 100%;
    min-width: 0;

    &:hover {
        transform: translateY(-2px);
    }
}

.card-cover {
    width: 100%;
    aspect-ratio: 16 / 10;
    background: #f3f4f6;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    position: relative;

    img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
    }

    .nav-btn {
        position: absolute;
        top: 50%;
        transform: translateY(-50%);
        border: none;
        background: rgba(15, 23, 42, 0.7);
        color: #fff;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        cursor: pointer;
        font-size: 1.2rem;
        display: grid;
        place-items: center;
        z-index: 2;

        &--prev {
            left: 8px;
        }
        &--next {
            right: 8px;
        }
    }

    .badge {
        position: absolute;
        left: 12px;
        top: 12px;
        background: rgba(15, 23, 42, 0.8);
        color: #fff;
        padding: 4px 10px;
        border-radius: 999px;
        font-size: 12px;
        z-index: 2;
    }

    .watermark-tag {
        position: absolute;
        right: 10px;
        bottom: 10px;
        z-index: 2;
        font-size: 0.62rem;
        font-weight: 700;
        letter-spacing: 0.08em;
        color: rgba(255, 255, 255, 0.64);
        text-shadow: 0 1px 6px rgba(15, 23, 42, 0.6);
        pointer-events: none;
    }
}

.card-body {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;

    .plate-number {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #111827;
        overflow-wrap: anywhere;
    }

    .meta-list {
        list-style: none;
        padding: 0;
        margin: 0;
        font-size: 14px;
        color: #4b5563;

        li {
            display: grid;
            grid-template-columns: 76px minmax(0, 1fr);
            column-gap: 4px;
            line-height: 1.6;
            min-width: 0;

            .label {
                color: #9ca3af;
                width: auto;
            }

            span:last-child {
                min-width: 0;
                overflow-wrap: anywhere;
                word-break: break-word;
            }
        }
    }

}

@media (max-width: 640px) {
    .card-body {
        padding: 12px;
    }

    .card-cover .nav-btn {
        width: 28px;
        height: 28px;
        font-size: 1rem;
    }
}
</style>
