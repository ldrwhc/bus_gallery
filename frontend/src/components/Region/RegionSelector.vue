<template>
    <div class="region-picker">
        <el-select
            v-model="selectedProvinceId"
            clearable
            filterable
            :disabled="disabled || !provinceOptions.length"
            :placeholder="provincePlaceholder"
        >
            <el-option v-for="option in provinceOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-select
            v-model="selectedRegionId"
            clearable
            filterable
            :disabled="disabled || !selectedProvinceId || !cityOptions.length"
            :placeholder="cityPlaceholderText"
        >
            <el-option v-for="option in cityOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
    </div>
    <div v-if="isProvinceOnlySelection" class="region-tip">当前为直辖市（或仅省级数据），已按省级地区选择。</div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import {
    buildCityOptions,
    buildProvinceOptions,
    buildRegionMap,
    isProvinceRegion,
    resolveProvinceId
} from '@/utils/region';

const props = defineProps({
    modelValue: {
        type: [Number, String],
        default: null
    },
    regions: {
        type: Array,
        default: () => []
    },
    provincePlaceholder: {
        type: String,
        default: '先选省'
    },
    cityPlaceholder: {
        type: String,
        default: '再选市（可搜索）'
    },
    disabled: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['update:modelValue']);

const toNullableNumber = (value) => {
    if (value === null || value === undefined || value === '') return null;
    const numeric = Number(value);
    return Number.isNaN(numeric) ? null : numeric;
};

const regionMap = computed(() => buildRegionMap(props.regions));
const provinceOptions = computed(() => buildProvinceOptions(props.regions));
const selectedProvinceId = ref(null);

const cityOptions = computed(() => buildCityOptions(props.regions, selectedProvinceId.value, true));
const selectedRegionId = computed({
    get: () => toNullableNumber(props.modelValue),
    set: (value) => emit('update:modelValue', toNullableNumber(value))
});

const isProvinceOnlySelection = computed(() => {
    if (!selectedProvinceId.value || cityOptions.value.length !== 1) return false;
    return cityOptions.value[0].value === selectedProvinceId.value;
});

const cityPlaceholderText = computed(() =>
    isProvinceOnlySelection.value ? '直辖市按省级选择' : props.cityPlaceholder
);

watch(
    () => props.modelValue,
    (value) => {
        const currentRegion = regionMap.value[toNullableNumber(value)];
        selectedProvinceId.value = resolveProvinceId(currentRegion, regionMap.value);
    },
    { immediate: true }
);

watch(
    [selectedProvinceId, cityOptions],
    ([provinceId, options]) => {
        const currentRegionId = toNullableNumber(props.modelValue);
        if (!provinceId) {
            if (currentRegionId != null) emit('update:modelValue', null);
            return;
        }
        if (currentRegionId != null) {
            const currentRegion = regionMap.value[currentRegionId];
            if (resolveProvinceId(currentRegion, regionMap.value) === provinceId) {
                if (options.some((item) => item.value === currentRegionId) || isProvinceRegion(currentRegion)) {
                    return;
                }
            }
        }
        const nextRegionId = options[0]?.value || null;
        if (nextRegionId !== currentRegionId) {
            emit('update:modelValue', nextRegionId);
        }
    },
    { immediate: true }
);
</script>

<style scoped lang="scss">
.region-picker {
    display: grid;
    grid-template-columns: 1fr 1.2fr;
    gap: 8px;
    width: 100%;
}

.region-tip {
    margin-top: 6px;
    color: #64748b;
    font-size: 12px;
}

@media (max-width: 640px) {
    .region-picker {
        grid-template-columns: 1fr;
    }
}
</style>
