<template>
    <div class="auth-page">
        <section class="auth-card">
            <h1>登录</h1>
            <p class="subtitle">使用账号登录以管理上传记录</p>

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
                    <el-button type="primary" :loading="loading" @click="handleSubmit">
                        登录
                    </el-button>
                    <router-link class="text-btn" to="/forgot-password">忘记密码？</router-link>
                    <router-link class="text-btn" to="/register">还没有账号？点此注册</router-link>
                </div>
            </el-form>
        </section>
    </div>
</template>

<script setup>
import { reactive, computed } from 'vue';
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
    if (!form.username || !form.password) {
        return;
    }
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
        // keep silent, error displayed by request interceptor
    }
};
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
</style>
