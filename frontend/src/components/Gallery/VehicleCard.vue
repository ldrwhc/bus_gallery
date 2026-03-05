<template>
    <div class="vehicle-card" @click="$emit('view-detail')">
        <div class="card-cover">
            <img :src="coverUrl" :alt="vehicle?.plateNumber || '车辆图片'" />
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
                    <span class="label">所属公司：</span>
                    <span>{{ vehicle?.company?.name || '未知公司' }}</span>
                </li>
                <li>
                    <span class="label">所属地区：</span>
                    <span>{{ vehicle?.region?.name || resolveRegionName }}</span>
                </li>
                <li>
                    <span class="label">投运时间：</span>
                    <span>{{ vehicle?.launchDate || '—' }}</span>
                </li>
            </ul>

            <p class="remark" v-if="vehicle?.remark">
                {{ vehicle.remark }}
            </p>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue';
import placeholderBus from '@/assets/images/placeholder-bus.png';

const props = defineProps({
    vehicle: {
        type: Object,
        required: true
    },
    images: {
        type: Array,
        default: () => []
    }
});

defineEmits(['view-detail']);

const coverUrl = computed(() => {
    const img = props.images?.[0];
    return img?.thumbnailUrl || img?.url || placeholderBus;
});

const resolveRegionName = computed(() => {
    if (props.vehicle?.remark) {
        const line = props.vehicle.remark
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

    &:hover {
        transform: translateY(-2px);
    }
}

.card-cover {
    width: 100%;
    height: 180px;
    background: #f3f4f6;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    img {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }
}

.card-body {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .plate-number {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #111827;
    }

    .meta-list {
        list-style: none;
        padding: 0;
        margin: 0;
        font-size: 14px;
        color: #4b5563;

        li {
            display: flex;
            line-height: 1.6;

            .label {
                color: #9ca3af;
                width: 80px;
            }
        }
    }

    .remark {
        margin: 0;
        font-size: 13px;
        color: #6b7280;
        line-height: 1.6;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
    }
}
</style>