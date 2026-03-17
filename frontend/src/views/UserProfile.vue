<template>
    <div class="user-profile-page">
        <section v-if="profile" class="profile-card">
            <div class="profile-info">
                <div class="avatar">{{ avatarInitial }}</div>
                <div>
                    <h1>{{ profile.displayName || '未命名用户' }}</h1>
                    <p class="muted">@{{ profile.username || 'unknown' }}</p>
                    <p class="muted">累计上传：{{ profile.uploadsCount || 0 }} 张</p>
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

        <section class="gallery-card">
            <header class="gallery-header">
                <div>
                    <h2>{{ isSelf ? '我的喜爱' : '收藏' }}</h2>
                    <p class="muted">
                        {{ isSelf ? '收藏的车辆（点击卡片打开详情）' : '收藏列表（若为空则尚未公开或暂未收藏）' }}
                    </p>
                </div>
            </header>
            <div v-if="favoritesLoading || profileLoading" class="state">收藏加载中...</div>
            <div v-else-if="!favorites.length && isSelf" class="state">还没有收藏车辆</div>
            <div v-else-if="!favorites.length && !isSelf" class="state">该用户暂无公开收藏</div>
            <div v-else class="favorite-grid">
                <article
                    v-for="fav in favorites"
                    :key="fav.vehicle?.id"
                    class="favorite-card"
                    @click="openFavorite(fav)"
                >
                    <img
                        :src="fav.images?.[0]?.thumbnailUrl || fav.images?.[0]?.url || ''"
                        :alt="fav.vehicle?.plateNumber || fav.vehicle?.modelName || 'favorite vehicle'"
                    />
                    <div class="favorite-meta">
                        <h3>{{ fav.vehicle?.plateNumber || '未命名车辆' }}</h3>
                    </div>
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
import { fetchFavorites } from '@/api/vehicles';
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
const favorites = ref([]);
const favoritesLoading = ref(false);

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
        ElMessage.error(error?.message || '获取用户信息失败');
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
        ElMessage.error(error?.message || '加载图片失败');
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
        ElMessage.error('加载车辆详情失败');
        activeVehicleId.value = null;
    }
};

const openFavorite = async (detail) => {
    if (!detail?.vehicle?.id) return;
    activeVehicleId.value = detail.vehicle.id;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', detail.vehicle.id);
    } catch (error) {
        ElMessage.error('加载车辆详情失败');
        activeVehicleId.value = null;
    }
};

const loadFavorites = async () => {
    favoritesLoading.value = true;
    try {
        const list = await fetchFavorites(targetUserId.value);
        favorites.value = Array.isArray(list) ? list : [];
    } catch (error) {
        favorites.value = [];
    } finally {
        favoritesLoading.value = false;
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
    async () => {
        pagination.page = 1;
        await loadProfile();
        await loadImages();
        await loadFavorites();
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

.favorite-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 16px;
    margin-top: 16px;
}

.favorite-card {
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
        background: #e2e8f0;
    }
}

.favorite-meta h3 {
    margin: 0 0 4px;
}

.state {
    text-align: center;
    padding: 48px 0;
    color: #94a3b8;
}
</style>
