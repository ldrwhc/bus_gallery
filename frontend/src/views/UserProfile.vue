<template>
    <div class="user-profile-page">
        <section v-if="profile" class="profile-card">
            <div class="profile-info">
                <div class="avatar">{{ avatarInitial }}</div>
                <div>
                    <h1>{{ profile.displayName || '匿名用户' }}</h1>
                    <p class="muted">@{{ profile.username || 'unknown' }}</p>
                    <p class="muted">累计上传：{{ profile.uploadsCount }} 张</p>
                </div>
            </div>
            <el-button v-if="isSelf" type="primary" @click="goUpload">继续上传</el-button>
        </section>

        <section class="gallery-card">
            <header class="gallery-header">
                <div>
                    <h2>图片档案</h2>
                    <p class="muted">点击卡片可打开车辆详情</p>
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

            <div v-if="imagesLoading || profileLoading" class="state">加载中...</div>
            <div v-else-if="!images.length" class="state">暂未上传图片</div>
            <div v-else class="image-grid">
                <article v-for="image in images" :key="image.id" class="image-card" @click="openImage(image)">
                    <img :src="image.thumbnailUrl || image.url" :alt="image.objectName || 'upload'" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import { fetchUserProfile, fetchUserImages } from '@/api/users';
import { formatDate } from '@/utils/formatters';
import VehicleDetailModal from '@/components/Gallery/VehicleDetailModal.vue';

const route = useRoute();
const router = useRouter();
const store = useStore();

const targetUserId = computed(() => {
    const raw = Number(route.params.userId);
    return Number.isNaN(raw) ? null : raw;
});

const profile = ref(null);
const profileLoading = ref(false);
const profileError = ref(null);
const avatarInitial = computed(() => {
    const name = profile.value?.displayName?.trim() || '';
    return name ? name.slice(0, 1).toUpperCase() : (profile.value?.username?.slice(0, 1)?.toUpperCase() || '?');
});

const pagination = reactive({
    page: 1,
    size: 12,
    total: 0
});

const images = ref([]);
const imagesLoading = ref(false);

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

const isSelf = computed(() => {
    const meId = store.state.auth.profile?.id;
    return meId && profile.value?.id && Number(meId) === Number(profile.value.id);
});

const ensureUserId = () => {
    if (!targetUserId.value) {
        ElMessage.error('未找到有效的用户 ID');
        router.replace({ name: 'Home' });
        return false;
    }
    return true;
};

const loadProfile = async () => {
    if (!ensureUserId()) return;
    profileLoading.value = true;
    profileError.value = null;
    try {
        const data = await fetchUserProfile(targetUserId.value);
        profile.value = data;
    } catch (error) {
        profileError.value = error;
        ElMessage.error(error?.message || '用户信息加载失败');
        router.replace({ name: 'Home' });
    } finally {
        profileLoading.value = false;
    }
};

const loadImages = async () => {
    if (!ensureUserId()) return;
    imagesLoading.value = true;
    try {
        const data = await fetchUserImages(targetUserId.value, {
            page: pagination.page,
            size: pagination.size
        });
        images.value = data.records || [];
        pagination.total = data.total || 0;
    } catch (error) {
        ElMessage.error(error?.message || '图片加载失败');
    } finally {
        imagesLoading.value = false;
    }
};

const handlePageChange = (page) => {
    pagination.page = page;
    loadImages();
};

const openImage = async (image) => {
    if (!image?.vehicleId) {
        ElMessage.info('该图片暂未关联车辆');
        return;
    }
    activeVehicleId.value = image.vehicleId;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', image.vehicleId);
    } catch (error) {
        ElMessage.error('车辆详情加载失败');
        activeVehicleId.value = null;
    }
};

const closeVehicleDetail = () => {
    activeVehicleId.value = null;
};

const goUpload = () => {
    router.push({ name: 'Upload' });
};

watch(
    () => route.params.userId,
    () => {
        pagination.page = 1;
        loadProfile();
        loadImages();
    },
    { immediate: true }
);

onMounted(() => {
    if (!store.state.auth.profile) {
        store.dispatch('auth/fetchProfile').catch(() => {});
    }
});
</script>

<style scoped lang="scss">
.user-profile-page {
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
}

.avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #1d4ed8;
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
    background: #f8fafc;
    border-radius: 16px;
    padding: 12px;
    box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.2);
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
    }

    img {
        width: 100%;
        height: 140px;
        object-fit: cover;
        border-radius: 12px;
        margin-bottom: 8px;
    }
}

.state {
    text-align: center;
    padding: 48px 0;
    color: #94a3b8;
}
</style>
