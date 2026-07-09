<template>
    <div class="page home-view">
        <main class="home-main">
            <section class="hero">
                <p class="eyebrow">Bus Gallery</p>
                <h1>图库搜集</h1>
                <form class="hero-search" @submit.prevent="handleSearch">
                    <input v-model.trim="searchKeyword" type="text" placeholder="搜索车牌 / 车型 / 公司 / 地区" />
                    <button class="search-submit" type="submit">搜索图库</button>
                </form>
                <div class="hero__actions">
                    <router-link class="primary-btn" to="/gallery">进入图库</router-link>
                    <button class="ghost-btn" type="button" @click="reloadList">刷新首页</button>
                </div>
            </section>

            <section class="hot-section">
                <header class="section-header">
                    <div class="section-title">
                        <h2>热门图片</h2>
                    </div>
                </header>

                <div v-if="initialLoading" class="state state--loading">正在加载首页内容...</div>
                <div v-else-if="loadError" class="state state--error">{{ loadError }}</div>
                <div v-else-if="!waterfallCards.length" class="state state--empty">暂无车辆数据</div>

                <div v-else class="waterfall">
                    <article
                        v-for="(card, index) in waterfallCards"
                        :key="card.key"
                        class="waterfall-card"
                        @click="openCard(card)"
                    >
                        <div class="waterfall-media">
                            <img
                                :src="card.image?.thumbnailUrl || fallback"
                                :alt="card.plateNumber || '车辆图片'"
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

                <div ref="loadMoreTrigger" class="load-trigger" />
                <div v-if="loadingMore" class="load-more">正在加载更多...</div>
                <div v-else-if="noMore && waterfallCards.length" class="load-more load-more--end">已加载全部内容</div>
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
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { fetchVehicleGallery } from '@/api/vehicles';
import { FALLBACK_IMAGE } from '@/utils/constants';

const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));
const PAGE_SIZE = 10;

const store = useStore();
const router = useRouter();

const fallback = FALLBACK_IMAGE;
const searchKeyword = ref('');
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1280);

const activeVehicleId = ref(null);
const activeImageId = ref(null);
const activeVehicleDetail = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] || null : null
);
const activeVehicleLoading = computed(
    () => (activeVehicleId.value && store.state.vehicles.detailLoadingMap[activeVehicleId.value]) || false
);
const isDetailVisible = computed(() => Boolean(activeVehicleId.value));

const rawRecords = ref([]);
const initialLoading = ref(false);
const loadingMore = ref(false);
const noMore = ref(false);
const loadError = ref('');
const nextLaunch = ref(null);
const nextId = ref(null);
const loadMoreTrigger = ref(null);
const loadObserver = ref(null);

const isMobileViewport = computed(() => viewportWidth.value <= 768);
const eagerImageCount = computed(() => (isMobileViewport.value ? 3 : 9));

const waterfallCards = computed(() =>
    rawRecords.value.map((record, index) => {
        const vehicle = record?.vehicle || {};
        const image = Array.isArray(record?.images) && record.images.length ? record.images[0] : null;
        return {
            key: `${vehicle?.id || 'vehicle'}-${index}`,
            plateNumber: vehicle?.plateNumber || '',
            vehicleId: vehicle?.id || null,
            image,
            author: image?.uploaderDisplayName || image?.uploaderUsername || '匿名上传',
            region:
                vehicle?.region?.name ||
                vehicle?.company?.regionName ||
                vehicle?.company?.name ||
                '地区待补充'
        };
    })
);

const formatPlate = (plate = '') => {
    const value = String(plate || '').trim();
    if (!value) return value;
    const match = value.match(/^(.{2})(.{5,6})$/u);
    if (match) return `${match[1]} ${match[2]}`;
    if (value.length > 2) return `${value.slice(0, 2)} ${value.slice(2)}`;
    return value;
};

const buildLoadParams = () => {
    const params = { size: PAGE_SIZE };
    if (nextLaunch.value) {
        params.lastLaunch = nextLaunch.value;
    }
    if (nextId.value) {
        params.lastId = nextId.value;
    }
    return params;
};

const loadMore = async () => {
    if (loadingMore.value || initialLoading.value || noMore.value) {
        return;
    }
    loadingMore.value = true;
    loadError.value = '';
    try {
        const response = await fetchVehicleGallery(buildLoadParams());
        const incoming = Array.isArray(response?.records) ? response.records : [];
        if (!incoming.length) {
            noMore.value = true;
            return;
        }
        const knownIds = new Set(rawRecords.value.map((item) => item?.vehicle?.id).filter(Boolean));
        const uniqueIncoming = incoming.filter((item) => {
            const id = item?.vehicle?.id;
            if (!id) return true;
            if (knownIds.has(id)) return false;
            knownIds.add(id);
            return true;
        });
        rawRecords.value = [...rawRecords.value, ...uniqueIncoming];
        nextLaunch.value = response?.nextLaunch ?? null;
        nextId.value = response?.nextId ?? null;
        if (!nextLaunch.value && !nextId.value) {
            noMore.value = true;
        }
    } catch (error) {
        loadError.value = error?.message || '加载首页失败，请稍后重试';
    } finally {
        loadingMore.value = false;
    }
};

const loadInitial = async () => {
    initialLoading.value = true;
    loadError.value = '';
    rawRecords.value = [];
    noMore.value = false;
    nextLaunch.value = null;
    nextId.value = null;
    try {
        await loadMore();
    } finally {
        initialLoading.value = false;
    }
};

const setupInfiniteObserver = async () => {
    if (typeof window === 'undefined') {
        return;
    }
    await nextTick();
    if (!loadMoreTrigger.value) {
        return;
    }
    if (loadObserver.value) {
        loadObserver.value.disconnect();
    }
    loadObserver.value = new IntersectionObserver(
        (entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    loadMore();
                }
            });
        },
        {
            root: null,
            rootMargin: '320px',
            threshold: 0.01
        }
    );
    loadObserver.value.observe(loadMoreTrigger.value);
};

const reloadList = async () => {
    await loadInitial();
    await setupInfiniteObserver();
};

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
            plateNumber: card.plateNumber,
            force: true
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

onMounted(async () => {
    handleResize();
    if (typeof window !== 'undefined') {
        window.addEventListener('resize', handleResize, { passive: true });
    }
    await loadInitial();
    await setupInfiniteObserver();
});

onBeforeUnmount(() => {
    if (typeof window !== 'undefined') {
        window.removeEventListener('resize', handleResize);
    }
    if (loadObserver.value) {
        loadObserver.value.disconnect();
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
}

.eyebrow {
    margin: 0;
    font-size: 0.86rem;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    font-weight: 700;
    opacity: 0.88;
}

.hero h1 {
    margin: 4px 0 12px;
    font-size: clamp(1.9rem, 4vw, 2.9rem);
    line-height: 1.08;
}

.hero-search {
    width: min(860px, 100%);
    background: rgba(255, 255, 255, 0.94);
    border-radius: 999px;
    padding: 6px;
    display: flex;
    gap: 8px;
    align-items: center;
}

.hero-search input {
    flex: 1;
    border: none;
    background: transparent;
    padding: 12px 16px;
    font-size: 1rem;
    color: #0f172a;
    outline: none;
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

.primary-btn,
.ghost-btn {
    border: none;
    border-radius: 999px;
    padding: 9px 16px;
    font-weight: 600;
    text-decoration: none;
    cursor: pointer;
}

.primary-btn {
    color: #fff;
    background: #0f172a;
}

.ghost-btn {
    color: #0f172a;
    background: rgba(255, 255, 255, 0.82);
}

.hot-section {
    margin-top: 4px;
}

.section-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    margin-bottom: 12px;
}

.section-title h2 {
    margin: 0;
    font-size: clamp(1.3rem, 2.5vw, 1.8rem);
    color: #0f172a;
}

.state {
    border-radius: 14px;
    padding: 24px;
    text-align: center;
    background: #fff;
    color: #475569;
}

.state--loading {
    color: #2563eb;
}

.state--error {
    color: #b91c1c;
    background: #fee2e2;
}

.waterfall {
    width: 100%;
    column-count: 3;
    column-gap: clamp(12px, 1.8vw, 18px);
}

.waterfall-card {
    display: inline-block;
    width: 100%;
    margin: 0 0 clamp(12px, 1.8vw, 18px);
    break-inside: avoid;
    background: #fff;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
    cursor: pointer;
    transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.waterfall-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 14px 26px rgba(15, 23, 42, 0.15);
}

.waterfall-media {
    position: relative;
    background: #e2e8f0;
}

.waterfall-media img {
    width: 100%;
    height: auto;
    display: block;
}

.waterfall-tag,
.watermark-tag {
    position: absolute;
    right: 10px;
    bottom: 10px;
    font-size: 0.62rem;
    letter-spacing: 0.08em;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.7);
    text-shadow: 0 1px 4px rgba(15, 23, 42, 0.6);
}

.waterfall-overlay {
    position: absolute;
    inset: auto 0 0 0;
    padding: 10px 10px 12px;
    background: linear-gradient(180deg, rgba(15, 23, 42, 0), rgba(15, 23, 42, 0.82));
}

.plate-badge {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    padding: 3px 9px;
    font-size: 0.72rem;
    font-weight: 600;
    background: rgba(255, 255, 255, 0.18);
    color: #fff;
}

.meta-line {
    margin: 8px 0 0;
    font-size: 0.8rem;
    color: rgba(255, 255, 255, 0.92);
}

.load-trigger {
    width: 100%;
    height: 1px;
}

.load-more {
    margin-top: 14px;
    text-align: center;
    color: #64748b;
}

.load-more--end {
    color: #94a3b8;
}

@media (max-width: 1200px) {
    .waterfall {
        column-count: 2;
    }
}

@media (max-width: 900px) {
    .waterfall {
        column-count: 2;
    }
}

@media (max-width: 560px) {
    .hero-search {
        border-radius: 14px;
        padding: 8px;
        flex-direction: column;
        align-items: stretch;
    }

    .search-submit {
        width: 100%;
    }

    .waterfall {
        column-count: 1;
    }
}
</style>
