<template>
    <div class="vehicle-card" @click="handleOpen">
        <div class="card-cover">
            <button v-if="hasPrev" class="nav-btn nav-btn--prev" type="button" @click.stop="prev">‹</button>
            <img :src="coverUrl" :alt="vehicle?.plateNumber || '车辆图片'" loading="lazy" decoding="async" />
            <button v-if="hasNext" class="nav-btn nav-btn--next" type="button" @click.stop="next">›</button>
            <span v-if="variants?.length > 1" class="badge">{{ variants.length }}</span>
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
import placeholderBus from '@/assets/images/placeholder-bus.png';
import { formatYearMonth } from '@/utils/formatters';

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
    }
});

const emit = defineEmits(['view-detail']);

const currentIndex = ref(0);

watch(
    () => props.variants,
    () => {
        currentIndex.value = 0;
    }
);

const currentVariant = computed(() => props.variants?.[currentIndex.value] || { vehicle: props.vehicle, images: props.images });
const vehicle = computed(() => currentVariant.value.vehicle || props.vehicle);
const variantImages = computed(() => currentVariant.value.images || props.images || []);

const coverUrl = computed(() => {
    const img = variantImages.value?.[0];
    return img?.thumbnailUrl || img?.url || placeholderBus;
});

const hasPrev = computed(() => props.variants && props.variants.length > 1 && currentIndex.value > 0);
const hasNext = computed(() => props.variants && props.variants.length > 1 && currentIndex.value < props.variants.length - 1);

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
