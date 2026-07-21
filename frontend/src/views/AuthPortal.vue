<template>
    <div class="auth-portal">
        <div class="auth-halo auth-halo--a"></div>
        <div class="auth-halo auth-halo--b"></div>

        <section class="auth-container" :class="{ 'right-panel-active': isRegisterMode }">
            <nav class="mobile-switch" aria-label="认证页面切换">
                <button
                    class="mobile-switch__item"
                    :class="{ 'is-active': !isRegisterMode }"
                    type="button"
                    @click="goLogin"
                >
                    登录
                </button>
                <button
                    class="mobile-switch__item"
                    :class="{ 'is-active': isRegisterMode }"
                    type="button"
                    @click="goRegister"
                >
                    注册
                </button>
            </nav>

            <div class="form-container sign-up-container">
                <el-form class="auth-form" label-position="top" :model="registerForm" @submit.prevent>
                    <h1>创建账号</h1>
                    <p class="form-subtitle">加入 Bus Gallery，上传并管理你的公交影像。</p>

                    <el-form-item label="昵称">
                        <el-input v-model.trim="registerForm.displayName" placeholder="站内展示名称" autocomplete="nickname" />
                    </el-form-item>
                    <el-form-item label="用户名">
                        <el-input v-model.trim="registerForm.username" placeholder="3-32 位字符" autocomplete="username" />
                    </el-form-item>
                    <el-form-item label="邮箱">
                        <el-input v-model.trim="registerForm.email" placeholder="用于验证和找回密码" autocomplete="email" />
                    </el-form-item>
                    <el-form-item label="邮箱验证码">
                        <div class="inline-row">
                            <el-input v-model.trim="registerForm.emailCode" placeholder="输入邮箱验证码" />
                            <el-button :loading="sendingCode" :disabled="!canSendCode" @click="handleSendCode">
                                {{ sendCodeText }}
                            </el-button>
                        </div>
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input
                            v-model="registerForm.password"
                            type="password"
                            placeholder="至少 8 位"
                            show-password
                            autocomplete="new-password"
                        />
                    </el-form-item>
                    <el-form-item label="确认密码">
                        <el-input
                            v-model="registerForm.confirmPassword"
                            type="password"
                            placeholder="再次输入密码"
                            show-password
                            autocomplete="new-password"
                        />
                    </el-form-item>

                    <p v-if="registerErrorText" class="error-text">{{ registerErrorText }}</p>
                    <el-button class="submit-btn" type="primary" :loading="loading && isRegisterMode" @click="handleRegister">
                        注册
                    </el-button>
                </el-form>
            </div>

            <div class="form-container sign-in-container">
                <el-form class="auth-form" label-position="top" :model="loginForm" @submit.prevent>
                    <h1>欢迎回来</h1>
                    <p class="form-subtitle">登录后继续管理上传记录与审核消息。</p>

                    <el-form-item label="用户名">
                        <el-input v-model.trim="loginForm.username" placeholder="输入用户名" autocomplete="username" />
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input
                            v-model="loginForm.password"
                            type="password"
                            placeholder="输入密码"
                            show-password
                            autocomplete="current-password"
                        />
                    </el-form-item>
                    <el-form-item v-if="captcha.required" label="图形验证码">
                        <div class="inline-row inline-row--captcha">
                            <el-input v-model.trim="loginForm.captchaCode" placeholder="输入图形验证码" />
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

                    <p v-if="loginErrorText" class="error-text">{{ loginErrorText }}</p>
                    <el-button class="submit-btn" type="primary" :loading="loading && !isRegisterMode" @click="handleLogin">
                        登录
                    </el-button>
                    <router-link class="text-btn" to="/forgot-password">忘记密码？</router-link>
                </el-form>
            </div>

            <div class="overlay-container">
                <div class="overlay">
                    <div class="overlay-panel overlay-left">
                        <h2>注册→</h2>
                        <p>已经有账号？点击登录👇</p>
                        <button class="ghost-btn" type="button" @click="goLogin">去登录</button>
                    </div>
                    <div class="overlay-panel overlay-right">
                        <h2>←登录</h2>
                        <p>没有账号？点击注册👇</p>
                        <button class="ghost-btn" type="button" @click="goRegister">去注册</button>
                    </div>
                </div>
            </div>
        </section>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { issueCaptcha, sendRegisterEmailCode } from '@/api/auth';

const route = useRoute();
const router = useRouter();
const store = useStore();

const isRegisterMode = computed(() => route.name === 'Register');
const loading = computed(() => store.state.auth.loading);
const storeError = computed(() => store.state.auth.error || '');
const redirectPath = computed(() =>
    typeof route.query.redirect === 'string' && route.query.redirect ? route.query.redirect : '/'
);

const loginForm = reactive({
    username: '',
    password: '',
    captchaId: '',
    captchaCode: ''
});
const captcha = reactive({
    required: false,
    imageBase64: ''
});
const loginLocalError = ref('');
const loginErrorText = computed(() => loginLocalError.value || (!isRegisterMode.value ? storeError.value : ''));

const registerForm = reactive({
    username: '',
    displayName: '',
    email: '',
    emailCode: '',
    challengeId: '',
    password: '',
    confirmPassword: ''
});
const registerLocalError = ref('');
const registerErrorText = computed(() => registerLocalError.value || (isRegisterMode.value ? storeError.value : ''));
const sendingCode = ref(false);
const countdown = ref(0);
let countdownTimer = null;
const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const canSendCode = computed(() =>
    !sendingCode.value &&
    countdown.value <= 0 &&
    Boolean(registerForm.username && registerForm.email)
);
const sendCodeText = computed(() => (countdown.value > 0 ? `${countdown.value}s 后重发` : '发送验证码'));

const clearAuthError = () => {
    store.commit('auth/SET_ERROR', null);
};

const goLogin = () => {
    if (route.name === 'Login') return;
    clearAuthError();
    registerLocalError.value = '';
    router.push({ name: 'Login', query: route.query });
};

const goRegister = () => {
    if (route.name === 'Register') return;
    clearAuthError();
    loginLocalError.value = '';
    router.push({ name: 'Register', query: route.query });
};

const handleLogin = async () => {
    loginLocalError.value = '';
    if (!loginForm.username || !loginForm.password) {
        loginLocalError.value = '请输入用户名和密码';
        return;
    }
    try {
        await store.dispatch('auth/login', { ...loginForm });
        captcha.required = false;
        captcha.imageBase64 = '';
        loginForm.captchaId = '';
        loginForm.captchaCode = '';
        router.replace(redirectPath.value);
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
        loginForm.captchaId = data?.captchaId || '';
        captcha.imageBase64 = data?.imageBase64 || '';
    } catch (e) {
        // toast handled in interceptor
    }
};

const startCountdown = (seconds = 60) => {
    countdown.value = seconds;
    if (countdownTimer) clearInterval(countdownTimer);
    countdownTimer = setInterval(() => {
        countdown.value -= 1;
        if (countdown.value <= 0) {
            clearInterval(countdownTimer);
            countdownTimer = null;
        }
    }, 1000);
};

const handleSendCode = async () => {
    registerLocalError.value = '';
    if (!registerForm.username || !registerForm.email) {
        registerLocalError.value = '请先填写用户名和邮箱';
        return;
    }
    if (!emailReg.test(registerForm.email)) {
        registerLocalError.value = '邮箱格式不正确';
        return;
    }
    sendingCode.value = true;
    try {
        const data = await sendRegisterEmailCode({
            username: registerForm.username,
            email: registerForm.email
        });
        registerForm.challengeId = data?.challengeId || '';
        startCountdown(Number(data?.resendAfterSeconds || 60));
    } catch (error) {
        registerLocalError.value = error?.message || '发送验证码失败';
    } finally {
        sendingCode.value = false;
    }
};

const handleRegister = async () => {
    registerLocalError.value = '';
    if (
        !registerForm.displayName ||
        !registerForm.username ||
        !registerForm.email ||
        !registerForm.emailCode ||
        !registerForm.password ||
        !registerForm.confirmPassword
    ) {
        registerLocalError.value = '请完整填写所有字段';
        return;
    }
    if (!emailReg.test(registerForm.email)) {
        registerLocalError.value = '邮箱格式不正确';
        return;
    }
    if (!registerForm.challengeId) {
        registerLocalError.value = '请先获取邮箱验证码';
        return;
    }
    if (registerForm.password.length < 8) {
        registerLocalError.value = '密码长度至少 8 位';
        return;
    }
    if (registerForm.password !== registerForm.confirmPassword) {
        registerLocalError.value = '两次输入的密码不一致';
        return;
    }
    try {
        await store.dispatch('auth/register', { ...registerForm });
        router.replace(redirectPath.value);
    } catch (e) {
        // handled by store
    }
};

watch(
    () => route.name,
    () => {
        clearAuthError();
        loginLocalError.value = '';
        registerLocalError.value = '';
    }
);

onBeforeUnmount(() => {
    if (countdownTimer) clearInterval(countdownTimer);
});
</script>

<style scoped lang="scss">
.auth-portal {
    min-height: calc(100vh - 74px);
    display: grid;
    place-items: center;
    padding: 28px 16px;
    overflow: hidden;
    position: relative;
    background: radial-gradient(circle at 18% 18%, #dbeafe 0%, #e2e8f0 45%, #f8fafc 100%);
}

.auth-halo {
    position: absolute;
    border-radius: 999px;
    filter: blur(26px);
    pointer-events: none;
}

.auth-halo--a {
    width: 360px;
    height: 360px;
    left: -120px;
    top: -100px;
    background: rgba(14, 116, 144, 0.24);
}

.auth-halo--b {
    width: 420px;
    height: 420px;
    right: -170px;
    bottom: -160px;
    background: rgba(37, 99, 235, 0.25);
}

.auth-container {
    position: relative;
    width: min(920px, 100%);
    min-height: 620px;
    border-radius: 22px;
    overflow: hidden;
    box-shadow: 0 30px 70px rgba(15, 23, 42, 0.18);
    background: #fff;
    z-index: 1;
}

.mobile-switch {
    display: none;
    position: absolute;
    top: 16px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 120;
    width: min(360px, calc(100% - 30px));
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    padding: 6px;
    border-radius: 999px;
    background: rgba(15, 23, 42, 0.1);
    backdrop-filter: blur(8px);
}

.mobile-switch__item {
    border: 0;
    height: 34px;
    border-radius: 999px;
    cursor: pointer;
    font-weight: 700;
    color: #0f172a;
    background: transparent;
    transition: all 0.2s ease;
}

.mobile-switch__item.is-active {
    background: #0f172a;
    color: #fff;
    box-shadow: 0 8px 18px rgba(15, 23, 42, 0.22);
}

.form-container {
    position: absolute;
    top: 0;
    height: 100%;
    background: #fff;
    transition: all 0.62s cubic-bezier(0.22, 1, 0.36, 1);
}

.sign-in-container {
    left: 0;
    width: 50%;
    z-index: 2;
}

.auth-container.right-panel-active .sign-in-container {
    transform: translateX(100%);
    opacity: 0;
    pointer-events: none;
}

.sign-up-container {
    left: 0;
    width: 50%;
    opacity: 0;
    pointer-events: none;
    z-index: 1;
}

.auth-container.right-panel-active .sign-up-container {
    transform: translateX(100%);
    opacity: 1;
    pointer-events: auto;
    z-index: 5;
}

.auth-form {
    height: 100%;
    box-sizing: border-box;
    padding: 34px 34px 30px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
}

.auth-form h1 {
    margin: 0;
    font-size: 2rem;
    line-height: 1.1;
    color: #0f172a;
    font-family: "Poppins", "Noto Sans SC", sans-serif;
}

.form-subtitle {
    margin: 8px 0 14px;
    color: #64748b;
}

.auth-form :deep(.el-form-item) {
    margin-bottom: 8px;
}

.auth-form :deep(.el-form-item__label) {
    padding-bottom: 4px;
    line-height: 1.2;
    color: #334155;
}

.inline-row {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 10px;
    align-items: center;
}

.inline-row--captcha {
    grid-template-columns: 1fr 124px;
}

.captcha-image {
    width: 124px;
    height: 40px;
    object-fit: cover;
    border-radius: 10px;
    border: 1px solid #cbd5e1;
    cursor: pointer;
}

.submit-btn {
    width: 100%;
    margin-top: 8px;
    height: 42px;
    font-weight: 700;
    border-radius: 12px;
}

.text-btn {
    margin-top: 10px;
    text-align: center;
    text-decoration: none;
    color: #2563eb;
}

.error-text {
    color: #b91c1c;
    margin: 2px 0 4px;
    min-height: 20px;
    font-size: 0.9rem;
}

.overlay-container {
    position: absolute;
    top: 0;
    left: 50%;
    width: 50%;
    height: 100%;
    overflow: hidden;
    transition: transform 0.62s cubic-bezier(0.22, 1, 0.36, 1);
    z-index: 90;
}

.auth-container.right-panel-active .overlay-container {
    transform: translateX(-100%);
}

.overlay {
    position: relative;
    left: -100%;
    width: 200%;
    height: 100%;
    background: linear-gradient(145deg, #0f172a, #1d4ed8 52%, #0ea5e9);
    color: #fff;
    transform: translateX(0);
    transition: transform 0.62s cubic-bezier(0.22, 1, 0.36, 1);
}

.auth-container.right-panel-active .overlay {
    transform: translateX(50%);
}

.overlay-panel {
    position: absolute;
    top: 0;
    width: 50%;
    height: 100%;
    padding: 0 44px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    transition: transform 0.62s cubic-bezier(0.22, 1, 0.36, 1);
}

.overlay-left {
    transform: translateX(-20%);
}

.auth-container.right-panel-active .overlay-left {
    transform: translateX(0);
}

.overlay-right {
    right: 0;
    transform: translateX(0);
}

.auth-container.right-panel-active .overlay-right {
    transform: translateX(20%);
}

.overlay-panel h2 {
    margin: 0;
    font-size: 1.9rem;
    font-family: "Poppins", "Noto Sans SC", sans-serif;
}

.overlay-panel p {
    margin: 12px 0 18px;
    line-height: 1.6;
    opacity: 0.9;
}

.ghost-btn {
    border: 1px solid rgba(255, 255, 255, 0.65);
    background: rgba(255, 255, 255, 0.12);
    color: #fff;
    border-radius: 999px;
    padding: 10px 24px;
    cursor: pointer;
    font-weight: 700;
    transition: transform 0.18s ease, background 0.18s ease;
}

.ghost-btn:hover {
    transform: translateY(-1px);
    background: rgba(255, 255, 255, 0.2);
}

@media (max-width: 960px) {
    .auth-container {
        width: min(560px, 100%);
        min-height: 760px;
    }

    .mobile-switch {
        display: grid;
    }

    .overlay-container {
        display: none;
    }

    .sign-in-container,
    .sign-up-container {
        width: 100%;
    }

    .sign-up-container {
        transform: translateX(100%);
        opacity: 0;
    }

    .auth-container.right-panel-active .sign-in-container {
        transform: translateX(-100%);
        opacity: 0;
    }

    .auth-container.right-panel-active .sign-up-container {
        transform: translateX(0);
        opacity: 1;
    }

    .auth-form {
        padding: 68px 18px 24px;
        justify-content: flex-start;
    }
}

@media (max-width: 520px) {
    .auth-portal {
        padding: 16px 10px;
    }

    .auth-container {
        min-height: 720px;
        border-radius: 16px;
    }

    .inline-row--captcha {
        grid-template-columns: 1fr 104px;
    }

    .captcha-image {
        width: 104px;
    }

    .auth-form h1 {
        font-size: 1.7rem;
    }
}
</style>
