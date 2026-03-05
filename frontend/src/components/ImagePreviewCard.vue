<template>
    <el-card class="preview-card" shadow="hover">
        <img :src="image.thumbnailUrl || image.url" alt="" />
        <div class="meta">
            <div>ID: {{ image.id }}</div>
            <div v-if="image.vehicleId">车辆: {{ image.vehicleId }}</div>
            <div v-if="image.createdAt">上传: {{ formatDate(image.createdAt) }}</div>
        </div>
    </el-card>
</template>

<script setup>
import { formatDate } from '@/utils/format';

const props = defineProps({
    image: { type: Object, required: true }
});
</script>

<style scoped>
.preview-card img {
    width: 100%;
    height: 160px;
    object-fit: cover;
}

.meta {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
}
</style>