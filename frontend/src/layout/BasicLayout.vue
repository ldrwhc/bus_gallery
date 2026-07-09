<template>
    <div class="app-shell">
        <AppHeader class="app-shell__header" @toggle-search="openSearch" />
        <main class="app-shell__main">
            <slot />
        </main>
        <AppFooter v-if="!hideFooter" class="app-shell__footer" />
        <SearchOverlay :visible="searchVisible" @close="closeSearch" @search="handleSearch" />
        <button v-if="showDocFab" class="doc-fab" type="button" title="文档" @click="$router.push({ name: 'About' })">
            <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <polyline points="14 2 14 8 20 8" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <line x1="16" y1="13" x2="8" y2="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <line x1="16" y1="17" x2="8" y2="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <span>文档</span>
        </button>
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
const showDocFab = computed(() => route.name !== 'About');

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

.doc-fab {
    position: fixed;
    bottom: 28px;
    left: 28px;
    z-index: 90;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 16px;
    border: 1px solid rgba(37, 99, 235, 0.25);
    background: rgba(255, 255, 255, 0.95);
    color: #2563eb;
    border-radius: 999px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(37, 99, 235, 0.15);
    backdrop-filter: blur(8px);
    transition: transform 0.15s, box-shadow 0.15s;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(37, 99, 235, 0.22);
    }
}
</style>
