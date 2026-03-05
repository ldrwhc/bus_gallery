<template>
    <div class="carousel">
        <div class="carousel__main">
            <button class="nav-btn nav-btn--prev" type="button" :disabled="!hasPrev" @click="prev">
                ‹
            </button>

            <div class="carousel__stage">
                <img :src="activeImageUrl" :alt="activeImageAlt" />
            </div>

            <button class="nav-btn nav-btn--next" type="button" :disabled="!hasNext" @click="next">
                ›
            </button>
        </div>

        <div v-if="images.length > 1" class="carousel__thumbs">
            <button v-for="(image, index) in images" :key="image.id || index" class="thumb"
                :class="{ 'thumb--active': index === currentIndex }" type="button" @click="go(index)">
                <img :src="image.thumbnailUrl || image.url" :alt="'预览' + (index + 1)" />
            </button>
        </div>
    </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { FALLBACK_IMAGE } from '@/utils/constants';

const props = defineProps({
    images: {
        type: Array,
        default: () => []
    }
});

const currentIndex = ref(0);

watch(
    () => props.images,
    () => {
        currentIndex.value = 0;
    }
);

const images = computed(() => props.images || []);

const activeImage = computed(() => images.value[currentIndex.value] || null);
const activeImageUrl = computed(
    () => activeImage.value?.url || activeImage.value?.thumbnailUrl || FALLBACK_IMAGE
);
const activeImageAlt = computed(() => activeImage.value?.description || '车辆图片');

const hasPrev = computed(() => currentIndex.value > 0);
const hasNext = computed(() => currentIndex.value < images.value.length - 1);

const prev = () => {
    if (hasPrev.value) currentIndex.value -= 1;
};

const next = () => {
    if (hasNext.value) currentIndex.value += 1;
};

const go = (index) => {
    currentIndex.value = index;
};
</script>

<style scoped lang="scss">
.carousel {
    display: flex;
    flex-direction: column;
    gap: 12px;

    &__main {
        position: relative;
        border-radius: 18px;
        overflow: hidden;

        img {
            width: 100%;
            height: 320px;
            object-fit: cover;
            display: block;
            background: #0f172a;
        }
    }

    &__thumbs {
        display: flex;
        gap: 8px;
    }
}

.nav-btn {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    border: none;
    background: rgba(15, 23, 42, 0.6);
    color: #fff;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    cursor: pointer;
    font-size: 1.4rem;

    &:disabled {
        opacity: 0.3;
        cursor: not-allowed;
    }

    &--prev {
        left: 12px;
    }

    &--next {
        right: 12px;
    }
}

.thumb {
    border: none;
    padding: 0;
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;

    &--active {
        border-color: #2563eb;
    }

    img {
        width: 80px;
        height: 60px;
        object-fit: cover;
        display: block;
    }
}
</style>