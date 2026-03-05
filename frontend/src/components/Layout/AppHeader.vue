<template>
    <header class="app-header">
        <div class="app-header__inner">
            <router-link class="brand" :title="title" to="/">
                <img :src="logoUrl" alt="Bus Gallery" class="brand__logo" />
                <div class="brand__text">
                    <strong>Bus Gallery</strong>
                    <span>中国公交车辆图鉴</span>
                </div>
            </router-link>

            <nav class="nav">
                <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav__link"
                    :class="{ 'nav__link--active': item.name === activeRouteName }">
                    {{ item.label }}
                </router-link>
            </nav>

            <div class="header-actions">
                <a class="ghost-btn ghost-btn--sm" href="https://github.com/" rel="noopener noreferrer" target="_blank">
                    GitHub
                </a>
            </div>
        </div>
    </header>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import logoUrl from '@/assets/images/logo.svg?url';
import { NAV_LINKS } from '@/utils/constants';

const route = useRoute();
const title = import.meta.env.VITE_APP_TITLE || 'Bus Gallery';

const navItems = NAV_LINKS;
const activeRouteName = computed(() => route.name);
</script>

<style scoped lang="scss">
.app-header {
    position: sticky;
    top: 0;
    z-index: 50;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(15, 23, 42, 0.06);
    padding: 8px 0;

    &__inner {
        width: min(1200px, 100%);
        margin: 0 auto;
        padding: 0 24px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 24px;
    }
}

.brand {
    display: flex;
    align-items: center;
    gap: 12px;
    text-decoration: none;
    color: inherit;

    &__logo {
        width: 44px;
        height: 44px;
    }

    &__text {
        display: flex;
        flex-direction: column;
        line-height: 1.2;

        span {
            font-size: 0.8rem;
            color: #6b7280;
        }
    }
}

.nav {
    display: flex;
    gap: 12px;

    &__link {
        padding: 8px 14px;
        border-radius: 999px;
        text-decoration: none;
        color: #475569;
        font-weight: 600;
        transition: background 0.2s, color 0.2s;

        &:hover {
            background: rgba(37, 99, 235, 0.08);
            color: #2563eb;
        }

        &--active {
            background: #2563eb;
            color: #fff;
            box-shadow: 0 10px 20px rgba(37, 99, 235, 0.2);
        }
    }
}

.header-actions {
    display: flex;
    align-items: center;
}

.ghost-btn {
    border: 1px solid rgba(37, 99, 235, 0.3);
    background: transparent;
    color: #2563eb;
    padding: 6px 16px;
    border-radius: 999px;
    cursor: pointer;
    font-weight: 600;

    &:hover {
        background: rgba(37, 99, 235, 0.08);
    }

    &--sm {
        font-size: 0.85rem;
    }
}
</style>