<template>
    <el-card class="gallery-card" shadow="hover">
        <div class="gallery-card__media">
            <img :src="cover" alt="" />
        </div>
        <div class="gallery-card__info">
            <h4>{{ title }}</h4>
            <p>{{ subtitle }}</p>
            <slot />
        </div>
    </el-card>
</template>

<script setup>
const props = defineProps({
    cover: { type: String, default: '' },
    title: { type: String, default: '' },
    subtitle: { type: String, default: '' }
});
</script>

<style scoped>
.gallery-card {
    display: flex;
    gap: 16px;
}

.gallery-card__media img {
    width: 160px;
    height: 120px;
    object-fit: cover;
    border-radius: 8px;
}

.gallery-card__info {
    flex: 1;
}
</style>