<template>
    <div class="page home-view">
        <main class="home-main constrained">
            <section class="hero">
                <div class="hero__text">
                    <p class="eyebrow">Bus Gallery</p>
                    <h1>公交车辆图库</h1>
                    <p class="description">
                        收录全国公交车辆的车牌、配置与上线资料。
                    </p>
                </div>

                <div class="hero__visual">
                    <div class="visual-card">
                        <p class="visual-title">图库收录</p>
                        <p class="visual-number">{{ pagination.total || '--' }}</p>
                        <p class="visual-caption">条车辆记录</p>
                    </div>
                </div>
            </section>

            <section class="gallery-section">
                <header class="section-header">
                    <div>
                        <h2>车辆图库</h2>
                        <p class="subtitle">当前筛选下共 {{ pagination.total }} 条</p>
                    </div>
                    <button class="ghost-btn" type="button" @click="handleResetFilters">
                        重置筛选
                    </button>
                </header>

                <div v-if="galleryLoading" class="state state--loading">
                    正在加载车辆信息...
                </div>

                <div v-else-if="galleryError" class="state state--error">
                    <p>{{ galleryError }}</p>
                    <button class="primary-btn" type="button" @click="handleRetry">
                        重新加载
                    </button>
                </div>

                <div v-else-if="!gallery.length" class="state state--empty">
                    <p>暂无符合条件的车辆</p>
                    <button class="ghost-btn" type="button" @click="handleResetFilters">
                        查看全部
                    </button>
                </div>

                <div v-else class="gallery-grid">
                    <VehicleCard v-for="item in gallery" :key="item?.vehicle?.id || item.vehicleId"
                        :vehicle="item.vehicle" :config="item.config" :images="item.images"
                        @view-detail="openVehicleDetail(item?.vehicle?.id)" />
                </div>
            </section>
        </main>

        <VehicleDetailModal v-if="isDetailVisible" :visible="isDetailVisible" :detail="activeVehicleDetail"
            :loading="activeVehicleLoading" @close="closeDetail" />
    </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import VehicleCard from '@/components/Gallery/VehicleCard.vue';
import VehicleDetailModal from '@/components/Gallery/VehicleDetailModal.vue';

const store = useStore();
const route = useRoute();
const router = useRouter();

const filters = reactive({
    regionId: null,
    companyId: null,
    brandId: null,
    modelId: null,
    keyword: ''
});

const gallery = computed(() => store.state.vehicles.gallery);
const galleryLoading = computed(() => store.state.vehicles.galleryLoading);
const galleryError = computed(() => store.state.vehicles.galleryError);
const pagination = computed(() => store.state.vehicles.pagination);

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

const mergeFilters = (payload = {}) => {
    Object.keys(filters).forEach((key) => {
        if (Object.prototype.hasOwnProperty.call(payload, key)) {
            filters[key] = payload[key];
        }
    });
};

const fetchGallery = (override = {}) => {
    mergeFilters(override);
    return store.dispatch('vehicles/loadVehicleGallery', {
        ...filters,
        ...override
    });
};

const refreshGallery = () => fetchGallery({ page: 1 });

const handleResetFilters = () => {
    Object.assign(filters, {
        regionId: null,
        companyId: null,
        brandId: null,
        modelId: null,
        keyword: ''
    });
    store.dispatch('vehicles/resetGalleryFilters');
    router.replace({ name: 'Home' });
};

const openVehicleDetail = async (vehicleId) => {
    if (!vehicleId) return;
    activeVehicleId.value = vehicleId;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', vehicleId);
    } catch (error) {
        console.error(error);
    }
};

const closeDetail = () => {
    activeVehicleId.value = null;
};

const handleRetry = () => {
    fetchGallery({ page: pagination.value.page || 1 });
};

const syncKeywordFromRoute = () => {
    const keyword = typeof route.query.keyword === 'string' ? route.query.keyword : '';
    filters.keyword = keyword;
    return keyword;
};

watch(
    () => route.query.keyword,
    (value) => {
        const keyword = typeof value === 'string' ? value : '';
        if (keyword === filters.keyword) return;
        fetchGallery({ keyword, page: 1 });
    }
);

onMounted(() => {
    const initialKeyword = syncKeywordFromRoute();
    fetchGallery({ page: 1, keyword: initialKeyword });
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
}

@media (max-width: 900px) {
    .hero {
        flex-direction: column;
        padding: 24px;
        gap: 20px;
    }

    .visual-card {
        display: none;
    }

    .gallery-section {
        padding: 24px;
    }
}

@media (max-width: 600px) {
    .hero {
        padding: 24px;
    }

    .gallery-grid {
        grid-template-columns: 1fr;
    }
}

.eyebrow {
    letter-spacing: 0.2em;
    font-size: 0.75rem;
    text-transform: uppercase;
    opacity: 0.8;
    margin-bottom: 8px;
}

.description {
    margin: 12px 0 20px;
    color: rgba(255, 255, 255, 0.8);
}

.visual-card {
    width: 100%;
    border-radius: 24px;
    padding: 32px;
    background: rgba(15, 23, 42, 0.3);
    text-align: center;
    backdrop-filter: blur(4px);
}

.visual-title {
    letter-spacing: 0.2em;
    font-size: 0.75rem;
    margin-bottom: 12px;
    opacity: 0.85;
}

.visual-number {
    font-size: 3.5rem;
    font-weight: 700;
    margin: 0;
}

.visual-caption {
    margin-top: 8px;
    color: rgba(255, 255, 255, 0.9);
}

.gallery-section {
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

.gallery-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
}

.state {
    border-radius: 18px;
    padding: 48px;
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

    &--empty {
        color: #475569;
    }

    button {
        margin-top: 16px;
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
</style>
