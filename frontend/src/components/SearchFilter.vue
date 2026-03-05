<template>
    <el-form :inline="true" class="search-filter" @submit.prevent>
        <el-form-item label="地区">
            <el-select v-model="inner.regionId" placeholder="全部地区" clearable>
                <el-option v-for="item in regionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
        </el-form-item>

        <el-form-item label="公司">
            <el-select v-model="inner.companyId" placeholder="全部公司" clearable>
                <el-option v-for="item in companyOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
        </el-form-item>

        <el-form-item label="品牌">
            <el-select v-model="inner.brandId" placeholder="全部品牌" clearable>
                <el-option v-for="item in brandOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
        </el-form-item>

        <el-form-item label="型号">
            <el-input v-model="inner.modelKeyword" placeholder="输入型号关键字" clearable />
        </el-form-item>

        <el-button type="primary" @click="trigger">筛选</el-button>
        <el-button @click="reset">重置</el-button>
    </el-form>
</template>

<script setup>
import { reactive, watch } from 'vue';

const props = defineProps({
    modelValue: { type: Object, default: () => ({}) },
    regionOptions: { type: Array, default: () => [] },
    companyOptions: { type: Array, default: () => [] },
    brandOptions: { type: Array, default: () => [] }
});
const emit = defineEmits(['update:modelValue', 'search']);

const inner = reactive({
    regionId: null,
    companyId: null,
    brandId: null,
    modelKeyword: ''
});

watch(
    () => props.modelValue,
    (val) => Object.assign(inner, val || {}),
    { immediate: true }
);

const trigger = () => {
    emit('update:modelValue', { ...inner });
    emit('search', { ...inner });
};

const reset = () => {
    Object.assign(inner, { regionId: null, companyId: null, brandId: null, modelKeyword: '' });
    trigger();
};
</script>

<style scoped>
.search-filter {
    margin-bottom: 16px;
}
</style>