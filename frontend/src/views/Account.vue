<template>
    <div class="account-page">
        <section class="profile-card" v-if="profile">
            <div class="profile-info">
                <div class="avatar">{{ avatarInitial }}</div>
                <div>
                    <h1>{{ profile.displayName }}</h1>
                    <p class="muted">{{ '@' + (profile.username || '') }}</p>
                    <p class="muted">累计上传：{{ profile.uploadsCount }} 张</p>
                </div>
            </div>
            <el-button type="primary" @click="goUpload">继续上传</el-button>
        </section>

        <section class="gallery-card">
            <header class="gallery-header">
                <div>
                    <h2>我的图片</h2>
                    <p class="muted">点击卡片可查看车辆详情</p>
                </div>
                <el-pagination
                    layout="prev, pager, next"
                    background
                    :page-size="pagination.size"
                    :current-page="pagination.page"
                    :total="pagination.total"
                    @current-change="handlePageChange"
                />
            </header>

            <div v-if="loading" class="state">加载中...</div>
            <div v-else-if="!images.length" class="state">还没有上传任何图片</div>
            <div v-else class="image-grid">
                <article v-for="image in images" :key="image.id" class="image-card" @click="openImage(image)">
                    <img
                        :src="image.thumbnailUrl"
                        :alt="image.objectName || 'upload'"
                        loading="lazy"
                        decoding="async"
                    />
                    <p class="muted">{{ formatDate(image.createTime) }}</p>
                </article>
            </div>
        </section>

        <VehicleDetailModal
            v-if="detailVisible"
            :visible="detailVisible"
            :detail="activeVehicleDetail"
            :loading="detailLoading"
            @close="closeVehicleDetail"
        />
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, defineAsyncComponent } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchMyImages } from '@/api/users';
import { formatDate } from '@/utils/formatters';
const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));

const store = useStore();
const router = useRouter();

const profile = computed(() => store.state.auth.profile);
const avatarInitial = computed(() => {
    const name = profile.value?.displayName?.trim();
    return name ? name.slice(0, 1).toUpperCase() : '';
});
const images = ref([]);
const loading = ref(false);
const pagination = reactive({
    page: 1,
    size: 12,
    total: 0
});

const activeVehicleId = ref(null);

const detailVisible = computed(() => Boolean(activeVehicleId.value));
const activeVehicleDetail = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] : null
);
const detailLoading = computed(
    () =>
        (activeVehicleId.value && store.state.vehicles.detailLoadingMap[activeVehicleId.value]) ||
        false
);

const loadProfile = async () => {
    if (!profile.value) {
        await store.dispatch('auth/fetchProfile');
    }
};

const loadImages = async () => {
    loading.value = true;
    try {
        const data = await fetchMyImages({ page: pagination.page, size: pagination.size });
        images.value = data.records || [];
        pagination.total = data.total || 0;
    } finally {
        loading.value = false;
    }
};

const handlePageChange = (page) => {
    pagination.page = page;
    loadImages();
};

const openImage = async (image) => {
    if (image?.vehicleId) {
        activeVehicleId.value = image.vehicleId;
        try {
            await store.dispatch('vehicles/loadVehicleDetail', { vehicleId: image.vehicleId, force: true });
            return;
        } catch (error) {
            console.error(error);
            ElMessage.error('车辆详情加载失败，请稍后重试。');
            activeVehicleId.value = null;
            return;
        }
    }
    ElMessage.info('该图片暂未关联车辆，稍后可在详情页补充。');
};

const closeVehicleDetail = () => {
    activeVehicleId.value = null;
};

const goUpload = () => {
    router.push({ name: 'Upload' });
};

onMounted(async () => {
    await loadProfile();
    await loadImages();
});
</script>

<style scoped lang="scss">
.account-page {
    width: min(1100px, 100%);
    margin: 0 auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.profile-card,
.gallery-card {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.profile-info {
    display: flex;
    align-items: center;
    gap: 16px;

    h1 {
        margin: 0;
    }
}

.avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #2563eb;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 1.5rem;
}

.muted {
    color: #6b7280;
    margin: 4px 0;
}

.gallery-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
}

.image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
    margin-top: 16px;
}

.image-card {
    border-radius: 16px;
    overflow: hidden;
    background: #f8fafc;
    cursor: pointer;
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    border: 1px solid transparent;
    transition: transform 0.15s ease, border-color 0.15s ease;

    img {
        width: 100%;
        height: 140px;
        object-fit: cover;
        border-radius: 12px;
    }

    &:hover {
        border-color: #2563eb;
        transform: translateY(-2px);
    }
}

.state {
    text-align: center;
    color: #64748b;
    padding: 48px 0;
}
</style>
