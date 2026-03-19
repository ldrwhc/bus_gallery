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

const resolveRouteKey = (route) =>
    route?.name === 'Login' || route?.name === 'Register' ? 'auth-portal' : route?.fullPath;
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
