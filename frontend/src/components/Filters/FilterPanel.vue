<template>
    <div class="filter-panel">
        <div class="filter-panel__row">
            <label class="filter">
                <span>地区</span>
                <select :value="localFilters.regionId ?? ''" @change="handleSelectChange('regionId', $event)">
                    <option value="">全部地区</option>
                    <option v-for="option in regions" :key="option.value" :value="option.value">
                        {{ option.label }}
                    </option>
                </select>
            </label>

            <label class="filter">
                <span>公司</span>
                <select :value="localFilters.companyId ?? ''" @change="handleSelectChange('companyId', $event)">
                    <option value="">全部公司</option>
                    <option v-for="option in companies" :key="option.value" :value="option.value">
                        {{ option.label }}
                    </option>
                </select>
            </label>

            <label class="filter">
                <span>品牌</span>
                <select :value="localFilters.brandId ?? ''" @change="handleSelectChange('brandId', $event)">
                    <option value="">全部品牌</option>
                    <option v-for="option in brands" :key="option.value" :value="option.value">
                        {{ option.label }}
                    </option>
                </select>
            </label>

            <label class="filter">
                <span>车型</span>
                <select :value="localFilters.modelId ?? ''" @change="handleSelectChange('modelId', $event)">
                    <option value="">全部车型</option>
                    <option v-for="option in models" :key="option.value" :value="option.value">
                        {{ option.label }}
                    </option>
                </select>
            </label>
        </div>

        <div class="filter-panel__footer">
            <div>
                <p class="filter-summary">
                    当前筛选：{{ summaryText || '全部车辆' }}
                </p>
            </div>
            <button class="ghost-btn" type="button" @click="handleReset">
                重置
            </button>
        </div>
    </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue';

const props = defineProps({
    filters: {
        type: Object,
        default: () => ({})
    },
    regions: {
        type: Array,
        default: () => []
    },
    companies: {
        type: Array,
        default: () => []
    },
    brands: {
        type: Array,
        default: () => []
    },
    models: {
        type: Array,
        default: () => []
    }
});

const emit = defineEmits(['change', 'reset']);

const defaultFilters = {
    regionId: null,
    companyId: null,
    brandId: null,
    modelId: null,
    keyword: ''
};

const localFilters = reactive({ ...defaultFilters });

watch(
    () => props.filters,
    (value) => {
        Object.assign(localFilters, defaultFilters, value || {});
    },
    { immediate: true, deep: true }
);

const normalizeValue = (value) => {
    if (value === '' || value === null || value === undefined) return null;
    const numeric = Number(value);
    return Number.isNaN(numeric) ? value : numeric;
};

const handleSelectChange = (key, event) => {
    const value = normalizeValue(event.target.value);
    localFilters[key] = value;
    emit('change', { [key]: value });
};

const handleReset = () => {
    Object.assign(localFilters, defaultFilters);
    emit('reset');
};

const getLabel = (list, value) => {
    const match = list.find((item) => item.value === value);
    return match ? match.label : '';
};

const summaryText = computed(() => {
    const segments = [];

    if (localFilters.regionId) {
        segments.push(getLabel(props.regions, localFilters.regionId));
    }
    if (localFilters.companyId) {
        segments.push(getLabel(props.companies, localFilters.companyId));
    }
    if (localFilters.brandId) {
        segments.push(getLabel(props.brands, localFilters.brandId));
    }
    if (localFilters.modelId) {
        segments.push(getLabel(props.models, localFilters.modelId));
    }

    return segments.join(' / ');
});
</script>

<style scoped lang="scss">
.filter-panel {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);

    &__row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 16px;
    }

    &__footer {
        margin-top: 18px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
}

.filter {
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 0.9rem;
    color: #475569;

    select {
        border-radius: 14px;
        border: 1px solid #e2e8f0;
        padding: 10px 12px;
        background: #f8fafc;
        font-weight: 500;
        color: #0f172a;

        &:focus {
            outline: 2px solid rgba(37, 99, 235, 0.35);
            border-color: transparent;
        }
    }
}

.filter-summary {
    color: #6b7280;
}

.ghost-btn {
    border: 1px solid rgba(37, 99, 235, 0.4);
    background: transparent;
    color: #2563eb;
    padding: 8px 18px;
    border-radius: 999px;
    cursor: pointer;

    &:hover {
        background: rgba(37, 99, 235, 0.1);
    }
}
</style>