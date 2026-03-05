<template>
    <div class="page home-view">
        <AppHeader />

        <main class="home-main constrained">
            <section class="hero">
                <div class="hero__text">
                    <p class="eyebrow">Bus Gallery</p>
                    <h1>全国公交车辆图鉴</h1>
                    <p class="description">
                        汇聚中国各地公交车辆的车牌、配置、运营信息与实拍照片，支持按地区、公司、品牌、型号随心筛选。
                    </p>

                    <SearchBar placeholder="输入车型 / 车牌 / 公司搜索" :value="filters.keyword" @search="handleKeywordSearch" />
                </div>

                <div class="hero__visual">
                    <div class="visual-card">
                        <p class="visual-title">图库收录</p>
                        <p class="visual-number">{{ pagination.total || '--' }}</p>
                        <p class="visual-caption">辆车辆档案</p>
                    </div>
                </div>
            </section>

            <section class="filter-section">
                <FilterPanel :filters="filters" :regions="regionOptions" :companies="companyOptions"
                    :brands="brandOptions" :models="modelOptions" @change="handleFilterChange"
                    @reset="handleResetFilters" />
            </section>

            <section class="gallery-section">
                <header class="section-header">
                    <div>
                        <h2>车辆图库</h2>
                        <p class="subtitle">当前筛选下共 {{ pagination.total }} 辆</p>
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

        <AppFooter />
    </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue';
import { useStore } from 'vuex';
import AppHeader from '@/components/Layout/AppHeader.vue';
import AppFooter from '@/components/Layout/AppFooter.vue';
import FilterPanel from '@/components/Filters/FilterPanel.vue';
import SearchBar from '@/components/Filters/SearchBar.vue';
import VehicleCard from '@/components/Gallery/VehicleCard.vue';
import VehicleDetailModal from '@/components/Gallery/VehicleDetailModal.vue';

const store = useStore();

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

const regionOptions = computed(() => store.getters['regions/regionOptions'] || []);
const companyOptions = computed(() => store.getters['companies/companyOptions'] || []);
const brandOptions = computed(() => store.getters['brands/brandOptions'] || []);
const modelOptions = computed(() => store.getters['models/modelOptions'] || []);

const activeVehicleId = ref(null);
const activeVehicleDetail = computed(() =>
    activeVehicleId.value
        ? store.state.vehicles.detailMap[activeVehicleId.value] || null
        : null
);
const activeVehicleLoading = computed(
    () =>
        (activeVehicleId.value &&
            store.state.vehicles.detailLoadingMap[activeVehicleId.value]) ||
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

const handleFilterChange = (payload) => {
    fetchGallery({ ...payload, page: 1 });
};

const handleKeywordSearch = (keyword) => {
    fetchGallery({ keyword, page: 1 });
};

const handleResetFilters = () => {
    Object.assign(filters, {
        regionId: null,
        companyId: null,
        brandId: null,
        modelId: null,
        keyword: ''
    });
    store.dispatch('vehicles/resetGalleryFilters');
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

onMounted(() => {
    store.dispatch('regions/loadRegions');
    store.dispatch('companies/loadCompanies');
    store.dispatch('brands/loadBrands');
    store.dispatch('models/loadModels');
    fetchGallery({ page: 1 });
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
    padding: 32px 24px 72px;
}

.hero {
    background: radial-gradient(circle at top left, #2563eb, #0f172a);
    border-radius: 32px;
    padding: 48px;
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

.filter-section {
    margin-bottom: 24px;
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