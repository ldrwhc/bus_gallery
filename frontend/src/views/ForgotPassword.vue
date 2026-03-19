<template>
    <div class="auth-page">
        <section class="auth-card">
            <h1>找回密码</h1>
            <p class="subtitle">通过邮箱验证码重置密码</p>

            <el-form class="auth-form" label-position="top" :model="form" @submit.prevent>
                <el-form-item label="用户名或邮箱">
                    <el-input v-model.trim="form.usernameOrEmail" placeholder="请输入用户名或绑定邮箱" />
                </el-form-item>
                <el-form-item v-if="captcha.required" label="图形验证码">
                    <div class="inline-row">
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
                <el-form-item label="邮箱验证码">
                    <div class="inline-row">
                        <el-input v-model.trim="form.emailCode" placeholder="输入邮箱验证码" />
                        <el-button :loading="sendingCode" :disabled="!canSendCode" @click="handleSendCode">
                            {{ sendCodeText }}
                        </el-button>
                    </div>
                </el-form-item>
                <el-form-item label="新密码">
                    <el-input
                        v-model="form.newPassword"
                        type="password"
                        placeholder="至少 8 位"
                        show-password
                        autocomplete="new-password"
                    />
                </el-form-item>
                <el-form-item label="确认新密码">
                    <el-input
                        v-model="form.confirmPassword"
                        type="password"
                        placeholder="再次输入新密码"
                        show-password
                        autocomplete="new-password"
                    />
                </el-form-item>
                <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
                <div class="actions">
                    <el-button type="primary" :loading="submitting" @click="handleSubmit">
                        重置密码
                    </el-button>
                    <router-link class="text-btn" to="/login">返回登录</router-link>
                </div>
            </el-form>
        </section>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
    issueCaptcha,
    resetForgotPassword,
    sendForgotPasswordEmailCode,
    verifyForgotPasswordEmailCode
} from '@/api/auth';

const router = useRouter();

const form = reactive({
    usernameOrEmail: '',
    challengeId: '',
    emailCode: '',
    resetTicket: '',
    newPassword: '',
    confirmPassword: '',
    captchaId: '',
    captchaCode: ''
});

const captcha = reactive({
    required: false,
    imageBase64: ''
});

const sendingCode = ref(false);
const submitting = ref(false);
const countdown = ref(0);
const localError = ref('');
let timer = null;

const canSendCode = computed(() => !sendingCode.value && countdown.value <= 0 && Boolean(form.usernameOrEmail));
const sendCodeText = computed(() => (countdown.value > 0 ? `${countdown.value}s 后重发` : '发送验证码'));
const errorMessage = computed(() => localError.value);

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

const refreshCaptcha = async () => {
    try {
        const data = await issueCaptcha('forgot');
        form.captchaId = data?.captchaId || '';
        captcha.imageBase64 = data?.imageBase64 || '';
    } catch (error) {
        localError.value = error?.message || '获取图形验证码失败';
    }
};

const handleSendCode = async () => {
    localError.value = '';
    if (!form.usernameOrEmail) {
        localError.value = '请输入用户名或邮箱';
        return;
    }
    sendingCode.value = true;
    try {
        const data = await sendForgotPasswordEmailCode({
            usernameOrEmail: form.usernameOrEmail,
            captchaId: form.captchaId,
            captchaCode: form.captchaCode
        });
        form.challengeId = data?.challengeId || '';
        startCountdown(Number(data?.resendAfterSeconds || 60));
        ElMessage.success('如果账号已绑定邮箱，验证码将发送到邮箱');
        captcha.required = false;
        captcha.imageBase64 = '';
        form.captchaId = '';
        form.captchaCode = '';
    } catch (error) {
        localError.value = error?.message || '发送验证码失败';
        if ((error?.message || '').includes('图形验证码')) {
            captcha.required = true;
            await refreshCaptcha();
        }
    } finally {
        sendingCode.value = false;
    }
};

const handleSubmit = async () => {
    localError.value = '';
    if (!form.challengeId) {
        localError.value = '请先发送验证码';
        return;
    }
    if (!form.emailCode || !form.newPassword || !form.confirmPassword) {
        localError.value = '请完整填写所有字段';
        return;
    }
    if (form.newPassword.length < 8) {
        localError.value = '密码长度至少 8 位';
        return;
    }
    if (form.newPassword !== form.confirmPassword) {
        localError.value = '两次输入的密码不一致';
        return;
    }

    submitting.value = true;
    try {
        const verifyData = await verifyForgotPasswordEmailCode({
            challengeId: form.challengeId,
            emailCode: form.emailCode
        });
        form.resetTicket = verifyData?.ticket || '';
        if (!form.resetTicket) {
            throw new Error('重置票据生成失败');
        }
        await resetForgotPassword({
            resetTicket: form.resetTicket,
            newPassword: form.newPassword,
            confirmPassword: form.confirmPassword
        });
        ElMessage.success('密码已重置，请重新登录');
        router.replace({ name: 'Login' });
    } catch (error) {
        localError.value = error?.message || '重置密码失败';
    } finally {
        submitting.value = false;
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

.captcha-image {
    width: 128px;
    height: 40px;
    object-fit: cover;
    border-radius: 10px;
    border: 1px solid #cbd5e1;
    cursor: pointer;
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
