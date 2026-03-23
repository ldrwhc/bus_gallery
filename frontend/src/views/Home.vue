<template>
    <div class="page home-view">
        <main class="home-main">
            <section class="hero">
                <p class="eyebrow">Bus Gallery</p>
                <h1>图库收集</h1>
                <p class="hero__desc">快速检索，探索图库。</p>
                <form class="hero-search" @submit.prevent="handleSearch">
                    <input v-model.trim="searchKeyword" type="text" placeholder="搜索车牌 / 车型 / 公司 / 地区" />
                    <button class="search-submit" type="submit">搜索图库</button>
                </form>
                <div class="hero__actions">
                    <router-link class="primary-btn" to="/gallery">进入图库</router-link>
                    <button class="ghost-btn" type="button" @click="refreshHot">刷新热门</button>
                </div>
            </section>

            <section class="hot-section">
                <header class="section-header">
                    <div class="section-title">
                        <h2>热门图片</h2>
                        <router-link class="ghost-btn ghost-btn--inline" to="/gallery">查看全部</router-link>
                    </div>
                </header>

                <div v-if="hotLoading" class="state state--loading">正在拉取热门快照...</div>
                <div v-else-if="hotError" class="state state--error">{{ hotError }}</div>
                <div v-else-if="!waterfallCards.length" class="state state--empty">暂无热门图片，稍后再试</div>

                <div v-else class="waterfall">
                    <article
                        v-for="(card, index) in waterfallCards"
                        :key="card.key"
                        class="waterfall-card"
                        @click="openCard(card)"
                    >
                        <div class="waterfall-media">
                            <img
                                :src="card.image?.thumbnailUrl || card.image?.url || fallback"
                                :alt="card.plateNumber || '公交图片'"
                                :loading="index < eagerImageCount ? 'eager' : 'lazy'"
                                :fetchpriority="index < eagerImageCount ? 'high' : 'auto'"
                                decoding="async"
                            />
                            <span class="watermark-tag">BUS GALLERY</span>
                            <div class="waterfall-overlay">
                                <div class="waterfall-meta">
                                    <span class="plate-badge">{{ formatPlate(card.plateNumber) || '未上牌' }}</span>
                                    <p class="meta-line">{{ card.author }} · {{ card.region }}</p>
                                </div>
                            </div>
                        </div>
                    </article>
                </div>
            </section>
        </main>

        <VehicleDetailModal
            v-if="isDetailVisible"
            :visible="isDetailVisible"
            :detail="activeVehicleDetail"
            :initial-image-id="activeImageId"
            :loading="activeVehicleLoading"
            @close="closeDetail"
        />
    </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { fetchHotSnapshots } from '@/api/snapshots';
import { FALLBACK_IMAGE } from '@/utils/constants';

const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));

const store = useStore();
const router = useRouter();
const hotSnapshots = ref([]);
const hotLoading = ref(false);
const hotError = ref('');
const fallback = FALLBACK_IMAGE;
const searchKeyword = ref('');
const activeImageId = ref(null);
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1280);

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
const isMobileViewport = computed(() => viewportWidth.value <= 768);
const eagerImageCount = computed(() => (isMobileViewport.value ? 3 : 9));
const hotSnapshotSize = computed(() => (isMobileViewport.value ? 8 : 12));
const maxCardCount = computed(() => {
    if (viewportWidth.value <= 420) return 16;
    if (viewportWidth.value <= 768) return 24;
    return 72;
});

const firstVariant = (snapshot) => snapshot?.variants?.[0] || null;
const firstVehicleId = (snapshot) => firstVariant(snapshot)?.vehicle?.id || null;

const waterfallCards = computed(() => {
    const cards = [];
    hotSnapshots.value.forEach((snapshot, sIdx) => {
        const variants = Array.isArray(snapshot?.variants) && snapshot.variants.length ? snapshot.variants : [null];
        variants.forEach((variant, vIdx) => {
            const vehicle = variant?.vehicle || null;
            const images = Array.isArray(variant?.images) && variant.images.length ? variant.images : [null];
            const displayImages = isMobileViewport.value ? images.slice(0, 1) : images;
            displayImages.forEach((image, iIdx) => {
                cards.push({
                    key: `${snapshot?.plateNumber || 'unknown'}-${sIdx}-${vehicle?.id || 'v'}-${vIdx}-${image?.id || iIdx}`,
                    plateNumber: snapshot?.plateNumber || vehicle?.plateNumber || '',
                    vehicleId: vehicle?.id || firstVehicleId(snapshot),
                    image,
                    author: image?.uploaderDisplayName || image?.uploaderUsername || '匿名上传',
                    region:
                        vehicle?.region?.name ||
                        vehicle?.company?.regionName ||
                        vehicle?.company?.name ||
                        '地区待补充'
                });
            });
        });
    });
    return cards.slice(0, maxCardCount.value);
});

const formatPlate = (plate = '') => {
    const value = String(plate || '').trim();
    if (!value) return value;
    const match = value.match(/^(.{2})(.{5,6})$/u);
    if (match) return `${match[1]} ${match[2]}`;
    if (value.length > 2) return `${value.slice(0, 2)} ${value.slice(2)}`;
    return value;
};

const loadHot = async () => {
    hotLoading.value = true;
    hotError.value = '';
    try {
        const res = await fetchHotSnapshots(hotSnapshotSize.value);
        hotSnapshots.value = Array.isArray(res) ? res : res?.data || [];
    } catch (error) {
        const message = String(error?.message || '');
        if (message.toLowerCase().includes('timeout')) {
            try {
                const retry = await fetchHotSnapshots(4);
                hotSnapshots.value = Array.isArray(retry) ? retry : retry?.data || [];
                hotError.value = '';
                return;
            } catch (retryError) {
                hotError.value = '热门数据加载超时，请稍后再试';
            }
        } else {
            hotError.value = '获取热门快照失败，请稍后再试';
        }
    } finally {
        hotLoading.value = false;
    }
};

const refreshHot = () => loadHot();

const handleSearch = () => {
    const keyword = searchKeyword.value.trim();
    router.push(keyword ? { name: 'Gallery', query: { keyword } } : { name: 'Gallery' });
};

const openCard = async (card) => {
    if (!card?.vehicleId) return;
    activeImageId.value = card?.image?.id || null;
    activeVehicleId.value = card.vehicleId;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', {
            vehicleId: card.vehicleId,
            plateNumber: card.plateNumber
        });
    } catch (error) {
        console.error(error);
    }
};

const closeDetail = () => {
    activeVehicleId.value = null;
    activeImageId.value = null;
};

const handleResize = () => {
    if (typeof window === 'undefined') return;
    viewportWidth.value = window.innerWidth;
};

onMounted(() => {
    handleResize();
    if (typeof window !== 'undefined') {
        window.addEventListener('resize', handleResize, { passive: true });
    }
    loadHot();
});

onBeforeUnmount(() => {
    if (typeof window !== 'undefined') {
        window.removeEventListener('resize', handleResize);
    }
});
</script>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background:
        radial-gradient(circle at 20% -10%, rgba(56, 189, 248, 0.18), transparent 45%),
        radial-gradient(circle at 90% 0%, rgba(14, 165, 233, 0.14), transparent 40%),
        #f6f8fc;
}

.home-main {
    width: 100%;
    margin: 0;
    flex: 1;
    padding: clamp(30px, 3.5vw, 40px) clamp(20px, 3vw, 38px) 84px;
    box-sizing: border-box;
}

.hero {
    position: relative;
    overflow: hidden;
    margin-bottom: 40px;
    border-radius: 16px;
    padding: clamp(26px, 3.2vw, 44px);
    color: #fff;
    background: linear-gradient(130deg, #0b4aa5 0%, #1d4ed8 45%, #0ea5e9 100%);
    box-shadow: 0 18px 36px rgba(30, 64, 175, 0.28);

    &::after {
        content: '';
        position: absolute;
        inset: -35% -5% auto auto;
        width: 360px;
        height: 360px;
        border-radius: 999px;
        background: radial-gradient(circle, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0));
        pointer-events: none;
    }

    h1 {
        margin: 4px 0 12px;
        font-size: clamp(1.9rem, 4vw, 2.9rem);
        line-height: 1.08;
        letter-spacing: 0.01em;
    }
}

.eyebrow {
    margin: 0;
    font-size: 0.86rem;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    font-weight: 700;
    opacity: 0.88;
}

.hero__desc {
    margin: 0 0 18px;
    max-width: 760px;
    color: rgba(255, 255, 255, 0.92);
}

.hero-search {
    width: min(860px, 100%);
    background: rgba(255, 255, 255, 0.94);
    border-radius: 999px;
    padding: 6px;
    display: flex;
    gap: 8px;
    align-items: center;

    input {
        flex: 1;
        border: none;
        background: transparent;
        padding: 12px 16px;
        font-size: 1rem;
        color: #0f172a;
        outline: none;
    }
}

.search-submit {
    border: none;
    border-radius: 999px;
    background: #0f172a;
    color: #fff;
    padding: 10px 16px;
    font-weight: 600;
    cursor: pointer;
}

.hero__actions {
    display: flex;
    gap: 10px;
    margin-top: 14px;
    flex-wrap: wrap;
}

.hot-section {
    background: transparent;
    padding: 0;
    margin-top: 6px;
}

.section-header {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    margin-bottom: 18px;
}

.section-title {
    display: inline-flex;
    align-items: center;
    gap: 10px;
}

.section-title h2 {
    margin: 0;
}

.waterfall {
    column-count: 3;
    column-gap: 14px;
}

.waterfall-card {
    margin: 0 0 14px;
    break-inside: avoid;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
    content-visibility: auto;
    contain-intrinsic-size: 360px;
}

.waterfall-media {
    position: relative;
}

.waterfall-media img {
    width: 100%;
    height: auto;
    display: block;
}

.waterfall-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: flex-end;
    background: linear-gradient(180deg, rgba(15, 23, 42, 0) 45%, rgba(15, 23, 42, 0.82) 100%);
    opacity: 0;
    transform: translateY(6px);
    transition: opacity 0.2s ease, transform 0.2s ease;
}

.watermark-tag {
    position: absolute;
    right: 10px;
    bottom: 10px;
    z-index: 1;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: rgba(255, 255, 255, 0.62);
    text-shadow: 0 1px 6px rgba(15, 23, 42, 0.58);
    pointer-events: none;
    user-select: none;
}

.waterfall-card:hover .waterfall-overlay {
    opacity: 1;
    transform: translateY(0);
}

.waterfall-meta {
    width: 100%;
    padding: 10px;
    color: #fff;
}

.plate-badge {
    display: inline-block;
    border-radius: 999px;
    padding: 4px 8px;
    font-size: 0.8rem;
    font-weight: 700;
    background: rgba(15, 23, 42, 0.55);
}

.meta-line {
    margin: 8px 0 0;
    font-size: 0.84rem;
    color: rgba(255, 255, 255, 0.92);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.state {
    border-radius: 12px;
    padding: 22px;
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
    padding: 8px 15px;
    cursor: pointer;
    font-weight: 600;
    transition: all 0.2s;
    text-decoration: none;
}

.primary-btn {
    background: #ffffff;
    color: #0f172a;

    &:hover {
        background: rgba(255, 255, 255, 0.9);
    }
}

.ghost-btn {
    background: rgba(255, 255, 255, 0.18);
    color: #fff;

    &:hover {
        background: rgba(255, 255, 255, 0.28);
    }
}

.ghost-btn--inline {
    padding: 6px 12px;
    background: rgba(37, 99, 235, 0.12);
    color: #1d4ed8;
}

@media (max-width: 1100px) {
    .waterfall {
        column-count: 2;
    }
}

@media (max-width: 768px) {
    .home-main {
        padding: 24px 16px 34px;
    }

    .hero {
        margin-bottom: 26px;
        padding: 18px 14px;
        border-radius: 12px;
    }

    .hero-search {
        border-radius: 14px;
        flex-direction: column;
        align-items: stretch;
    }

    .search-submit {
        width: 100%;
    }

    .waterfall {
        column-count: 1;
    }

    .waterfall-overlay {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
