<template>
    <el-card class="vehicle-card" shadow="never">
        <img :src="cover" alt="" class="vehicle-img" loading="lazy" decoding="async" />
        <div class="vehicle-body">
            <div class="vehicle-title">{{ vehicle.modelName }}（{{ vehicle.plateNumber || '未登记' }}）</div>
            <div class="vehicle-meta">
                <span>{{ vehicle.companyName }}</span>
                <span>{{ vehicle.regionName }}</span>
            </div>
            <div class="vehicle-extra">
                <slot />
            </div>
        </div>
    </el-card>
</template>

<script setup>
defineProps({
    vehicle: { type: Object, required: true },
    cover: { type: String, default: '' }
});
</script>

<style scoped>
.vehicle-card {
    overflow: hidden;
    width: 100%;
    min-width: 0;
    box-sizing: border-box;
}

.vehicle-card :deep(.el-card__body) {
    min-width: 0;
}

.vehicle-img {
    display: block;
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 10;
    object-fit: cover;
    max-width: 100%;
}

.vehicle-body {
    padding-top: 8px;
    min-width: 0;
}

.vehicle-title {
    font-weight: 600;
    line-height: 1.45;
    word-break: break-word;
    overflow-wrap: anywhere;
}

.vehicle-meta {
    color: #909399;
    font-size: 12px;
    display: flex;
    gap: 4px 10px;
    flex-wrap: wrap;
}

.vehicle-meta span {
    min-width: 0;
    overflow-wrap: anywhere;
    word-break: break-word;
}

@media (max-width: 417px) {
    .vehicle-card {
        border-radius: 12px;
    }
}
</style>
