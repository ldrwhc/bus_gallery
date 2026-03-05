<template>
    <div class="brand-list">
        <article v-for="brand in brands" :key="brand.id" class="brand-card">
            <header>
                <div>
                    <h3>{{ brand.name }}</h3>
                    <p class="meta">
                        车型 {{ brand.models?.length || 0 }}
                    </p>
                </div>
                <button class="ghost-btn ghost-btn--sm" type="button" @click="$emit('select-brand', brand)">
                    查看品牌
                </button>
            </header>

            <div class="model-grid">
                <div v-for="model in brand.models || []" :key="model.id" class="model-pill"
                    @click="$emit('select-model', model)">
                    <img :src="model.thumbnailUrl || placeholder" :alt="model.name" loading="lazy" />
                    <div>
                        <p class="model-name">{{ model.name }}</p>
                        <p class="meta">{{ model.length ? `${model.length}m` : '—' }}</p>
                    </div>
                </div>
            </div>
        </article>
    </div>
</template>

<script setup>
import { FALLBACK_IMAGE } from '@/utils/constants';

defineProps({
    brands: {
        type: Array,
        default: () => []
    },
    placeholder: {
        type: String,
        default: FALLBACK_IMAGE
    }
});

defineEmits(['select-brand', 'select-model']);
</script>

<style scoped lang="scss">
.brand-card {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 30px rgba(67, 56, 202, 0.15);
    margin-bottom: 16px;

    header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;
    }
}

.model-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px;
}

.model-pill {
    border-radius: 18px;
    border: 1px solid rgba(99, 102, 241, 0.2);
    padding: 12px;
    display: flex;
    gap: 10px;
    cursor: pointer;
    background: linear-gradient(180deg, #f8fafc, #eef2ff);

    &:hover {
        border-color: #4c1d95;
    }

    img {
        width: 64px;
        height: 64px;
        border-radius: 12px;
        object-fit: cover;
    }

    .model-name {
        font-weight: 600;
    }
}

.meta {
    color: #a5b4fc;
}

.ghost-btn {
    border: 1px solid rgba(79, 70, 229, 0.4);
    border-radius: 999px;
    padding: 6px 12px;
    background: transparent;
    cursor: pointer;

    &--sm {
        font-size: 0.85rem;
    }
}
</style>