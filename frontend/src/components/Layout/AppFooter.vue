<template>
    <footer class="app-footer">
        <div class="app-footer__inner">
            <div class="app-footer__brand">
                <strong>Bus Gallery</strong>
                <p>记录公交发展。</p>
            </div>

            <div class="app-footer__meta">
                <span>© {{ year }} Bus Gallery</span>
                <span>Made with whc by the community</span>
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
    color: rgba(255, 255, 255, 0.75);
    padding: 16px 0;
    width: 100%;

    &__inner {
        width: min(1200px, 100%);
        margin: 0 auto;
        padding: 0 clamp(16px, 4vw, 32px);
        box-sizing: border-box;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;
    }

    &__brand {
        strong {
            color: #fff;
            display: block;
            margin-bottom: 4px;
        }

        p {
            margin: 0;
            font-size: 0.9rem;
        }
    }

    &__meta {
        display: flex;
        gap: 12px;
        font-size: 0.85rem;
        flex-wrap: wrap;
        justify-content: flex-end;
        margin-left: auto;
        text-align: right;
    }
}
</style>
