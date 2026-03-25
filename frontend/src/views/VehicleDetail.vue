<template>
    <div class="detail-page" v-if="vehicleData">
        <div class="detail-layout">
            <div class="main-column">
                <section class="hero-card">
                    <ImageCarousel
                        :images="imageList"
                        :active-index="currentImageIndex"
                        :show-nav="imageList.length > 1 || variants.length > 1"
                        @change="handleImageChange"
                    />
                    <div class="hero-meta">
                        <div class="hero-title">
                            <h1>{{ vehicleData.vehicle?.plateNumber || vehicleData.vehicle?.modelName || '车辆详情' }}</h1>
                            <p class="muted">
                                {{ vehicleData.vehicle?.manufacturer || '未注明厂商' }}
                                <span v-if="vehicleData.vehicle?.modelName"> · {{ vehicleData.vehicle.modelName }}</span>
                            </p>
                            <p class="brand-line" v-if="currentBrand.brandId">
                                品牌：
                                <router-link
                                    class="brand-link"
                                    :to="{ name: 'BrandCatalog', params: { brandId: currentBrand.brandId } }"
                                >
                                    {{ currentBrand.brandLabel }}
                                </router-link>
                            </p>
                            <p class="brand-line" v-else>品牌：待补充</p>
                        </div>
                        <div class="hero-uploader" v-if="vehicleData.images?.[0]?.uploaderUsername">
                            <router-link
                                class="uploader-link"
                                :to="{ name: 'UserProfile', params: { userId: vehicleData.images[0].uploaderId || vehicleData.images[0].uploaderUsername } }"
                            >
                                {{ vehicleData.images[0].uploaderDisplayName || vehicleData.images[0].uploaderUsername }}
                            </router-link>
                            <span class="uploader-label">上传</span>
                        </div>
                    </div>
                </section>
                <div class="like-bar">
                    <button
                        class="like-btn"
                        :class="{ active: liked }"
                        @click="toggleLike"
                        :disabled="!isAuthenticated || likeLoading"
                    >
                        ★
                    </button>
                    <div class="like-users">
                        <template v-if="likeTotal">
                            <span v-for="(user, index) in likes" :key="user.id || index" class="like-user">
                                {{ user.name || user.username || '用户' }}
                            </span>
                            <span v-if="likeTotal > likes.length" class="like-more">等{{ likeTotal - likes.length }}人</span>
                            <span>喜欢</span>
                        </template>
                        <span v-else class="like-empty">还没有人喜欢</span>
                    </div>
                </div>

                <el-card class="related-card" v-if="sameCompany.length">
                    <template #header>同公司其它车辆</template>
                    <el-row :gutter="12">
                        <el-col
                            v-for="item in sameCompany"
                            :key="item.vehicle.id"
                            :xs="24"
                            :sm="12"
                            :md="8"
                            :lg="6"
                        >
                            <vehicle-card :vehicle="item.vehicle" :images="item.images" />
                        </el-col>
                    </el-row>
                </el-card>
            </div>

            <aside class="comment-column" v-if="!isMobile">
                <button class="favorite-toggle" @click="toggleLike" :disabled="!isAuthenticated || likeLoading">
                    {{ liked ? '★ 已收藏' : '☆ 收藏' }} <span class="favorite-count" v-if="likeTotal">({{ likeTotal }})</span>
                </button>
                <div class="comment-panel">
                    <div class="comment-panel__header">
                        <h3>评论</h3>
                        <span>{{ commentsTotal || 0 }} 条</span>
                    </div>
                    <div v-if="!isAuthenticated" class="login-hint">
                        <p>登录后可查看并发布评论。</p>
                        <el-button type="primary" size="small" @click="goLogin">去登录</el-button>
                    </div>
                    <div v-else class="comment-body">
                        <div class="comment-list" v-loading="commentsLoading">
                            <p v-if="!comments.length && !commentsLoading" class="empty-tip">暂时没有评论</p>
                            <div v-for="item in comments" :key="item.id" class="comment-item">
                                <div class="comment-meta">
                                    <span class="comment-author">{{ item.displayName || item.username }}</span>
                                    <span class="comment-time">{{ item.createdAt }}</span>
                                </div>
                                <p class="comment-content">{{ item.content }}</p>
                                <div class="comment-actions" v-if="canDeleteComment(item)">
                                    <el-button
                                        size="small"
                                        text
                                        type="danger"
                                        :loading="deletingCommentIds.has(item.id)"
                                        @click="deleteComment(item)"
                                    >
                                        删除
                                    </el-button>
                                </div>
                            </div>
                        </div>
                        <div class="comment-editor">
                            <el-input
                                v-model="commentInput"
                                type="textarea"
                                :autosize="{ minRows: 2, maxRows: 4 }"
                                placeholder="说点什么吧（最多500字）"
                                maxlength="500"
                                show-word-limit
                            />
                            <div class="editor-actions">
                                <el-button type="primary" size="small" :loading="commentSubmitting" @click="submitComment">
                                    发布
                                </el-button>
                            </div>
                        </div>
                    </div>
                </div>
            </aside>
        </div>

        <div class="mobile-comments" v-if="isMobile">
            <div class="mobile-comment-list" v-if="isAuthenticated" v-loading="commentsLoading">
                <p v-if="!comments.length && !commentsLoading" class="empty-tip">暂无评论</p>
                <div v-for="item in comments" :key="item.id" class="comment-item">
                    <div class="comment-meta">
                        <span class="comment-author">{{ item.displayName || item.username }}</span>
                        <span class="comment-time">{{ item.createdAt }}</span>
                    </div>
                    <p class="comment-content">{{ item.content }}</p>
                    <div class="comment-actions" v-if="canDeleteComment(item)">
                        <el-button
                            size="small"
                            text
                            type="danger"
                            :loading="deletingCommentIds.has(item.id)"
                            @click="deleteComment(item)"
                        >
                            删除
                        </el-button>
                    </div>
                </div>
            </div>
            <div class="mobile-login-hint" v-else>
                <p>登录后查看评论</p>
                <el-button size="small" type="primary" @click="goLogin">登录</el-button>
            </div>
            <div class="mobile-comment-input">
                <el-input
                    v-model="commentInput"
                    placeholder="写评论..."
                    maxlength="500"
                    @keyup.enter.exact.prevent="submitComment"
                />
                <el-button type="primary" size="small" :loading="commentSubmitting" @click="submitComment">发送</el-button>
            </div>
        </div>
    </div>
    <el-empty v-else description="数据加载中..." />
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import ImageCarousel from '@/components/Gallery/ImageCarousel.vue';
import VehicleCard from '@/components/VehicleCard.vue';
import {
    fetchVehicleGallery,
    fetchFavoriteSummary,
    toggleFavorite,
    fetchVehiclesByPlate
} from '@/api/vehicles';

const route = useRoute();
const router = useRouter();
const store = useStore();

const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const currentUser = computed(() => store.state.auth.profile);
const currentUserId = computed(() => currentUser.value?.id || currentUser.value?.userId || null);
const currentUserRole = computed(() => currentUser.value?.role || '');
const variants = ref([]);
const imageList = ref([]);
const currentImageIndex = ref(0);
const currentVariantIndex = ref(0);
const vehicleData = computed(() => variants.value[currentVariantIndex.value] || null);
const sameCompany = ref([]);
const currentBrand = computed(() => {
    const detail = vehicleData.value;
    const modelBrandId = detail?.vehicle?.model?.brandId;
    const configBrandId = detail?.vehicleConfig?.brandId;
    const brandId = modelBrandId || configBrandId || null;
    const brandById = brandId ? store.getters['brands/brandById'](brandId) : null;
    const brandChnName =
        detail?.vehicle?.model?.brandChnName ||
        detail?.vehicleConfig?.brandChnName ||
        brandById?.chnName;
    const brandName =
        detail?.vehicle?.model?.brandName ||
        detail?.vehicleConfig?.brandName ||
        brandById?.name;
    return {
        brandId,
        brandLabel: brandChnName || brandName || '未知品牌'
    };
});

const comments = ref([]);
const commentsTotal = ref(0);
const commentsLoading = ref(false);
const commentInput = ref('');
const commentSubmitting = ref(false);
const deletingCommentIds = ref(new Set());
const likes = ref([]);
const liked = ref(false);
const likeTotal = ref(0);
const likeLoading = ref(false);
let likeDebounceTimer = null;
const syncedLiked = ref(false);
let pendingLikeSync = false;
const favoriteCache = new Map();
const favoriteLoading = new Set();

const isMobile = ref(false);
const resizeHandler = () => {
    isMobile.value = window.innerWidth < 900;
    refreshCommentPolling();
};

const COMMENT_PAGE_SIZE = 50;
const COMMENT_POLL_INTERVAL_MS = 8000;
let commentPollTimer = null;

const syncCommentsFromCache = (vehicleId) => {
    const cached = store.state.vehicles?.commentCache?.[vehicleId];
    if (!cached) return;
    comments.value = cached.records || [];
    commentsTotal.value = cached.total ?? cached.records?.length ?? 0;
};

const loadSameCompany = async (companyId, currentVehicleId) => {
    if (!companyId) {
        sameCompany.value = [];
        return;
    }
    const resp = await fetchVehicleGallery({ companyId, size: 6 });
    const list = resp.records || resp.items || resp.data || [];
    sameCompany.value = list.filter((item) => item.vehicle?.id !== currentVehicleId);
};

const rebuildImages = () => {
    const list = [];
    variants.value.forEach((variant, vIdx) => {
        (variant.images || []).forEach((img) => {
            list.push({ ...img, __vehicleIndex: vIdx, __vehicleId: variant.vehicle?.id });
        });
    });
    imageList.value = list;
    const targetVehicleId = variants.value[currentVariantIndex.value]?.vehicle?.id;
    const foundIdx = list.findIndex((img) => img.__vehicleId === targetVehicleId);
    currentImageIndex.value = foundIdx >= 0 ? foundIdx : 0;
};

const loadDetail = async (id) => {
    if (!id) {
        variants.value = [];
        sameCompany.value = [];
        return;
    }
    liked.value = false;
    likeTotal.value = 0;
    likes.value = [];
    const detail = await store.dispatch('vehicles/loadVehicleDetail', id);
    let grouped = Array.isArray(detail?.variants) ? detail.variants : [];
    if (!grouped.length) {
        try {
            const plate = (detail?.vehicle?.plateNumber || '').replace(/\s+/g, '');
            if (plate) {
                const resp = await fetchVehiclesByPlate(plate);
                grouped = resp?.variants || [];
            }
        } catch (e) {
            grouped = [];
        }
    }
    variants.value = grouped.length ? grouped : [detail];
    currentVariantIndex.value =
        variants.value.findIndex((v) => v.vehicle?.id === detail?.vehicle?.id) >= 0
            ? variants.value.findIndex((v) => v.vehicle?.id === detail?.vehicle?.id)
            : 0;
    rebuildImages();
    await loadSameCompany(vehicleData.value?.vehicle?.companyId, vehicleData.value?.vehicle?.id);
    if (isAuthenticated.value) {
        await loadComments();
    }
    await loadLikeSummary();
    refreshCommentPolling();
};

const handleImageChange = async (index) => {
    currentImageIndex.value = index;
    const img = imageList.value[index];
    if (img && typeof img.__vehicleIndex === 'number') {
        if (currentVariantIndex.value !== img.__vehicleIndex) {
            currentVariantIndex.value = img.__vehicleIndex;
            await loadSameCompany(vehicleData.value?.vehicle?.companyId, vehicleData.value?.vehicle?.id);
            await loadLikeSummary();
            if (isAuthenticated.value) {
                await loadComments();
            }
            refreshCommentPolling();
        }
    }
};

const loadComments = async ({ force = false } = {}) => {
    if (!vehicleData.value?.vehicle?.id || !isAuthenticated.value) {
        comments.value = [];
        commentsTotal.value = 0;
        return;
    }
    commentsLoading.value = true;
    try {
        const resp = await store.dispatch('vehicles/loadVehicleComments', {
            vehicleId: vehicleData.value.vehicle.id,
            page: 1,
            size: COMMENT_PAGE_SIZE,
            force
        });
        comments.value = resp?.records || [];
        commentsTotal.value = resp?.total ?? comments.value.length ?? 0;
    } catch (error) {
        ElMessage.error(error.message || '加载评论失败');
    } finally {
        commentsLoading.value = false;
    }
};

const submitComment = async () => {
    if (!isAuthenticated.value) {
        ElMessage.info('请先登录再发表评论');
        goLogin();
        return;
    }
    const text = commentInput.value?.trim();
    if (!text) {
        ElMessage.warning('请输入评论内容');
        return;
    }
    commentSubmitting.value = true;
    try {
        await store.dispatch('vehicles/addVehicleComment', { vehicleId: vehicleData.value.vehicle.id, content: text });
        commentInput.value = '';
        syncCommentsFromCache(vehicleData.value.vehicle.id);
        ElMessage.success('评论已发布');
    } catch (error) {
        ElMessage.error(error.message || '发布失败');
    } finally {
        commentSubmitting.value = false;
    }
};

const canDeleteComment = (comment) => {
    if (!isAuthenticated.value || !comment) {
        return false;
    }
    if (currentUserRole.value === 'STATION') {
        return true;
    }
    const ownerId = comment?.userId;
    return ownerId != null && currentUserId.value != null && Number(ownerId) === Number(currentUserId.value);
};

const deleteComment = async (comment) => {
    const vehicleId = vehicleData.value?.vehicle?.id;
    const commentId = comment?.id;
    if (!vehicleId || !commentId || deletingCommentIds.value.has(commentId)) {
        return;
    }
    if (!canDeleteComment(comment)) {
        ElMessage.warning('没有权限删除该评论');
        return;
    }
    const nextSet = new Set(deletingCommentIds.value);
    nextSet.add(commentId);
    deletingCommentIds.value = nextSet;
    try {
        await store.dispatch('vehicles/deleteVehicleComment', { vehicleId, commentId });
        syncCommentsFromCache(vehicleId);
        ElMessage.success('评论已删除');
    } catch (error) {
        ElMessage.error(error?.message || '删除评论失败');
    } finally {
        const releaseSet = new Set(deletingCommentIds.value);
        releaseSet.delete(commentId);
        deletingCommentIds.value = releaseSet;
    }
};

const mapUsers = (list = []) =>
    list.map((u) => ({
        id: u.id,
        name: u.displayName || u.username || '用户',
        isSelf: u.isSelf
    }));

const applyOptimisticLike = (nextLiked) => {
    const user = currentUser.value;
    if (!user) return;
    if (nextLiked) {
        const exists = likes.value.some((u) => u.id === user.id);
        if (!exists) {
            likes.value = [
                {
                    id: user.id,
                    name: user.displayName || user.username || '我',
                    isSelf: true
                },
                ...likes.value
            ].slice(0, 2);
        }
    } else {
        likes.value = likes.value.filter((u) => u.id !== user.id);
    }
};

const loadLikeSummary = async () => {
    const id = vehicleData.value?.vehicle?.id;
    if (!id) return;
    const detailFavorite = vehicleData.value?.favoriteSummary;
    if (detailFavorite && !favoriteCache.has(id)) {
        favoriteCache.set(id, detailFavorite);
    }
    if (favoriteCache.has(id)) {
        const cached = favoriteCache.get(id);
        liked.value = cached.liked || false;
        likeTotal.value = cached.total || 0;
        likes.value = mapUsers(cached.topUsers || []).slice(0, 2);
        syncedLiked.value = liked.value;
    }
    const shouldFetchRemote = isAuthenticated.value || !favoriteCache.has(id);
    if (!shouldFetchRemote || favoriteLoading.has(id)) return;
    favoriteLoading.add(id);
    try {
        const resp = await fetchFavoriteSummary(id);
        favoriteCache.set(id, resp || {});
        liked.value = resp?.liked || false;
        likeTotal.value = resp?.total || 0;
        likes.value = mapUsers(resp?.topUsers || []).slice(0, 2);
        syncedLiked.value = liked.value;
    } catch (error) {
        // ignore silently
    } finally {
        favoriteLoading.delete(id);
    }
};

const syncLike = async () => {
    const id = vehicleData.value?.vehicle?.id;
    if (!id) return;
    if (likeLoading.value) {
        pendingLikeSync = true;
        return;
    }
    if (liked.value === syncedLiked.value) return;
    likeLoading.value = true;
    try {
        const resp = await toggleFavorite(id);
        liked.value = resp?.liked || false;
        likeTotal.value = resp?.total || 0;
        likes.value = mapUsers(resp?.topUsers || []).slice(0, 2);
        syncedLiked.value = liked.value;
        favoriteCache.set(id, resp || {});
    } catch (error) {
        const fallback = favoriteCache.get(id);
        if (fallback) {
            liked.value = fallback.liked || false;
            likeTotal.value = fallback.total || 0;
            likes.value = mapUsers(fallback.topUsers || []).slice(0, 2);
            syncedLiked.value = liked.value;
        }
        ElMessage.error(error?.message || '收藏操作失败');
    } finally {
        likeLoading.value = false;
        if (pendingLikeSync) {
            pendingLikeSync = false;
            scheduleLikeSync();
        }
    }
};

const scheduleLikeSync = () => {
    if (likeDebounceTimer) {
        clearTimeout(likeDebounceTimer);
    }
    likeDebounceTimer = setTimeout(syncLike, 250);
};

const toggleLike = () => {
    if (!isAuthenticated.value) {
        ElMessage.info('请先登录再收藏');
        goLogin();
        return;
    }
    if (!vehicleData.value?.vehicle?.id) return;
    if (likeLoading.value) return;

    const nextLiked = !liked.value;
    liked.value = nextLiked;
    likeTotal.value = Math.max(0, likeTotal.value + (nextLiked ? 1 : -1));
    applyOptimisticLike(nextLiked);

    scheduleLikeSync();
};

function refreshCommentPolling() {
    const shouldPoll = isAuthenticated.value && Boolean(vehicleData.value?.vehicle?.id);
    if (!shouldPoll) {
        if (commentPollTimer) {
            clearInterval(commentPollTimer);
            commentPollTimer = null;
        }
        return;
    }
    if (commentPollTimer) return;
    commentPollTimer = setInterval(() => {
        if (!isAuthenticated.value) return;
        loadComments({ force: true });
    }, COMMENT_POLL_INTERVAL_MS);
}

const goLogin = () => {
    router.push({ name: 'Login', query: { redirect: route.fullPath } });
};

watch(
    () => route.params.id,
    (id) => {
        const resolved = Number(id);
        if (Number.isNaN(resolved)) {
            vehicleData.value = null;
            sameCompany.value = [];
            return;
        }
        loadDetail(resolved);
    },
    { immediate: true }
);

watch(isAuthenticated, async (val) => {
    if (val) {
        await loadComments();
    } else if (!val) {
        comments.value = [];
        commentsTotal.value = 0;
    }
    refreshCommentPolling();
    await loadLikeSummary();
});

onMounted(() => {
    store.dispatch('brands/loadBrands').catch(() => {});
    resizeHandler();
    window.addEventListener('resize', resizeHandler);
});

onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeHandler);
    if (likeDebounceTimer) {
        clearTimeout(likeDebounceTimer);
    }
    if (commentPollTimer) {
        clearInterval(commentPollTimer);
        commentPollTimer = null;
    }
});
</script>

<style scoped lang="scss">
.detail-page {
    padding: 16px;
    position: relative;
    overflow-x: hidden;
}

.detail-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 380px;
    gap: 16px;
    align-items: start;
}

.hero-card {
    background: #0b1220;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 16px 36px rgba(15, 23, 42, 0.26);
}

.hero-meta {
    padding: 12px 16px 18px;
    color: #e2e8f0;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
}

.hero-title h1 {
    margin: 0;
    font-size: 1.2rem;
    color: #f8fafc;
}

.hero-title .muted {
    margin: 4px 0 0;
    color: #cbd5e1;
}

.brand-line {
    margin: 8px 0 0;
    color: #cbd5e1;
    font-size: 0.95rem;
}

.brand-link {
    color: #67e8f9;
    font-weight: 600;
    text-decoration: none;
}

.brand-link:hover {
    text-decoration: underline;
}

.muted {
    color: #cbd5e1;
}

.hero-uploader {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #cbd5e1;
    white-space: nowrap;
}

.uploader-link {
    color: #38bdf8;
    font-weight: 600;
    text-decoration: none;
}

.uploader-link:hover {
    text-decoration: underline;
}

.uploader-label {
    font-size: 12px;
    color: #94a3b8;
}

.main-column {
    min-width: 0;
}

.related-card {
    margin-top: 24px;
}

.like-bar {
    margin-top: 12px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
}

.like-btn {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    border: 1px solid #e2e8f0;
    background: #fff;
    color: #cbd5f5;
    font-size: 22px;
    cursor: pointer;
    transition: all 0.2s;
}

.like-btn.active {
    color: #fbbf24;
    border-color: #fbbf24;
    background: #fffaf0;
}

.like-btn:disabled {
    cursor: not-allowed;
    opacity: 0.6;
}

.like-users {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    color: #475569;
}

.like-user {
    background: #eef2ff;
    color: #1d4ed8;
    padding: 2px 8px;
    border-radius: 10px;
    font-weight: 600;
}

.like-more {
    color: #94a3b8;
}

.like-empty {
    color: #94a3b8;
}

:deep(.vehicle-gallery-card .carousel__main) {
    min-height: 360px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #0b1220;
}

:deep(.vehicle-gallery-card .carousel__main img) {
    width: 100%;
    height: auto;
    max-height: 520px;
    object-fit: contain;
    background: #0b1220;
}

:deep(.vehicle-gallery-card .carousel__thumbs img) {
    width: 100%;
    height: auto;
    aspect-ratio: 4 / 3;
    object-fit: contain;
    background: #0b1220;
}

.comment-column {
    position: sticky;
    top: 16px;
    align-self: start;
    border-left: 1px solid #d1d5db;
    padding-left: 16px;
}

.favorite-toggle {
    width: 100%;
    border: 1px solid #fde68a;
    background: #fffbeb;
    border-radius: 10px;
    padding: 10px 14px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    color: #b45309;
    font-weight: 600;
    margin-top: 10px;
}

.favorite-toggle:hover {
    border-color: #fbbf24;
    box-shadow: 0 10px 20px rgba(245, 158, 11, 0.16);
}

.favorite-toggle:disabled {
    cursor: not-allowed;
    opacity: 0.6;
}

.favorite-count {
    color: #92400e;
    margin-left: 4px;
}

.comment-panel {
    margin-top: 12px;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    background: #fff;
    box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.comment-panel__header {
    padding: 12px 16px;
    border-bottom: 1px solid #e2e8f0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.comment-panel__header h3 {
    margin: 0;
    font-size: 16px;
    color: #0f172a;
}

.comment-panel__header span {
    color: #64748b;
    font-size: 13px;
}

.login-hint,
.mobile-login-hint {
    padding: 16px;
    text-align: center;
}

.comment-body {
    display: flex;
    flex-direction: column;
    height: 420px;
}

.comment-list {
    flex: 1 1 auto;
    padding: 12px 16px;
    overflow-y: auto;
}

.comment-item + .comment-item {
    margin-top: 12px;
}

.comment-meta {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #94a3b8;
}

.comment-author {
    font-weight: 600;
    color: #0f172a;
}

.comment-content {
    margin: 4px 0 0;
    color: #1f2937;
    word-break: break-word;
}

.comment-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 6px;
}

.empty-tip {
    text-align: center;
    color: #94a3b8;
    margin: 12px 0;
}

.comment-editor {
    padding: 12px 16px;
    border-top: 1px solid #e2e8f0;
    background: #f8fafc;
}

.editor-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
}

.mobile-comments {
    margin-top: 16px;
    padding-bottom: 64px;
}

.mobile-comment-input {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    gap: 8px;
    padding: 10px 12px;
    background: #ffffff;
    border-top: 1px solid #e2e8f0;
}

.mobile-comment-input :deep(.el-input) {
    flex: 1 1 auto;
}

@media (max-width: 900px) {
    .detail-layout {
        grid-template-columns: 1fr;
    }
    .comment-column {
        display: none;
    }
}

@media (max-width: 417px) {
    .detail-page {
        padding: 12px;
    }

    :deep(.related-card .el-row) {
        margin-left: 0 !important;
        margin-right: 0 !important;
    }

    :deep(.related-card .el-col) {
        padding-left: 0 !important;
        padding-right: 0 !important;
    }
}
</style>

