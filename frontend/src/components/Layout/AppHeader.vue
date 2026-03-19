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
                        <div class="info-wrap">
                            <button class="info-btn" type="button" aria-label="消息通知" @click="toggleInbox">
                                <svg viewBox="0 0 24 24">
                                    <path d="M12 3a6 6 0 0 0-6 6v3.8l-1.6 2.7a1 1 0 0 0 .86 1.5h13.48a1 1 0 0 0 .86-1.5L18 12.8V9a6 6 0 0 0-6-6z" fill="none" stroke="currentColor" stroke-width="1.8" />
                                    <path d="M9.5 18.5a2.5 2.5 0 0 0 5 0" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                                </svg>
                                <span v-if="inboxCount" class="info-badge">{{ inboxCount > 99 ? '99+' : inboxCount }}</span>
                            </button>
                            <div v-if="inboxVisible" class="inbox-panel">
                                <div class="inbox-toolbar">
                                    <p class="inbox-title">{{ inboxTitle }}</p>
                                    <button
                                        class="inbox-clear"
                                        type="button"
                                        :disabled="!visibleInboxItems.length"
                                        @click="clearInbox"
                                    >
                                        一键清除
                                    </button>
                                </div>
                                <p v-if="inboxLoading" class="inbox-empty">加载中...</p>
                                <template v-else-if="visibleInboxItems.length">
                                    <div
                                        v-for="item in visibleInboxItems.slice(0, 8)"
                                        :key="item.id"
                                        class="inbox-item"
                                    >
                                        <button
                                            class="inbox-item__main"
                                            type="button"
                                            @click="openInboxItem(item)"
                                        >
                                            <span class="inbox-item__status">{{ resolveItemStatus(item) }}</span>
                                            <span class="inbox-item__text">{{ resolveItemText(item) }}</span>
                                        </button>
                                        <button
                                            class="inbox-item__delete"
                                            type="button"
                                            aria-label="删除消息"
                                            @click="removeInboxItem(item)"
                                        >
                                            ×
                                        </button>
                                    </div>
                                </template>
                                <p v-else class="inbox-empty">暂无消息</p>
                            </div>
                        </div>
                        <router-link
                            v-if="role === 'REVIEWER' || role === 'STATION'"
                            class="ghost-btn ghost-btn--sm icon-btn"
                            to="/review"
                            title="审核页"
                            aria-label="审核页"
                        >
                            <svg viewBox="0 0 24 24" aria-hidden="true">
                                <path
                                    d="M7 3h10a2 2 0 0 1 2 2v14l-3.3-1.8a1.6 1.6 0 0 0-1.5 0L11 19l-3.2-1.8a1.6 1.6 0 0 0-1.5 0L3 19V5a2 2 0 0 1 2-2z"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="1.8"
                                    stroke-linejoin="round"
                                />
                                <path d="M8 8h8M8 12h6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                            </svg>
                        </router-link>
                        <router-link v-if="role === 'STATION'" class="ghost-btn ghost-btn--sm" to="/dashboard">
                            后台
                        </router-link>
                        <router-link class="user-pill" to="/account">
                            {{ displayName }}
                        </router-link>
                        <button
                            class="ghost-btn ghost-btn--sm icon-btn logout-btn"
                            type="button"
                            title="退出"
                            aria-label="退出"
                            @click="handleLogout"
                        >
                            <svg viewBox="0 0 24 24" aria-hidden="true">
                                <path
                                    d="M12 3v8"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="2"
                                    stroke-linecap="round"
                                />
                                <path
                                    d="M7.8 5.9A8 8 0 1 0 16.2 5.9"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="2"
                                    stroke-linecap="round"
                                />
                            </svg>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import logoUrl from '@/assets/images/logo.svg?url';
import { NAV_LINKS } from '@/utils/constants';
import { fetchReviewInbox } from '@/api/reviews';

const emit = defineEmits(['toggle-search']);

const route = useRoute();
const router = useRouter();
const store = useStore();

const title = import.meta.env.VITE_APP_TITLE || 'Bus Gallery';

const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const profile = computed(() => store.state.auth.profile);
const role = computed(() => profile.value?.role || '');
const displayName = computed(() => profile.value?.displayName || profile.value?.username || '我的账号');

const visibleNavItems = computed(() =>
    NAV_LINKS.filter((item) => !item.requiresAuth || isAuthenticated.value)
);

const activeRouteName = computed(() => route.name);
const navOpen = ref(false);
const inboxVisible = ref(false);
const inboxLoading = ref(false);
const inboxItems = ref([]);
const dismissedInboxIds = ref(new Set());
let inboxTimer = null;
const INBOX_POLL_INTERVAL_MS = 90000;

const inboxStorageKey = computed(() => {
    const uid = profile.value?.id || 'guest';
    return `busGalleryInboxDismissed:${uid}`;
});
const visibleInboxItems = computed(() =>
    inboxItems.value.filter((item) => !dismissedInboxIds.value.has(String(item.id || '')))
);
const inboxCount = computed(() => visibleInboxItems.value.length);
const inboxTitle = computed(() => {
    if (role.value === 'REVIEWER' || role.value === 'STATION') {
        return '待审核消息';
    }
    return '审核通知';
});

const toggleNav = () => {
    navOpen.value = !navOpen.value;
};

const loadInbox = async () => {
    if (!isAuthenticated.value) {
        inboxItems.value = [];
        return;
    }
    inboxLoading.value = true;
    try {
        const list = await fetchReviewInbox();
        inboxItems.value = Array.isArray(list) ? list : [];
    } catch (error) {
        inboxItems.value = [];
    } finally {
        inboxLoading.value = false;
    }
};

const loadDismissedInboxIds = () => {
    if (typeof window === 'undefined') return;
    try {
        const raw = window.localStorage.getItem(inboxStorageKey.value);
        const list = raw ? JSON.parse(raw) : [];
        dismissedInboxIds.value = new Set(Array.isArray(list) ? list.map((id) => String(id)) : []);
    } catch (error) {
        dismissedInboxIds.value = new Set();
    }
};

const saveDismissedInboxIds = () => {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(inboxStorageKey.value, JSON.stringify(Array.from(dismissedInboxIds.value)));
};

const clearInbox = () => {
    visibleInboxItems.value.forEach((item) => dismissedInboxIds.value.add(String(item.id || '')));
    saveDismissedInboxIds();
};

const removeInboxItem = (item = {}) => {
    dismissedInboxIds.value.add(String(item.id || ''));
    saveDismissedInboxIds();
};

const isDocumentVisible = () => typeof document === 'undefined' || document.visibilityState === 'visible';
const shouldPollInbox = () => isAuthenticated.value && isDocumentVisible();

const startInboxPolling = () => {
    stopInboxPolling();
    if (!shouldPollInbox()) return;
    inboxTimer = setInterval(() => {
        if (!shouldPollInbox()) {
            stopInboxPolling();
            return;
        }
        loadInbox();
    }, INBOX_POLL_INTERVAL_MS);
};

const stopInboxPolling = () => {
    if (inboxTimer) {
        clearInterval(inboxTimer);
        inboxTimer = null;
    }
};

const handleVisibilityChange = () => {
    if (!isAuthenticated.value) {
        stopInboxPolling();
        return;
    }
    if (!isDocumentVisible()) {
        stopInboxPolling();
        return;
    }
    loadInbox();
    startInboxPolling();
};

const toggleInbox = async () => {
    inboxVisible.value = !inboxVisible.value;
    if (inboxVisible.value) {
        await loadInbox();
    }
};

const resolveItemStatus = (item = {}) => {
    const status = item.status || '';
    if (status === 'PENDING') return '待审核';
    if (status === 'APPROVED') return '已通过';
    if (status === 'REJECTED') return '已拒绝';
    return '通知';
};

const resolveItemText = (item = {}) => {
    if (item.status === 'REJECTED') {
        return item.rejectReason || '审核未通过';
    }
    if (item.requestPayload?.plateNumber) {
        return `${item.requestPayload.plateNumber} ${item.actionType === 'UPDATE' ? '修改申请' : '上传申请'}`;
    }
    return item.actionType === 'UPDATE' ? '图片信息修改申请' : '图片上传申请';
};

const openInboxItem = (item = {}) => {
    inboxVisible.value = false;
    if (role.value === 'REVIEWER' || role.value === 'STATION') {
        const query = item.id ? { submissionId: item.id, at: Date.now() } : { at: Date.now() };
        router.push({ name: 'ReviewCenter', query });
        return;
    }
    router.push({ name: 'Account' });
};

watch(
    () => route.fullPath,
    () => {
        navOpen.value = false;
        inboxVisible.value = false;
    }
);

watch(
    () => isAuthenticated.value,
    (val) => {
        if (val && !profile.value) {
            store.dispatch('auth/fetchProfile');
        }
        if (val) {
            loadDismissedInboxIds();
            handleVisibilityChange();
        } else {
            inboxItems.value = [];
            dismissedInboxIds.value = new Set();
            stopInboxPolling();
        }
    },
    { immediate: true }
);

watch(
    () => profile.value?.id,
    () => {
        if (isAuthenticated.value) {
            loadDismissedInboxIds();
        }
    }
);

const handleLogout = async () => {
    await store.dispatch('auth/logout');
    stopInboxPolling();
    router.push({ name: 'Home' });
};

onMounted(() => {
    if (typeof document !== 'undefined') {
        document.addEventListener('visibilitychange', handleVisibilityChange);
    }
});

onBeforeUnmount(() => {
    if (typeof document !== 'undefined') {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
    }
    stopInboxPolling();
});
</script>

<style scoped lang="scss">
.app-header {
    background: rgba(255, 255, 255, 0.95);
    border-bottom: 1px solid rgba(15, 23, 42, 0.06);
    backdrop-filter: blur(12px);
    padding: 6px 0;
    min-height: 74px;
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
        min-height: 62px;
    }
}

.brand {
    display: flex;
    align-items: center;
    gap: 12px;
    text-decoration: none;
    color: inherit;
    flex-shrink: 0;
    min-height: 44px;

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
    width: 34px;
    height: 34px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    flex: 0 0 34px;

    svg {
        width: 18px;
        height: 18px;
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

.info-wrap {
    position: relative;
}

.info-btn {
    border: 1px solid rgba(37, 99, 235, 0.28);
    background: #fff;
    color: #2563eb;
    border-radius: 999px;
    width: 34px;
    height: 34px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    position: relative;

    svg {
        width: 18px;
        height: 18px;
    }
}

.info-badge {
    position: absolute;
    top: -6px;
    right: -6px;
    min-width: 18px;
    height: 18px;
    border-radius: 999px;
    background: #ef4444;
    color: #fff;
    font-size: 0.68rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 4px;
    line-height: 1;
}

.inbox-panel {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    width: 320px;
    max-height: 420px;
    overflow-y: auto;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    background: #fff;
    box-shadow: 0 16px 34px rgba(15, 23, 42, 0.18);
    padding: 10px;
    z-index: 80;
}

.inbox-title {
    margin: 2px 0 8px;
    font-weight: 600;
    color: #0f172a;
}

.inbox-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
}

.inbox-clear {
    border: 1px solid #cbd5e1;
    background: #fff;
    color: #334155;
    border-radius: 8px;
    padding: 4px 8px;
    font-size: 0.75rem;
    cursor: pointer;
}

.inbox-clear:disabled {
    opacity: 0.45;
    cursor: not-allowed;
}

.inbox-empty {
    margin: 8px 0;
    color: #94a3b8;
    font-size: 0.85rem;
}

.inbox-item {
    width: 100%;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    background: #f8fafc;
    display: flex;
    align-items: stretch;
    margin-bottom: 8px;
}

.inbox-item__main {
    flex: 1;
    border: none;
    background: transparent;
    padding: 8px 10px;
    display: flex;
    flex-direction: column;
    gap: 4px;
    cursor: pointer;
    text-align: left;
}

.inbox-item:hover {
    background: #eef2ff;
    border-color: #c7d2fe;
}

.inbox-item__delete {
    border: none;
    background: transparent;
    width: 30px;
    color: #94a3b8;
    cursor: pointer;
    font-size: 1rem;
}

.inbox-item__delete:hover {
    color: #ef4444;
}

.inbox-item__status {
    font-size: 0.78rem;
    color: #1d4ed8;
}

.inbox-item__text {
    font-size: 0.84rem;
    color: #334155;
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

.icon-btn {
    width: 34px;
    height: 34px;
    flex: 0 0 34px;
    padding: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;

    svg {
        width: 18px;
        height: 18px;
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

@media (max-width: 1054px) {
    .app-header__inner {
        flex-wrap: nowrap;
        gap: 8px;
        position: relative;
    }

    .header-utility {
        position: relative;
        width: auto;
        justify-content: flex-end;
        flex-wrap: nowrap;
        white-space: nowrap;
        gap: 12px;
        margin-left: auto;
        min-width: 0;
    }

    .search-btn {
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
        width: auto;
        justify-content: flex-end;
        flex-wrap: nowrap;
        gap: 6px;
        min-width: 0;
        max-width: 52vw;
        overflow-x: auto;
        scrollbar-width: none;
    }

    .header-actions::-webkit-scrollbar {
        display: none;
    }

    .ghost-btn,
    .primary-btn,
    .user-pill {
        padding: 6px 10px;
    }

    .user-pill {
        max-width: 96px;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .inbox-panel {
        width: min(320px, 86vw);
    }
}

@media (max-width: 925px) {
    .brand {
        gap: 12px;

        &__text {
            display: none;
        }
    }

    .header-actions {
        max-width: 58vw;
    }
}
</style>
