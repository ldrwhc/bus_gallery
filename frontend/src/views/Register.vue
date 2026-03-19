<template>
    <div class="auth-page">
        <section class="auth-card">
            <h1>注册</h1>
            <p class="subtitle">创建账户以便上传、收藏与管理图片</p>

            <el-form class="auth-form" label-position="top" :model="form" @submit.prevent>
                <el-form-item label="昵称">
                    <el-input v-model.trim="form.displayName" placeholder="将在站内展示" autocomplete="nickname" />
                </el-form-item>
                <el-form-item label="用户名">
                    <el-input v-model.trim="form.username" placeholder="3-32 个字符" autocomplete="username" />
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model.trim="form.email" placeholder="用于身份验证和找回密码" autocomplete="email" />
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
                    <el-button type="primary" :loading="loading" @click="handleSubmit">
                        注册
                    </el-button>
                    <router-link class="text-btn" to="/login">已经有账号？点此登录</router-link>
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
    min-height: calc(100vh - 160px);
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 32px 16px;
}

.auth-card {
    background: #fff;
    width: min(460px, 100%);
    padding: 32px;
    border-radius: 24px;
    box-shadow: 0 20px 50px rgba(15, 23, 42, 0.1);
}

.subtitle {
    margin-top: 8px;
    color: #64748b;
}

.auth-form {
    margin-top: 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.inline-row {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 10px;
}

.actions {
    margin-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 12px;
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
    margin: -4px 0 4px;
}
</style>

