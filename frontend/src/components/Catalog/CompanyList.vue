<template>
    <div class="company-list">
        <article v-for="company in companies" :key="company.id" class="company-block">
            <header>
                <div>
                    <h3>{{ company.name }}</h3>
                    <p class="meta">
                        地区：{{ company.regionName || '未知' }}
                    </p>
                </div>
                <button class="ghost-btn ghost-btn--sm" type="button" @click="$emit('select-company', company)">
                    某公司详情
                </button>
            </header>

            <div class="model-grid">
                <div v-for="model in company.models || []" :key="model.id" class="model-card"
                    @click="$emit('select-model', model)">
                    <img :src="model.thumbnailUrl || placeholder" :alt="model.name" loading="lazy" />
                    <p class="model-name">{{ model.name }}</p>
                    <p class="meta">{{ model.brandName || '品牌未知' }}</p>
                </div>
            </div>
        </article>
    </div>
</template>

<script setup>
import { FALLBACK_IMAGE } from '@/utils/constants';

defineProps({
    companies: {
        type: Array,
        default: () => []
    },
    placeholder: {
        type: String,
        default: FALLBACK_IMAGE
    }
});

defineEmits(['select-company', 'select-model']);
</script>

<style scoped lang="scss">
.company-block {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
    margin-bottom: 16px;

    header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;
    }
}

.model-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;
}

.model-card {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    text-align: center;
    cursor: pointer;

    &:hover {
        border-color: #2563eb;
    }

    img {
        width: 100%;
        height: 110px;
        object-fit: cover;
        border-radius: 12px;
        margin-bottom: 8px;
    }

    .model-name {
        font-weight: 600;
    }
}

.meta {
    color: #94a3b8;
}

.ghost-btn {
    border-radius: 999px;
    padding: 6px 12px;
    border: 1px solid rgba(37, 99, 235, 0.35);
    background: transparent;
    cursor: pointer;

    &--sm {
        font-size: 0.85rem;
    }
}
</style>