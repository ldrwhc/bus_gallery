<template>
    <transition name="overlay-fade">
        <div v-if="visible" class="search-overlay">
            <div class="search-overlay__backdrop" @click="$emit('close')"></div>
            <div class="search-overlay__panel">
                <h2>搜索</h2>
                <form class="search-overlay__form" @submit.prevent="handleSearch">
                    <div class="search-input-wrapper">
                        <input
                            ref="inputRef"
                            v-model="keyword"
                            type="search"
                            placeholder="车牌 / 线路号 / 公司 / 车型 / 配置"
                            @input="onInput"
                            @keydown.enter.prevent="handleSearch"
                            @keydown.escape="$emit('close')"
                        />
                        <!-- UPGRADE(ES): Use ES suggest API with completion suggesters for instant results -->
                        <ul v-if="suggestions.length && keyword.trim()" class="suggest-dropdown">
                            <li v-for="(s, idx) in suggestions" :key="idx"
                                @mousedown.prevent="selectSuggestion(s)">
                                <span class="suggest-type">{{ typeLabel(s.type) }}</span>
                                {{ s.value }}
                            </li>
                        </ul>
                    </div>
                    <div class="scope-selector">
                        <label v-for="opt in scopeOptions" :key="opt.value"
                               :class="{ active: scope === opt.value }">
                            <input type="radio" :value="opt.value" v-model="scope" />
                            {{ opt.label }}
                        </label>
                    </div>
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
import { useRouter } from 'vue-router';
import { searchSuggest } from '@/api/search';

const props = defineProps({
    visible: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['close']);
const router = useRouter();

const keyword = ref('');
const scope = ref('all');
const inputRef = ref(null);
const suggestions = ref([]);
let debounceTimer = null;

const scopeOptions = [
    { value: 'all', label: '全部' },
    { value: 'vehicles', label: '车辆' },
    { value: 'routes', label: '线路' }
];

const typeLabel = (type) => {
    const map = { route: '线路', vehicle: '车辆', brand: '品牌' };
    return map[type] || type;
};

const onInput = () => {
    clearTimeout(debounceTimer);
    const kw = keyword.value.trim();
    if (!kw || kw.length < 1) {
        suggestions.value = [];
        return;
    }
    debounceTimer = setTimeout(async () => {
        try {
            const resp = await searchSuggest(kw);
            suggestions.value = Array.isArray(resp) ? resp.slice(0, 6) : [];
        } catch {
            suggestions.value = [];
        }
    }, 300);
};

const selectSuggestion = (s) => {
    keyword.value = s.value;
    suggestions.value = [];
    handleSearch();
};

const handleSearch = () => {
    const kw = keyword.value.trim();
    if (!kw) return;
    emit('close');
    router.push({ name: 'SearchResults', query: { keyword: kw, scope: scope.value } });
    keyword.value = '';
    scope.value = 'all';
    suggestions.value = [];
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
            suggestions.value = [];
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

.search-input-wrapper {
    position: relative;
}

.suggest-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 10;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    margin-top: 4px;
    padding: 6px 0;
    list-style: none;
    box-shadow: 0 10px 30px rgba(15, 23, 42, 0.15);
    text-align: left;

    li {
        padding: 10px 16px;
        cursor: pointer;
        font-size: 14px;
        color: #0f172a;
        display: flex;
        align-items: center;
        gap: 8px;

        &:hover {
            background: #f1f5f9;
        }
    }

    .suggest-type {
        font-size: 11px;
        padding: 2px 8px;
        border-radius: 999px;
        background: #eff6ff;
        color: #2563eb;
        font-weight: 500;
        flex-shrink: 0;
    }
}

.scope-selector {
    display: flex;
    justify-content: center;
    gap: 8px;

    label {
        padding: 4px 14px;
        border-radius: 999px;
        border: 1px solid #d1d5db;
        font-size: 13px;
        cursor: pointer;
        color: #6b7280;
        transition: all 0.15s;

        &.active {
            background: #2563eb;
            border-color: #2563eb;
            color: #fff;
        }

        input { display: none; }
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
        box-sizing: border-box;

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
