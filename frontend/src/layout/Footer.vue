<template>
    <el-footer class="footer">
        <div>© {{ year }} Bus Gallery · 交通文化收藏馆</div>
    </el-footer>
</template>

<script setup>
const year = new Date().getFullYear();
</script>

<style scoped>
.footer {
    text-align: center;
    color: #909399;
    padding: 24px 0;
}
</style>