<template>
    <div class="app-shell">
        <AppHeader class="app-shell__header" @toggle-search="openSearch" />
        <main class="app-shell__main">
            <slot />
        </main>
        <AppFooter v-if="!hideFooter" class="app-shell__footer" />
        <SearchOverlay :visible="searchVisible" @close="closeSearch" @search="handleSearch" />
        <div v-if="showFabGroup" class="fab-group">
            <button class="fab-btn fab-btn--stats" type="button" title="数据统计" @click="$router.push({ name: 'Stats' })">
                <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                    <rect x="3" y="3" width="7" height="16" rx="1" fill="none" stroke="currentColor" stroke-width="2"/>
                    <rect x="13" y="8" width="7" height="11" rx="1" fill="none" stroke="currentColor" stroke-width="2"/>
                    <line x1="7" y1="10" x2="7" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    <line x1="17" y1="13" x2="17" y2="15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
            </button>
            <button class="fab-btn fab-btn--doc" type="button" title="查看文档" @click="$router.push({ name: 'About' })">
                <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <polyline points="14 2 14 8 20 8" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <line x1="16" y1="13" x2="8" y2="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    <line x1="16" y1="17" x2="8" y2="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
            </button>
            <button class="fab-btn fab-btn--groupbuy" type="button" title="拼团交易台" @click="$router.push({ name: 'GroupBuyMarket' })">
                <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                    <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <line x1="3" y1="6" x2="21" y2="6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    <path d="M16 10a4 4 0 0 1-8 0" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
            </button>
        </div>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppHeader from '@/components/Layout/AppHeader.vue';
import AppFooter from '@/components/Layout/AppFooter.vue';
import SearchOverlay from '@/components/Search/SearchOverlay.vue';

const searchVisible = ref(false);
const router = useRouter();
const route = useRoute();
const hideFooter = computed(() => route.name === 'Home');
const showFabGroup = computed(() =>
    route.name !== 'About' && route.name !== 'GroupBuyMarket' && route.name !== 'Stats'
);

const lockBody = (locked) => {
    document.body.style.overflow = locked ? 'hidden' : '';
};

const openSearch = () => {
    searchVisible.value = true;
};

const closeSearch = () => {
    searchVisible.value = false;
};

const handleSearch = (keyword) => {
    const query = keyword ? { keyword } : {};
    router.push({ name: 'Home', query });
    closeSearch();
};

watch(searchVisible, (visible) => lockBody(visible));
onBeforeUnmount(() => lockBody(false));
</script>

<style scoped>
.app-shell {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f7fb;
    max-width: 100vw;
    overflow-x: hidden;
}

.app-shell__header {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: 100;
}

.app-shell__footer {
    position: static;
    width: 100%;
}

.app-shell__main {
    flex: 1;
    width: 100%;
    max-width: 100vw;
    box-sizing: border-box;
    padding-top: 74px;
    padding-bottom: 24px;
}

.fab-group {
    position: fixed;
    bottom: 28px;
    left: 28px;
    z-index: 90;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.fab-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px; height: 44px; padding: 0;
    border-radius: 50%;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(8px);
    transition: transform 0.15s, box-shadow 0.15s;
    border: 1px solid rgba(37, 99, 235, 0.25);
    color: #2563eb;
    box-shadow: 0 4px 16px rgba(37, 99, 235, 0.15);

    &:hover {
        transform: translateY(-2px);
    }

    &--doc {
        &:hover { box-shadow: 0 8px 24px rgba(37, 99, 235, 0.22); }
    }

    &--stats {
        border-color: rgba(16, 185, 129, 0.3);
        color: #10b981;
        box-shadow: 0 4px 16px rgba(16, 185, 129, 0.15);
        &:hover { box-shadow: 0 8px 24px rgba(16, 185, 129, 0.22); }
    }

    &--groupbuy {
        border-color: rgba(239, 68, 68, 0.3);
        color: #ef4444;
        box-shadow: 0 4px 16px rgba(239, 68, 68, 0.15);
        &:hover { box-shadow: 0 8px 24px rgba(239, 68, 68, 0.22); }
    }
}
</style>
