<template>
    <div class="trade-download-page">
        <section class="download-card">
            <p class="eyebrow">Download Center</p>
            <h1>交易成功</h1>
            <p class="desc">
                原图下载已解锁。页面会自动触发浏览器下载，你也可以手动点击按钮重新下载。
            </p>
            <p class="record-line">记录ID：{{ recordId || '-' }}</p>

            <div class="actions">
                <el-button type="primary" :loading="downloading" @click="downloadNow">下载原图</el-button>
                <el-button @click="goAccount">查看交易记录</el-button>
            </div>
        </section>
    </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getToken } from '@/utils/auth';

const route = useRoute();
const router = useRouter();
const downloading = ref(false);
const recordId = computed(() => String(route.query.recordId || ''));

const downloadNow = async () => {
    if (!recordId.value) {
        ElMessage.warning('缺少交易记录ID');
        return;
    }
    const token = getToken();
    if (!token) {
        ElMessage.warning('登录已失效，请重新登录');
        router.push({ name: 'Login', query: { redirect: route.fullPath } });
        return;
    }
    downloading.value = true;
    try {
        const resp = await fetch(`/api/bridge/purchases/${recordId.value}/download`, {
            headers: {
                Authorization: `Bearer ${token}`
            }
        });
        if (!resp.ok) {
            throw new Error('下载失败，请稍后重试');
        }
        const blob = await resp.blob();
        const disposition = resp.headers.get('content-disposition') || '';
        const nameMatch = disposition.match(/filename=\"?([^\";]+)\"?/i);
        const fileName = nameMatch?.[1] || `original-${recordId.value}.jpg`;
        const objectUrl = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        URL.revokeObjectURL(objectUrl);
        ElMessage.success('已开始下载原图');
    } catch (error) {
        ElMessage.error(error?.message || '下载失败');
    } finally {
        downloading.value = false;
    }
};

const goAccount = () => {
    router.push({ name: 'Account' });
};

onMounted(() => {
    if (recordId.value) {
        downloadNow();
    }
});
</script>

<style scoped lang="scss">
.trade-download-page {
    min-height: calc(100vh - 70px);
    background: #f1f5f9;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
}

.download-card {
    width: min(560px, 100%);
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
    padding: 22px;
}

.eyebrow {
    margin: 0;
    color: #64748b;
    font-size: 12px;
    letter-spacing: 0.06em;
}

h1 {
    margin: 6px 0;
    color: #0f172a;
}

.desc {
    margin: 0;
    color: #475569;
    line-height: 1.7;
}

.record-line {
    margin: 12px 0 0;
    color: #1e293b;
    font-size: 13px;
}

.actions {
    margin-top: 16px;
    display: flex;
    gap: 10px;
}

@media (max-width: 640px) {
    .actions {
        flex-direction: column;
    }
}
</style>
