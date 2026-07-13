<template>
    <basic-layout>
        <router-view v-slot="{ Component, route }">
            <transition name="page-fade" mode="out-in">
                <component :is="Component" :key="resolveRouteKey(route)" />
            </transition>
        </router-view>
    </basic-layout>
</template>

<script setup>
import BasicLayout from '@/layout/BasicLayout.vue';

const resolveRouteKey = (route) => {
    if (!route?.name) return route?.fullPath || 'default';
    // Use route name as key so param-only changes don't remount the component
    if (route.name === 'Login' || route.name === 'Register') return 'auth-portal';
    return route.name;
};
</script>

<style>
.page-fade-enter-active,
.page-fade-leave-active {
    transition: opacity 0.2s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
    opacity: 0;
}
</style>
