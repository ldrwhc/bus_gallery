<template>
    <div class="page home-view">
        <main class="home-main constrained">
            <section class="hero">
                <div class="hero__text">
                    <p class="eyebrow">Bus Gallery</p>
                    <h1>公交车辆热点速览</h1>
                    <p class="description">热门车牌一键秒开：先取 Redis 快照，再拉增量数据，低带宽也能快。</p>
                    <div class="hero__actions">
                        <router-link class="primary-btn" to="/gallery">进入图库</router-link>
                        <button class="ghost-btn" type="button" @click="refreshHot">刷新热门</button>
                    </div>
                </div>
                <div class="hero__visual">
                    <div class="visual-card">
                        <p class="visual-title">今日快照</p>
                        <p class="visual-number">{{ hotSnapshots.length || '--' }}</p>
                        <p class="visual-caption">热门车牌</p>
                    </div>
                </div>
            </section>

            <section class="hot-section">
                <header class="section-header">
                    <div>
                        <h2>热门图片 / 车牌快照</h2>
                        <p class="subtitle">来自 Redis big key 的预渲染快照，点击立即打开详情</p>
                    </div>
                    <router-link class="ghost-btn" to="/gallery">查看全部图库</router-link>
                </header>

                <div v-if="hotLoading" class="state state--loading">正在拉取热门快照...</div>
                <div v-else-if="hotError" class="state state--error">{{ hotError }}</div>
                <div v-else-if="!hotSnapshots.length" class="state state--empty">暂无热门车牌，稍后再试</div>

                <div v-else class="hot-grid">
                    <article v-for="item in hotSnapshots" :key="item.plateNumber" class="hot-card" @click="openSnapshot(item)">
                        <div class="hot-card__cover">
                            <img :src="firstImage(item)?.thumbnailUrl || firstImage(item)?.url || fallback" :alt="item.plateNumber" />
                            <span class="badge">{{ item.plateNumber }}</span>
                        </div>
                        <div class="hot-card__body">
                            <p class="plate">{{ item.plateNumber }}</p>
                            <p class="meta">变体 {{ item.variants?.length || 0 }} · 评论 {{ item.comments?.length || 0 }} · 收藏 {{ item.favoriteSummary?.total || 0 }}</p>
                            <p class="recommend" v-if="item.recommendations?.length">同公司推荐：{{ item.recommendations.length }}</p>
                        </div>
                    </article>
                </div>
            </section>
        </main>

        <VehicleDetailModal v-if="isDetailVisible" :visible="isDetailVisible" :detail="activeVehicleDetail"
            :loading="activeVehicleLoading" @close="closeDetail" />
    </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useStore } from 'vuex';
import VehicleDetailModal from '@/components/Gallery/VehicleDetailModal.vue';
import { fetchHotSnapshots } from '@/api/snapshots';
import { FALLBACK_IMAGE } from '@/utils/constants';

const store = useStore();
const hotSnapshots = ref([]);
const hotLoading = ref(false);
const hotError = ref('');
const fallback = FALLBACK_IMAGE;

const activeVehicleId = ref(null);
const activeVehicleDetail = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] || null : null
);
const activeVehicleLoading = computed(
    () =>
        (activeVehicleId.value && store.state.vehicles.detailLoadingMap[activeVehicleId.value]) ||
        false
);
const isDetailVisible = computed(() => Boolean(activeVehicleId.value));

const firstImage = (snapshot) => snapshot?.variants?.[0]?.images?.[0] || null;
const firstVehicleId = (snapshot) => snapshot?.variants?.[0]?.vehicle?.id || null;

const loadHot = async () => {
    hotLoading.value = true;
    hotError.value = '';
    try {
        const res = await fetchHotSnapshots(6);
        hotSnapshots.value = Array.isArray(res) ? res : res?.data || [];
    } catch (e) {
        hotError.value = '获取热门快照失败，请稍后再试';
    } finally {
        hotLoading.value = false;
    }
};

const refreshHot = () => loadHot();

const openSnapshot = async (snapshot) => {
    const vid = firstVehicleId(snapshot);
    if (!vid) return;
    activeVehicleId.value = vid;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', vid);
    } catch (err) {
        console.error(err);
    }
};

const closeDetail = () => {
    activeVehicleId.value = null;
};

onMounted(() => {
    loadHot();
});
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f7fb;
}

.constrained {
    width: min(1200px, 100%);
    margin: 0 auto;
    flex: 1;
    padding: clamp(32px, 5vw, 72px) clamp(16px, 4vw, 32px) clamp(96px, 8vw, 120px);
    box-sizing: border-box;
}

.hero {
    background: radial-gradient(circle at top left, #e0f2fe, #c7d2fe);
    border-radius: 32px;
    padding: 40px;
    color: #fff;
    display: flex;
    gap: 40px;
    align-items: center;
    margin-bottom: 32px;

    &__text {
        flex: 1;
    }

    &__visual {
        flex: 0 0 280px;
        display: flex;
        justify-content: center;
    }

    &__actions {
        display: flex;
        gap: 12px;
        margin-top: 20px;
        flex-wrap: wrap;
    }
}

.hot-section {
    background: #fff;
    border-radius: 24px;
    padding: 32px;
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;

    .subtitle {
        color: #6b7280;
        margin-top: 4px;
    }
}

.hot-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
}

.hot-card {
    border-radius: 16px;
    background: #f8fafc;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0 10px 20px rgba(15, 23, 42, 0.06);
    transition: transform 0.15s ease, box-shadow 0.15s ease;

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 14px 28px rgba(15, 23, 42, 0.1);
    }

    &__cover {
        position: relative;
        aspect-ratio: 4 / 3;
        background: #e2e8f0;

        img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }

        .badge {
            position: absolute;
            left: 12px;
            top: 12px;
            background: rgba(15, 23, 42, 0.8);
            color: #fff;
            padding: 4px 10px;
            border-radius: 999px;
            font-size: 0.85rem;
        }
    }

    &__body {
        padding: 12px 14px 14px;
        display: flex;
        flex-direction: column;
        gap: 6px;
    }
}

.plate {
    font-weight: 700;
    color: #0f172a;
}

.meta {
    color: #475569;
    font-size: 0.9rem;
}

.recommend {
    color: #1d4ed8;
    font-size: 0.9rem;
}

.state {
    border-radius: 18px;
    padding: 32px;
    text-align: center;
    background: #f8fafc;
    color: #475569;

    &--loading {
        color: #2563eb;
    }

    &--error {
        background: #fee2e2;
        color: #b91c1c;
    }
}

.primary-btn,
.ghost-btn {
    border: none;
    border-radius: 999px;
    padding: 10px 18px;
    cursor: pointer;
    font-weight: 600;
    transition: all 0.2s;
}

.primary-btn {
    background: #2563eb;
    color: #fff;

    &:hover {
        background: #1d4ed8;
    }
}

.ghost-btn {
    background: rgba(37, 99, 235, 0.1);
    color: #2563eb;

    &:hover {
        background: rgba(37, 99, 235, 0.2);
    }
}

@media (max-width: 900px) {
    .hero {
        flex-direction: column;
        padding: 24px;
        gap: 20px;
    }

    .hot-section {
        padding: 24px;
    }
}
</style>
