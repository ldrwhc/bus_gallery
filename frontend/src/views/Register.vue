<template>
    <div class="auth-page">
        <div class="auth-glow auth-glow--left"></div>
        <div class="auth-glow auth-glow--right"></div>

        <section class="auth-card">
            <header class="auth-head">
                <nav class="auth-switch" aria-label="认证页面切换">
                    <router-link class="auth-switch__item" to="/login">登录</router-link>
                    <router-link class="auth-switch__item is-active" to="/register">注册</router-link>
                </nav>
                <h1>创建账号</h1>
                <p>注册后即可上传图片、修改信息并参与审核流程。</p>
            </header>

            <el-form class="auth-form" label-position="top" :model="form" @submit.prevent>
                <el-form-item label="昵称">
                    <el-input v-model.trim="form.displayName" placeholder="站内展示名称" autocomplete="nickname" />
                </el-form-item>
                <el-form-item label="用户名">
                    <el-input v-model.trim="form.username" placeholder="3-32 位字符" autocomplete="username" />
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model.trim="form.email" placeholder="用于验证和找回密码" autocomplete="email" />
                </el-form-item>
                <el-form-item label="邮箱验证码">
                    <div class="inline-row">
                        <el-input v-model.trim="form.emailCode" placeholder="输入邮箱验证码" />
                        <el-button :loading="sendingCode" :disabled="!canSendCode" @click="handleSendCode">
                            {{ sendCodeText }}
                        </el-button>
                    </div>
                </el-form-item>
                <el-form-item label="密码">
                    <el-input
                        v-model="form.password"
                        type="password"
                        placeholder="至少 8 位"
                        show-password
                        autocomplete="new-password"
                    />
                </el-form-item>
                <el-form-item label="确认密码">
                    <el-input
                        v-model="form.confirmPassword"
                        type="password"
                        placeholder="再次输入密码"
                        show-password
                        autocomplete="new-password"
                    />
                </el-form-item>

                <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
                <div class="actions">
                    <el-button class="submit-btn" type="primary" :loading="loading" @click="handleSubmit">
                        注册
                    </el-button>
                </div>
            </el-form>
        </section>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import { sendRegisterEmailCode } from '@/api/auth';

const store = useStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
    username: '',
    displayName: '',
    email: '',
    emailCode: '',
    challengeId: '',
    password: '',
    confirmPassword: ''
});

const localError = ref('');
const loading = computed(() => store.state.auth.loading);
const storeError = computed(() => store.state.auth.error);
const errorMessage = computed(() => localError.value || storeError.value);

const sendingCode = ref(false);
const countdown = ref(0);
let timer = null;

const canSendCode = computed(() =>
    !sendingCode.value &&
    countdown.value <= 0 &&
    Boolean(form.username && form.email)
);
const sendCodeText = computed(() => (countdown.value > 0 ? `${countdown.value}s 后重发` : '发送验证码'));

const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

watch(storeError, () => {
    if (storeError.value) {
        localError.value = '';
    }
});

const startCountdown = (seconds = 60) => {
    countdown.value = seconds;
    if (timer) clearInterval(timer);
    timer = setInterval(() => {
        countdown.value -= 1;
        if (countdown.value <= 0) {
            clearInterval(timer);
            timer = null;
        }
    }, 1000);
};

const handleSendCode = async () => {
    localError.value = '';
    if (!form.username || !form.email) {
        localError.value = '请先填写用户名和邮箱';
        return;
    }
    if (!emailReg.test(form.email)) {
        localError.value = '邮箱格式不正确';
        return;
    }
    sendingCode.value = true;
    try {
        const data = await sendRegisterEmailCode({
            username: form.username,
            email: form.email
        });
        form.challengeId = data?.challengeId || '';
        startCountdown(Number(data?.resendAfterSeconds || 60));
    } catch (error) {
        localError.value = error?.message || '发送验证码失败';
    } finally {
        sendingCode.value = false;
    }
};

const handleSubmit = async () => {
    localError.value = '';
    if (!form.displayName || !form.username || !form.email || !form.emailCode || !form.password || !form.confirmPassword) {
        localError.value = '请完整填写所有字段';
        return;
    }
    if (!emailReg.test(form.email)) {
        localError.value = '邮箱格式不正确';
        return;
    }
    if (!form.challengeId) {
        localError.value = '请先获取邮箱验证码';
        return;
    }
    if (form.password.length < 8) {
        localError.value = '密码长度至少 8 位';
        return;
    }
    if (form.password !== form.confirmPassword) {
        localError.value = '两次输入的密码不一致';
        return;
    }
    try {
        await store.dispatch('auth/register', { ...form });
        const redirect = route.query.redirect || '/account';
        router.replace(redirect);
    } catch (e) {
        // handled in store
    }
};

onBeforeUnmount(() => {
    if (timer) clearInterval(timer);
});
</script>

<style scoped lang="scss">
.auth-page {
    min-height: calc(100vh - 74px);
    display: grid;
    place-items: center;
    position: relative;
    overflow: hidden;
    padding: 36px 16px;
    background: radial-gradient(circle at 88% 8%, #dbeafe 0%, #f8fafc 40%, #f1f5f9 100%);
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
    left: -130px;
    bottom: -110px;
    background: rgba(14, 165, 233, 0.2);
}

.auth-glow--right {
    width: 360px;
    height: 360px;
    right: -150px;
    top: -120px;
    background: rgba(37, 99, 235, 0.22);
}

.auth-card {
    position: relative;
    z-index: 1;
    width: min(500px, 100%);
    border-radius: 24px;
    padding: 24px;
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

.inline-row {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 10px;
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
}
</style>
