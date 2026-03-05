<template>
    <div class="basic-layout">
        <el-container>
            <el-header class="bg-header">
                <div class="logo">Bus Gallery</div>
                <el-menu class="nav-menu" mode="horizontal" :default-active="active">
                    <el-menu-item v-for="item in menus" :key="item.path" :index="item.path" @click="go(item.path)">
                        {{ item.label }}
                    </el-menu-item>
                </el-menu>
            </el-header>

            <el-main>
                <slot />
            </el-main>

            <footer-bar />
        </el-container>
    </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import FooterBar from './Footer.vue';
import { computed } from 'vue';

const route = useRoute();
const router = useRouter();

const menus = [
    { path: '/', label: '首页' },
    { path: '/regions', label: '按地区' },
    { path: '/companies', label: '按公司' },
    { path: '/brands', label: '按品牌' },
    { path: '/models', label: '按型号' },
    { path: '/upload', label: '上传' },
    { path: '/about', label: '关于' }
];

const active = computed(() => route.path);
const go = (path) => router.push(path);
</script>

<style scoped>
.basic-layout {
    min-height: 100vh;
    background: #f5f7fa;
}

.bg-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.logo {
    font-size: 24px;
    font-weight: 600;
    color: #fff;
}

.nav-menu {
    background: transparent;
}
</style>