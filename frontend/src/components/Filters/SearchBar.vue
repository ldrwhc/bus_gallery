<template>
    <form class="search-bar" @submit.prevent="triggerSearch">
        <span class="icon">🔍</span>
        <input v-model="keyword" class="search-input" type="search" :placeholder="placeholder"
            @keyup.enter="triggerSearch" />
        <button class="primary-btn" type="submit">搜索</button>
    </form>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
    value: {
        type: String,
        default: ''
    },
    placeholder: {
        type: String,
        default: '搜索'
    }
});

const emit = defineEmits(['update:value', 'search']);

const keyword = ref(props.value);

watch(
    () => props.value,
    (val) => {
        if (val !== keyword.value) {
            keyword.value = val || '';
        }
    }
);

const triggerSearch = () => {
    emit('update:value', keyword.value);
    emit('search', keyword.value.trim());
};
</script>

<style scoped lang="scss">
.search-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 999px;
    padding: 8px 8px 8px 16px;
    backdrop-filter: blur(6px);
}

.icon {
    font-size: 1.1rem;
    opacity: 0.85;
}

.search-input {
    flex: 1;
    border: none;
    background: none;
    color: #fff;
    font-size: 1rem;

    &::placeholder {
        color: rgba(255, 255, 255, 0.6);
    }

    &:focus {
        outline: none;
    }
}

.primary-btn {
    padding: 10px 20px;
    border-radius: 999px;
    border: none;
    background: #fff;
    color: #2563eb;
    font-weight: 600;
    cursor: pointer;

    &:hover {
        background: #f1f5f9;
    }
}
</style>