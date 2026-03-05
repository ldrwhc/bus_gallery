<template>
    <footer class="app-footer">
        <div class="app-footer__inner">
            <div>
                <strong>Bus Gallery</strong>
                <p>以数据与图片记录中国公交发展。</p>
            </div>

            <div class="app-footer__meta">
                <span>© {{ year }} Bus Gallery</span>
                <span>Made with ❤️ by Web Team</span>
            </div>
        </div>
    </footer>
</template>

<script setup>
const year = new Date().getFullYear();
</script>

<style scoped lang="scss">
.app-footer {
    background: #0f172a;
    color: rgba(255, 255, 255, 0.7);
    padding: 28px 0;
    margin-top: auto;

    &__inner {
        width: min(1200px, 100%);
        margin: 0 auto;
        padding: 0 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 24px;

        strong {
            color: #fff;
        }
    }

    &__meta {
        display: flex;
        gap: 12px;
        font-size: 0.85rem;
    }
}
</style>