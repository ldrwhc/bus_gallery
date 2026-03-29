<template>
    <div class="group-goods-detail-page">
        <section class="card">
            <header class="head">
                <div>
                    <p class="eyebrow">Group Buy</p>
                    <h1>商品详情</h1>
                </div>
                <el-button @click="goBack">返回拼团大厅</el-button>
            </header>

            <div v-if="loading" class="state">正在加载商品详情...</div>
            <div v-else class="content">
                <div class="image-wrap">
                    <img v-if="displayImageUrl" :src="displayImageUrl" alt="商品展示图" />
                    <p v-else class="state">暂无可展示图片</p>
                    <span class="wm-badge">BUS GALLERY</span>
                </div>

                <div class="meta">
                    <p><span>标题</span><strong>{{ marketConfig?.goodsTitle || '图片商品' }}</strong></p>
                    <p><span>原价</span><strong>{{ centsToYuan(marketConfig?.originalPriceCents) }}</strong></p>
                    <p><span>拼团价</span><strong>{{ centsToYuan(marketConfig?.groupPriceCents) }}</strong></p>
                    <p><span>成团人数</span><strong>{{ marketConfig?.targetCount || '-' }} 人</strong></p>
                    <p><span>活动ID</span><strong>{{ activityId || '-' }}</strong></p>
                    <p><span>商品ID</span><strong>{{ goodsId || '-' }}</strong></p>
                </div>
            </div>
        </section>
    </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchImageDetail } from '@/api/images';
import { fetchTradeBridgeBindingByGoods, fetchTradeBridgeConfig } from '@/api/tradeBridge';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const marketConfig = ref(null);
const imageDetail = ref(null);
const bindingSnapshot = ref(null);

const goodsId = computed(() => String(route.query.goodsId || ''));
const activityId = computed(() => String(route.query.activityId || ''));
const imageId = computed(() => String(route.query.imageId || ''));
const displayImageUrl = computed(
    () =>
        imageDetail.value?.url ||
        imageDetail.value?.thumbnailUrl ||
        bindingSnapshot.value?.coverUrl ||
        ''
);

const centsToYuan = (cents) => `${(Number(cents || 0) / 100).toFixed(2)} 元`;

const loadPageData = async () => {
    if (!goodsId.value && !imageId.value) {
        marketConfig.value = null;
        bindingSnapshot.value = null;
        imageDetail.value = null;
        return;
    }
    loading.value = true;
    try {
        const configPromise = goodsId.value
            ? fetchTradeBridgeConfig({ goodsId: goodsId.value }).catch(() => null)
            : Promise.resolve(null);
        const bindingPromise = goodsId.value
            ? fetchTradeBridgeBindingByGoods(goodsId.value).catch(() => null)
            : Promise.resolve(null);
        const [configResp, bindingResp] = await Promise.all([configPromise, bindingPromise]);
        const resolvedImageId = Number(imageId.value || bindingResp?.imageId || 0);
        const imageResp = resolvedImageId > 0 ? await fetchImageDetail(resolvedImageId).catch(() => null) : null;

        marketConfig.value = configResp;
        bindingSnapshot.value = bindingResp;
        imageDetail.value = imageResp;

        if (!marketConfig.value && !imageDetail.value) {
            ElMessage.warning('当前商品详情不可用');
        }
    } finally {
        loading.value = false;
    }
};

const goBack = () => {
    if (window.history.length > 1) {
        router.back();
        return;
    }
    router.push({ name: 'GroupBuyMarket' });
};

watch(
    () => route.query,
    async () => {
        await loadPageData();
    },
    { immediate: true }
);
</script>

<style scoped lang="scss">
.group-goods-detail-page {
    min-height: calc(100vh - 70px);
    padding: 24px 12px 40px;
    background: #f1f5f9;
}

.card {
    width: min(980px, 100%);
    margin: 0 auto;
    border-radius: 16px;
    background: #fff;
    border: 1px solid #e2e8f0;
    box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
    padding: 18px;
}

.head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 12px;
}

.eyebrow {
    margin: 0;
    font-size: 12px;
    color: #64748b;
    letter-spacing: 0.06em;
}

h1 {
    margin: 4px 0 0;
    color: #0f172a;
}

.content {
    display: grid;
    grid-template-columns: minmax(0, 1.2fr) minmax(280px, 1fr);
    gap: 14px;
}

.image-wrap {
    position: relative;
    border-radius: 14px;
    overflow: hidden;
    border: 1px solid #e2e8f0;
    background: #0f172a;
    min-height: 260px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.image-wrap img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    display: block;
}

.wm-badge {
    position: absolute;
    right: 12px;
    bottom: 12px;
    font-size: 11px;
    color: #fff;
    background: rgba(15, 23, 42, 0.62);
    border: 1px solid rgba(255, 255, 255, 0.4);
    padding: 2px 8px;
    border-radius: 999px;
    letter-spacing: 0.04em;
}

.meta {
    display: grid;
    gap: 8px;
}

.meta p {
    margin: 0;
    border-radius: 10px;
    border: 1px solid #dbeafe;
    background: #f8fbff;
    padding: 10px;
    display: flex;
    justify-content: space-between;
    gap: 8px;
}

.meta span {
    color: #475569;
}

.meta strong {
    color: #0f172a;
    text-align: right;
}

.state {
    color: #64748b;
}

@media (max-width: 768px) {
    .content {
        grid-template-columns: 1fr;
    }
}
</style>
