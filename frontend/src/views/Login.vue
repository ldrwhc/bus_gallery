<template>
    <div class="auth-page">
        <div class="auth-glow auth-glow--left"></div>
        <div class="auth-glow auth-glow--right"></div>

        <section class="auth-card">
            <header class="auth-head">
                <nav class="auth-switch" aria-label="认证页面切换">
                    <router-link class="auth-switch__item is-active" to="/login">登录</router-link>
                    <router-link class="auth-switch__item" to="/register">注册</router-link>
                </nav>
                <h1>欢迎回来</h1>
                <p>登录后继续管理上传、审核消息和车辆信息。</p>
            </header>

            <el-form class="auth-form" label-position="top" :model="form" @submit.prevent>
                <el-form-item label="用户名">
                    <el-input v-model.trim="form.username" placeholder="输入用户名" autocomplete="username" />
                </el-form-item>
                <el-form-item label="密码">
                    <el-input
                        v-model="form.password"
                        type="password"
                        placeholder="输入密码"
                        show-password
                        autocomplete="current-password"
                    />
                </el-form-item>
                <el-form-item v-if="captcha.required" label="图形验证码">
                    <div class="captcha-row">
                        <el-input v-model.trim="form.captchaCode" placeholder="输入图形验证码" />
                        <img
                            v-if="captcha.imageBase64"
                            class="captcha-image"
                            :src="captcha.imageBase64"
                            alt="captcha"
                            @click="refreshCaptcha"
                        />
                        <el-button v-else @click="refreshCaptcha">获取验证码</el-button>
                    </div>
                </el-form-item>
                <p v-if="error" class="error-text">{{ error }}</p>

                <div class="actions">
                    <el-button class="submit-btn" type="primary" :loading="loading" @click="handleSubmit">
                        登录
                    </el-button>
                    <router-link class="text-btn" to="/forgot-password">忘记密码？</router-link>
                </div>
            </el-form>
        </section>
    </div>
</template>

<script setup>
import { computed, reactive } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import { issueCaptcha } from '@/api/auth';

const store = useStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
    username: '',
    password: '',
    captchaId: '',
    captchaCode: ''
});

const captcha = reactive({
    required: false,
    imageBase64: ''
});

const loading = computed(() => store.state.auth.loading);
const error = computed(() => store.state.auth.error);

const handleSubmit = async () => {
    if (!form.username || !form.password) return;
    try {
        await store.dispatch('auth/login', { ...form });
        captcha.required = false;
        captcha.imageBase64 = '';
        form.captchaId = '';
        form.captchaCode = '';
        const redirect = route.query.redirect || '/account';
        router.replace(redirect);
    } catch (e) {
        const message = e?.message || '';
        if (message.includes('图形验证码')) {
            captcha.required = true;
            await refreshCaptcha();
        }
    }
};

const refreshCaptcha = async () => {
    try {
        const data = await issueCaptcha('login');
        form.captchaId = data?.captchaId || '';
        captcha.imageBase64 = data?.imageBase64 || '';
    } catch (e) {
        // request interceptor handles the toast
    }
};
</script>

<style scoped lang="scss">
.auth-page {
    min-height: calc(100vh - 74px);
    display: grid;
    place-items: center;
    position: relative;
    overflow: hidden;
    padding: 36px 16px;
    background: radial-gradient(circle at 12% 8%, #dbeafe 0%, #f8fafc 40%, #f1f5f9 100%);
}

.auth-glow {
    position: absolute;
    border-radius: 999px;
    filter: blur(20px);
    pointer-events: none;
}

.auth-glow--left {
    width: 320px;
    height: 320px;
    left: -120px;
    top: -80px;
    background: rgba(37, 99, 235, 0.2);
}

.auth-glow--right {
    width: 360px;
    height: 360px;
    right: -150px;
    bottom: -120px;
    background: rgba(14, 165, 233, 0.2);
}

.auth-card {
    position: relative;
    z-index: 1;
    width: min(460px, 100%);
    border-radius: 24px;
    padding: 26px 24px 24px;
    background: rgba(255, 255, 255, 0.92);
    border: 1px solid rgba(255, 255, 255, 0.9);
    box-shadow: 0 24px 60px rgba(15, 23, 42, 0.14);
    backdrop-filter: blur(12px);
}

.auth-head h1 {
    margin: 16px 0 6px;
    font-size: 1.5rem;
    color: #0f172a;
}

.auth-head p {
    margin: 0;
    color: #64748b;
}

.auth-switch {
    display: grid;
    grid-template-columns: 1fr 1fr;
    padding: 4px;
    background: #eef2ff;
    border-radius: 999px;
    gap: 4px;
}

.auth-switch__item {
    text-align: center;
    text-decoration: none;
    color: #334155;
    font-weight: 600;
    border-radius: 999px;
    padding: 8px 0;
    transition: all 0.22s ease;
}

.auth-switch__item.is-active {
    background: #2563eb;
    color: #fff;
    box-shadow: 0 10px 18px rgba(37, 99, 235, 0.24);
}

.auth-form {
    margin-top: 20px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.auth-form :deep(.el-form-item) {
    margin-bottom: 8px;
}

.auth-form :deep(.el-form-item__label) {
    padding-bottom: 4px;
    line-height: 1.2;
}

.captcha-row {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 128px;
    gap: 10px;
    align-items: center;
}

.captcha-image {
    width: 128px;
    height: 40px;
    object-fit: cover;
    border-radius: 10px;
    border: 1px solid #cbd5e1;
    cursor: pointer;
}

.actions {
    margin-top: 4px;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.submit-btn {
    height: 40px;
    font-weight: 700;
}

.text-btn {
    text-align: center;
    color: #2563eb;
    text-decoration: none;
}

.text-btn:visited {
    color: #2563eb;
}

.error-text {
    color: #b91c1c;
    margin: -4px 0 2px;
}

@media (max-width: 520px) {
    .auth-page {
        padding: 24px 12px;
    }

    .auth-card {
        padding: 20px 16px 18px;
        border-radius: 18px;
    }

    .captcha-row {
        grid-template-columns: 1fr 112px;
    }

    .captcha-image {
        width: 112px;
    }
}
</style>
