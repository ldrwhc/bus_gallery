<template>
    <div class="carousel">
        <div class="carousel__main">
            <button class="nav-btn nav-btn--prev" type="button" :disabled="!hasPrev" @click="prev">
                ‹
            </button>

            <div class="carousel__stage">
                <img :src="activeImageUrl" :alt="activeImageAlt" />
                <div v-if="activeImage" class="carousel__meta">
                    <router-link
                        v-if="uploaderRoute"
                        class="carousel__user-link"
                        :to="uploaderRoute"
                        @click.stop
                    >
                        由 {{ uploaderLabel }} 上传
                    </router-link>
                    <span v-else class="carousel__user-label">由 {{ uploaderLabel }} 上传</span>
                    <span class="carousel__time">上传于 {{ timestampLabel }}</span>
                </div>
            </div>

            <button class="nav-btn nav-btn--next" type="button" :disabled="!hasNext" @click="next">
                ›
            </button>
        </div>

        <div v-if="images.length > 1" class="carousel__thumbs">
            <button
                v-for="(image, index) in images"
                :key="image.id || index"
                class="thumb"
                :class="{ 'thumb--active': index === currentIndex }"
                type="button"
                @click="go(index)"
            >
                <img :src="image.thumbnailUrl || image.url" :alt="'预览' + (index + 1)" />
            </button>
        </div>
    </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { FALLBACK_IMAGE } from '@/utils/constants';
import { formatDateTime } from '@/utils/formatters';

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
const uploaderLabel = computed(() => {
    if (!activeImage.value) return '匿名用户';
    return (
        activeImage.value.uploaderDisplayName ||
        activeImage.value.uploadUser ||
        activeImage.value.uploaderUsername ||
        '匿名用户'
    );
});
const timestampLabel = computed(() => {
    if (!activeImage.value?.createTime) return '未知';
    return formatDateTime(activeImage.value.createTime);
});

const uploaderRoute = computed(() => {
    const id = activeImage.value?.uploaderId;
    if (!id) return null;
    return { name: 'UserProfile', params: { userId: id } };
});

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
        border-radius: inherit;
        overflow: hidden;
    }

    &__stage {
        position: relative;
        width: 100%;
        min-height: clamp(240px, 40vh, 480px);
        display: flex;
        align-items: center;
        justify-content: center;
        background: radial-gradient(circle at top, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.95));
        overflow: hidden;

        &::after {
            content: '';
            position: absolute;
            inset: 0;
            background: linear-gradient(135deg, rgba(15, 23, 42, 0.4), rgba(15, 23, 42, 0.2));
            pointer-events: none;
        }

        img {
            position: relative;
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
            display: block;
            z-index: 1;
        }
    }

    &__meta {
        position: absolute;
        right: 16px;
        bottom: 16px;
        padding: 10px 14px;
        border-radius: 14px;
        background: rgba(15, 23, 42, 0.78);
        color: #fff;
        font-size: 0.85rem;
        display: flex;
        flex-direction: column;
        gap: 4px;
        text-align: right;
        pointer-events: auto;
    }

    &__user-link,
    &__user-label {
        color: inherit;
        font-weight: 600;
        text-decoration: none;
    }

    &__user-link:hover {
        text-decoration: underline;
    }

    &__time {
        font-size: 0.78rem;
        opacity: 0.85;
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
