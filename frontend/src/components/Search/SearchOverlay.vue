<template>
    <transition name="overlay-fade">
        <div v-if="visible" class="search-overlay">
            <div class="search-overlay__backdrop" @click="$emit('close')"></div>
            <div class="search-overlay__panel">
                <h2>搜索车辆</h2>
                <form class="search-overlay__form" @submit.prevent="handleSearch">
                    <input
                        ref="inputRef"
                        v-model="keyword"
                        type="search"
                        placeholder="输入车牌 / 公司 / 车型"
                    />
                    <div class="search-overlay__actions">
                        <button class="primary-btn" type="submit">搜索</button>
                        <button class="ghost-btn" type="button" @click="$emit('close')">取消</button>
                    </div>
                </form>
            </div>
        </div>
    </transition>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';

const props = defineProps({
    visible: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['close', 'search']);

const keyword = ref('');
const inputRef = ref(null);

const handleSearch = () => {
    emit('search', keyword.value.trim());
    keyword.value = '';
};

watch(
    () => props.visible,
    (value) => {
        if (value) {
            nextTick(() => {
                inputRef.value?.focus();
            });
        } else {
            keyword.value = '';
        }
    }
);
</script>

<style scoped lang="scss">
.search-overlay {
    position: fixed;
    inset: 0;
    z-index: 2000;
    display: flex;
    align-items: center;
    justify-content: center;
}

.search-overlay__backdrop {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at top, rgba(15, 23, 42, 0.85), rgba(15, 23, 42, 0.95));
    backdrop-filter: blur(10px);
}

.search-overlay__panel {
    position: relative;
    width: min(520px, 90vw);
    background: rgba(255, 255, 255, 0.97);
    border-radius: 24px;
    padding: 32px;
    box-shadow: 0 25px 60px rgba(15, 23, 42, 0.4);
    text-align: center;

    h2 {
        margin: 0 0 16px;
        color: #0f172a;
    }
}

.search-overlay__form {
    display: flex;
    flex-direction: column;
    gap: 12px;

    input {
        width: 100%;
        border: 1px solid #dbeafe;
        border-radius: 20px;
        padding: 14px 20px;
        font-size: 1rem;
        box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.05);

        &:focus-visible {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
        }
    }
}

.search-overlay__actions {
    display: flex;
    justify-content: center;
    gap: 12px;

    .primary-btn,
    .ghost-btn {
        min-width: 120px;
    }
}

.primary-btn {
    border: none;
    background: #2563eb;
    color: #fff;
    padding: 10px 24px;
    border-radius: 999px;
    cursor: pointer;
    font-weight: 600;

    &:hover {
        background: #1d4ed8;
    }
}

.ghost-btn {
    border: 1px solid rgba(37, 99, 235, 0.4);
    background: transparent;
    color: #2563eb;
    padding: 10px 24px;
    border-radius: 999px;
    cursor: pointer;
    font-weight: 600;

    &:hover {
        background: rgba(37, 99, 235, 0.1);
    }
}

.overlay-fade-enter-active,
.overlay-fade-leave-active {
    transition: opacity 0.2s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
    opacity: 0;
}
</style>
