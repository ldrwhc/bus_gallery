<template>
    <div class="region-list">
        <article v-for="region in regions" :key="region.id" class="region-card">
            <header>
                <div>
                    <h3>{{ region.name }}</h3>
                    <p class="meta">
                        公司 {{ region.companies?.length || 0 }} 家
                    </p>
                </div>
                <button class="ghost-btn ghost-btn--sm" type="button" @click="$emit('select-region', region)">
                    查看地区
                </button>
            </header>

            <div class="companies">
                <article v-for="company in region.companies || []" :key="company.id" class="company"
                    @click="$emit('select-company', company)">
                    <img :src="company.thumbnailUrl || placeholder" :alt="company.name" />
                    <div>
                        <p class="company-name">{{ company.name }}</p>
                        <p class="meta">
                            车型 {{ company.modelsCount ?? '—' }}
                        </p>
                    </div>
                </article>
            </div>
        </article>
    </div>
</template>

<script setup>
import { FALLBACK_IMAGE } from '@/utils/constants';

defineProps({
    regions: {
        type: Array,
        default: () => []
    },
    placeholder: {
        type: String,
        default: FALLBACK_IMAGE
    }
});

defineEmits(['select-region', 'select-company']);
</script>

<style scoped lang="scss">
.region-card {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);

    header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;
    }
}

.meta {
    color: #94a3b8;
}

.companies {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px;
}

.company {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 10px;
    display: flex;
    gap: 10px;
    cursor: pointer;
    transition: border-color 0.2s;

    &:hover {
        border-color: #2563eb;
    }

    img {
        width: 64px;
        height: 48px;
        object-fit: cover;
        border-radius: 12px;
    }

    .company-name {
        font-weight: 600;
    }
}

.ghost-btn {
    border: 1px solid #cbd5f5;
    background: transparent;
    padding: 6px 12px;
    border-radius: 999px;
    cursor: pointer;

    &--sm {
        font-size: 0.85rem;
    }
}
</style>