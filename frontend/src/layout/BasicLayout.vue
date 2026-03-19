<template>
    <div class="app-shell">
        <AppHeader class="app-shell__header" @toggle-search="openSearch" />
        <main class="app-shell__main">
            <slot />
        </main>
        <AppFooter class="app-shell__footer" />
        <SearchOverlay :visible="searchVisible" @close="closeSearch" @search="handleSearch" />
    </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import AppHeader from '@/components/Layout/AppHeader.vue';
import AppFooter from '@/components/Layout/AppFooter.vue';
import SearchOverlay from '@/components/Search/SearchOverlay.vue';

const searchVisible = ref(false);
const router = useRouter();

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
    padding-top: 96px;
    padding-bottom: 24px;
}
</style>
