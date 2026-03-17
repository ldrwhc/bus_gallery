<template>
    <div class="model-list">
        <article v-for="model in models" :key="model.id" class="model-block">
            <header>
                <div>
                    <h3>{{ model.name }}</h3>
                    <p class="meta">
                        品牌：{{ model.brandName || '未知' }}
                    </p>
                </div>
                <button class="ghost-btn ghost-btn--sm" type="button" @click="$emit('select-model', model)">
                    查看车型
                </button>
            </header>

            <div class="company-grid">
                <div v-for="company in model.companies || []" :key="company.id" class="company-pill"
                    @click="$emit('select-company', company)">
                    <img :src="company.thumbnailUrl || placeholder" :alt="company.name" />
                    <div>
                        <p class="company-name">{{ company.name }}</p>
                        <p class="meta">{{ company.regionName || '地区未知' }}</p>
                    </div>
                </div>
            </div>
        </article>
    </div>
</template>

<script setup>
import { FALLBACK_IMAGE } from '@/utils/constants';

defineProps({
    models: {
        type: Array,
        default: () => []
    },
    placeholder: {
        type: String,
        default: FALLBACK_IMAGE
    }
});

defineEmits(['select-model', 'select-company']);
</script>

<style scoped lang="scss">
.model-block {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
    margin-bottom: 16px;

    header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;
    }
}

.company-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 10px;
}

.company-pill {
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 10px;
    display: flex;
    gap: 10px;
    cursor: pointer;
    background: #f8fafc;
    white-space: normal;
    min-width: 200px;

    &:hover {
        border-color: #0f172a;
    }

    img {
        width: 72px;
        height: 48px;
        object-fit: cover;
        border-radius: 12px;
    }

    .company-name {
        font-weight: 600;
    }
}

.meta {
    color: #94a3b8;
}

.ghost-btn {
    border: 1px solid rgba(15, 23, 42, 0.3);
    border-radius: 999px;
    padding: 6px 12px;
    background: transparent;
    cursor: pointer;

    &--sm {
        font-size: 0.85rem;
    }
}
</style>
