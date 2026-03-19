<template>
    <header class="app-header">
        <div class="app-header__inner">
            <router-link class="brand" :title="title" to="/">
                <img :src="logoUrl" alt="Bus Gallery" class="brand__logo" loading="eager" decoding="async" fetchpriority="high" />
                <div class="brand__text">
                    <strong>Bus Gallery</strong>
                    <span>公交图库</span>
                </div>
            </router-link>

            <button class="menu-toggle" type="button" aria-label="展开导航" @click="toggleNav">
                <span></span>
                <span></span>
                <span></span>
            </button>

            <nav :class="['nav', { 'nav--open': navOpen }]">
                <router-link v-for="item in visibleNavItems" :key="item.path" :to="item.path" class="nav__link"
                    :class="{ 'nav__link--active': item.name === activeRouteName }" @click="navOpen = false">
                    {{ item.label }}
                </router-link>
            </nav>

            <div class="header-utility">
                <button class="search-btn" type="button" aria-label="打开搜索" @click="emit('toggle-search')">
                    <svg viewBox="0 0 24 24">
                        <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2" fill="none" />
                        <line x1="16" y1="16" x2="21" y2="21" stroke="currentColor" stroke-width="2"
                            stroke-linecap="round" />
                    </svg>
                </button>

                <div class="header-actions">
                    <template v-if="isAuthenticated">
                        <router-link class="user-pill" to="/account">
                            {{ displayName }}
                        </router-link>
                        <button class="ghost-btn ghost-btn--sm" type="button" @click="handleLogout">
                            退出登录
                        </button>
                    </template>
                    <template v-else>
                        <router-link class="ghost-btn ghost-btn--sm" to="/login">登录</router-link>
                        <router-link class="primary-btn primary-btn--sm" to="/register">注册</router-link>
                    </template>
                </div>
            </div>
        </div>
    </header>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import logoUrl from '@/assets/images/logo.svg?url';
import { NAV_LINKS } from '@/utils/constants';

const emit = defineEmits(['toggle-search']);

const route = useRoute();
const router = useRouter();
const store = useStore();

const title = import.meta.env.VITE_APP_TITLE || 'Bus Gallery';

const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const profile = computed(() => store.state.auth.profile);
const displayName = computed(() => profile.value?.displayName || profile.value?.username || '我的账号');

const visibleNavItems = computed(() =>
    NAV_LINKS.filter((item) => !item.requiresAuth || isAuthenticated.value)
);

const activeRouteName = computed(() => route.name);
const navOpen = ref(false);

const toggleNav = () => {
    navOpen.value = !navOpen.value;
};

watch(
    () => route.fullPath,
    () => {
        navOpen.value = false;
    }
);

watch(
    () => isAuthenticated.value,
    (val) => {
        if (val && !profile.value) {
            store.dispatch('auth/fetchProfile');
        }
    },
    { immediate: true }
);

const handleLogout = async () => {
    await store.dispatch('auth/logout');
    router.push({ name: 'Home' });
};
</script>

<style scoped lang="scss">
.app-header {
    background: rgba(255, 255, 255, 0.95);
    border-bottom: 1px solid rgba(15, 23, 42, 0.06);
    backdrop-filter: blur(12px);
    padding: 10px 0;
    width: 100%;

    &__inner {
        width: min(1200px, 100%);
        margin: 0 auto;
        padding: 0 clamp(16px, 4vw, 32px);
        box-sizing: border-box;
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 16px;
    }
}

.brand {
    display: flex;
    align-items: center;
    gap: 12px;
    text-decoration: none;
    color: inherit;
    flex-shrink: 0;

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

.menu-toggle {
    display: none;
    flex-direction: column;
    gap: 4px;
    background: none;
    border: none;
    cursor: pointer;
    width: 32px;
    height: 32px;
    justify-content: center;
    align-items: center;

    span {
        width: 22px;
        height: 2px;
        background: #0f172a;
        border-radius: 999px;
    }
}

.nav {
    display: flex;
    flex: 1 1 0;
    gap: 8px;
    align-items: center;
    justify-content: center;

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

.nav__link:visited {
    color: inherit;
}

.header-utility {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 0 0 auto;
    white-space: nowrap;
    flex-wrap: nowrap;
    flex-shrink: 0;
}

.search-btn {
    border: none;
    background: rgba(37, 99, 235, 0.12);
    color: #2563eb;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;

    svg {
        width: 20px;
        height: 20px;
    }

    &:hover {
        background: rgba(37, 99, 235, 0.2);
    }
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    justify-content: flex-end;
    flex-wrap: nowrap;
}

.ghost-btn,
.primary-btn,
.user-pill {
    border-radius: 999px;
    font-weight: 600;
    text-decoration: none;
    text-align: center;
}

.ghost-btn {
    border: 1px solid rgba(37, 99, 235, 0.3);
    background: transparent;
    color: #2563eb;
    padding: 6px 16px;
    cursor: pointer;

    &:hover {
        background: rgba(37, 99, 235, 0.08);
    }

    &--sm {
        font-size: 0.85rem;
    }
}

.primary-btn {
    border: none;
    background: #2563eb;
    color: #fff;
    padding: 6px 16px;
    cursor: pointer;
    box-shadow: 0 8px 16px rgba(37, 99, 235, 0.25);

    &:hover {
        background: #1d4ed8;
    }

    &--sm {
        font-size: 0.85rem;
    }
}

.user-pill {
    padding: 6px 16px;
    border: 1px solid rgba(15, 23, 42, 0.1);
    color: #0f172a;
    background: #fff;
}

.user-pill:visited {
    color: #0f172a;
}

@media (max-width: 900px) {
    .app-header__inner {
        flex-wrap: wrap;
    }

    .header-utility {
        position: relative;
        width: auto;
        justify-content: flex-end;
        flex-wrap: nowrap;
        white-space: nowrap;
        gap: 10px;
    }

    .search-btn {
        order: 3;
        margin-left: 0;
    }

    .nav {
        display: none;
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        width: 100%;
        flex-direction: column;
        align-items: flex-start;
        padding: 12px 16px 16px;
        background: #fff;
        box-shadow: 0 12px 30px rgba(15, 23, 42, 0.15);
        border-radius: 0 0 16px 16px;
        z-index: 50;

        &--open {
            display: flex;
        }

        &__link {
            width: 100%;
            text-align: left;
            justify-content: flex-start;
        }
    }

    .menu-toggle {
        display: flex;
    }

    .header-actions {
        width: 100%;
        justify-content: flex-end;
        flex-wrap: nowrap;
        gap: 6px;
        position: sticky;
        bottom: 0;
        background: #fff;
        padding-top: 8px;
    }
}
</style>
