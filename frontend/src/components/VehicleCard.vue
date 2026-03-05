<template>
    <el-card class="vehicle-card" shadow="never">
        <img :src="cover" alt="" class="vehicle-img" />
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
}

.vehicle-img {
    width: 100%;
    height: 140px;
    object-fit: cover;
}

.vehicle-body {
    padding-top: 8px;
}

.vehicle-title {
    font-weight: 600;
}

.vehicle-meta {
    color: #909399;
    font-size: 12px;
    display: flex;
    justify-content: space-between;
}
</style>