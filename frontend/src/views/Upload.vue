<template>
    <div class="page upload-view">
        <AppHeader />

        <main class="upload-main constrained">
            <section class="intro">
                <p class="eyebrow">Upload</p>
                <h1>上传车辆图片并建档</h1>
                <p class="subtitle">
                    填写车辆信息并上传实拍图片，系统会自动补全品牌 / 车型 / 公司 / 地区。
                </p>
            </section>

            <section class="form-wrapper">
                <ImageUpload />
            </section>
        </main>

        <AppFooter />
    </div>
</template>

<script setup>
import AppHeader from '@/components/Layout/AppHeader.vue';
import AppFooter from '@/components/Layout/AppFooter.vue';
import ImageUpload from '@/components/Upload/ImageUpload.vue';
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f7fb;
}

.constrained {
    width: min(960px, 100%);
    margin: 0 auto;
    flex: 1;
    padding: 32px 24px 72px;
}

.intro {
    margin-bottom: 24px;

    .eyebrow {
        letter-spacing: 0.25em;
        text-transform: uppercase;
        color: #94a3b8;
        margin-bottom: 8px;
    }

    h1 {
        margin: 0;
    }

    .subtitle {
        color: #6b7280;
        margin-top: 12px;
    }
}

.form-wrapper {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}
</style>