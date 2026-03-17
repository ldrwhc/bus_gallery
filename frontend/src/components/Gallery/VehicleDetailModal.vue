<template>
    <teleport to="body">
        <div v-if="visible" class="modal-backdrop" @click.self="handleClose">
            <div class="modal">
                <header class="modal__header">
                    <div class="modal__headline">
                        <p class="modal__eyebrow">车辆详情</p>
                        <div class="title-row">
                            <h2>{{ vehicleTitle }}</h2>
                            <button
                                v-if="hasExif"
                                class="info-badge"
                                type="button"
                                title="查看 EXIF 信息"
                                @click="exifVisible = true"
                            >
                                i
                            </button>
                        </div>
                    </div>
                    <div class="header-actions">
                        <button class="favorite-btn" type="button" :disabled="likeLoading" @click="toggleFavoriteAction">
                            {{ liked ? '★ 已收藏' : '☆ 收藏' }}
                            <span v-if="likeTotal" class="favorite-count">({{ likeTotal }})</span>
                        </button>
                        <button class="comment-entry" type="button" @click="toggleComments">
                            💬 评论
                        </button>
                        <button class="close-btn" type="button" @click="handleClose">×</button>
                    </div>
                </header>

                <div class="modal__content">
                    <section v-if="loading" class="modal__state">正在加载车辆详情...</section>
                    <section v-else-if="!vehicle" class="modal__state">暂无车辆详情数据</section>

                    <section v-else class="modal__body">
                        <div class="image-section">
                            <div v-if="hasImages" class="image-section__viewer">
                                <button
                                    v-if="variants.length > 1"
                                    class="variant-nav nav-btn nav-btn--prev variant-nav--prev"
                                    type="button"
                                    @click="prevVariant"
                                >
                                    ‹
                                </button>
                                <ImageCarousel
                                    :images="images"
                                    :active-index="currentImageIndex"
                                    :show-nav="false"
                                    @change="handleImageChange"
                                />
                                <button
                                    v-if="variants.length > 1"
                                    class="variant-nav nav-btn nav-btn--next variant-nav--next"
                                    type="button"
                                    @click="nextVariant"
                                >
                                    ›
                                </button>
                            </div>
                            <p v-else class="image-section__empty">暂无图片</p>
                            <div class="favorite-users" v-if="likeTotal">
                                <span class="favorite-label">喜欢：</span>
                                <span v-for="(user, idx) in likes" :key="user.id || idx" class="favorite-user">
                                    {{ user.name }}
                                </span>
                                <span v-if="likeTotal > likes.length" class="favorite-more">等{{ likeTotal - likes.length }}人</span>
                            </div>
                        </div>

                        <div class="info-section" v-if="vehicleInfoCards.length">
                            <h3>车辆信息</h3>
                            <div class="info-grid">
                                <div v-for="field in vehicleInfoCards" :key="field.key" class="info-item">
                                    <span>{{ field.label }}</span>
                                    <strong>
                                        <router-link
                                            v-if="field.link && getVehicleLink(field)"
                                            class="info-link"
                                            :to="getVehicleLink(field)"
                                        >
                                            {{ field.value }}
                                        </router-link>
                                        <span v-else class="info-value">{{ field.value }}</span>
                                    </strong>
                                </div>
                            </div>
                        </div>

                        <div class="info-section" v-if="configInfoCards.length">
                            <h3>车型配置</h3>
                            <div class="info-grid">
                                <div v-for="field in configInfoCards" :key="field.key" class="info-item">
                                    <span>{{ field.label }}</span>
                                    <strong>
                                        <router-link
                                            v-if="field.link && getConfigLink(field)"
                                            class="info-link"
                                            :to="getConfigLink(field)"
                                        >
                                            {{ field.value }}
                                        </router-link>
                                        <span v-else class="info-value">{{ field.value }}</span>
                                    </strong>
                                </div>
                            </div>
                        </div>
                    </section>
                </div>

                <transition name="slide">
                    <aside v-if="commentsOpen" class="comment-drawer">
                        <header class="comment-drawer__header">
                            <div>
                                <p class="comment-drawer__eyebrow">评论</p>
                                <h4>{{ commentsTotal || 0 }} 条</h4>
                            </div>
                            <button type="button" class="close-btn" @click="commentsOpen = false">×</button>
                        </header>
                        <div class="comment-drawer__body" v-loading="commentsLoading">
                            <p v-if="!isAuthenticated" class="comment-empty">登录后查看评论</p>
                            <template v-else>
                                <p v-if="!comments.length && !commentsLoading" class="comment-empty">暂无评论</p>
                                <div v-for="item in comments" :key="item.id" class="comment-item">
                                    <div class="comment-meta">
                                        <span class="comment-author">{{ item.displayName || item.username }}</span>
                                        <span class="comment-time">{{ item.createdAt }}</span>
                                    </div>
                                    <p class="comment-content">{{ item.content }}</p>
                                </div>
                            </template>
                        </div>
                        <div class="comment-drawer__footer" v-if="isAuthenticated">
                            <el-input
                                v-model="commentInput"
                                type="textarea"
                                :autosize="{ minRows: 2, maxRows: 4 }"
                                placeholder="写点什么吧"
                                maxlength="500"
                                show-word-limit
                            />
                            <el-button
                                type="primary"
                                size="small"
                                :loading="commentSubmitting"
                                class="comment-submit"
                                @click="submitComment"
                            >
                                发布
                            </el-button>
                        </div>
                    </aside>
                </transition>
            </div>
        </div>
    </teleport>
    <teleport to="body">
        <transition name="fade">
            <div v-if="exifVisible" class="exif-overlay" @click.self="exifVisible = false">
                <div class="exif-modal">
                    <header class="exif-modal__header">
                        <div>
                            <p class="modal__eyebrow">EXIF</p>
                            <h3>拍摄参数</h3>
                        </div>
                        <button class="close-btn" type="button" @click="exifVisible = false">×</button>
                    </header>
                    <div v-if="exifEntries.length" class="exif-modal__body">
                        <dl>
                            <div v-for="[key, value] in exifEntries" :key="key" class="exif-row">
                                <dt>{{ key }}</dt>
                                <dd>{{ value }}</dd>
                            </div>
                        </dl>
                    </div>
                    <p v-else class="exif-empty">暂未解析到 EXIF 信息</p>
                </div>
            </div>
        </transition>
    </teleport>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount, reactive } from 'vue';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import ImageCarousel from './ImageCarousel.vue';
import { fetchVehicleComments, createVehicleComment, fetchFavoriteSummary, toggleFavorite } from '@/api/vehicles';
import { CONFIG_INFO_FIELDS, VEHICLE_INFO_FIELDS } from '@/utils/constants';
import { formatBoolean, formatFuelType, formatYearMonth } from '@/utils/formatters';

const props = defineProps({
    visible: Boolean,
    detail: {
        type: Object,
        default: () => null
    },
    loading: Boolean
});

const emit = defineEmits(['close']);
const store = useStore();
const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const currentUser = computed(() => store.state.auth.profile);

const BODY_LOCK_CLASS = 'vehicle-detail-modal-open';
const toggleBodyScroll = (shouldLock) => {
    if (typeof document === 'undefined') return;
    document.body.classList[shouldLock ? 'add' : 'remove'](BODY_LOCK_CLASS);
};

onBeforeUnmount(() => toggleBodyScroll(false));

const variants = computed(() => {
    // 兼容多种后端字段：variants / groupedVariants / plateVariants
    const candidate =
        props.detail?.variants ||
        props.detail?.groupedVariants ||
        props.detail?.plateVariants ||
        (props.detail?.plateGroup ? props.detail.plateGroup.variants : null);
    if (candidate && candidate.length) return candidate;
    if (props.detail?.vehicle) {
        return [{ vehicle: props.detail.vehicle, images: props.detail.images || [] }];
    }
    return [];
});

const images = computed(() => {
    const list = [];
    variants.value.forEach((v, idx) => {
        (v.images || []).forEach((img) => list.push({ ...img, __variantIndex: idx }));
    });
    return list;
});

const findFirstImageIndexForVariant = (variantIdx) =>
    images.value.findIndex((img) => img.__variantIndex === variantIdx);

const currentImageIndex = ref(0);
const currentVariantIndex = ref(0);

const vehicle = computed(() => variants.value[currentVariantIndex.value]?.vehicle || null);
const config = computed(
    () => variants.value[currentVariantIndex.value]?.vehicleConfig || props.detail?.vehicleConfig || {}
);

watch(
    () => props.detail,
    () => {
        currentVariantIndex.value = 0;
        const firstIdx = findFirstImageIndexForVariant(0);
        currentImageIndex.value = firstIdx >= 0 ? firstIdx : 0;
    }
);

watch(currentVariantIndex, (idx) => {
    const firstIdx = findFirstImageIndexForVariant(idx);
    if (firstIdx >= 0 && firstIdx !== currentImageIndex.value) {
        currentImageIndex.value = firstIdx;
    }
});

const hasImages = computed(() => (images.value?.length || 0) > 0);

const vehicleTitle = computed(
    () => vehicle.value?.plateNumber || vehicle.value?.model?.name || vehicle.value?.company?.name || '车辆详情'
);

const exifSource = computed(() => {
    if (!images.value?.length) return null;
    return images.value.find((item) => item?.exif && Object.keys(item.exif).length);
});

const handleImageChange = (nextIndex) => {
    currentImageIndex.value = nextIndex;
    const targetVariant = images.value[nextIndex]?.__variantIndex;
    if (typeof targetVariant === 'number' && targetVariant !== currentVariantIndex.value) {
        currentVariantIndex.value = targetVariant;
    }
};

const prevVariant = () => {
    const total = variants.value.length;
    if (total < 2) return;
    currentVariantIndex.value = (currentVariantIndex.value - 1 + total) % total;
};

const nextVariant = () => {
    const total = variants.value.length;
    if (total < 2) return;
    currentVariantIndex.value = (currentVariantIndex.value + 1) % total;
};

const hasExif = computed(() => Boolean(exifSource.value));
const exifEntries = computed(() => {
    if (!exifSource.value?.exif) return [];
    return Object.entries(exifSource.value.exif);
});

// 收藏
const likes = ref([]);
const likeTotal = ref(0);
const liked = ref(false);
const likeLoading = ref(false);
const syncedLiked = ref(false);
let likeDebounceTimer = null;
let pendingLikeSync = false;
const favoriteCache = reactive({});
const favoriteLoadingIds = reactive(new Set());

const mapUsers = (list = []) =>
    list.map((u) => ({
        id: u.id,
        name: u.displayName || u.username || '用户',
        isSelf: u.isSelf
    }));

async function loadFavoriteSummary() {
    if (!vehicle.value?.id) return;
    const vid = vehicle.value.id;
    // 先用 props.detail 携带的数据（若有）
    if (props.detail?.favoriteSummary && !favoriteCache[vid]) {
        favoriteCache[vid] = props.detail.favoriteSummary;
    }
    const storeFavorite = store.state.vehicles?.detailMap?.[vid]?.favorite;
    if (storeFavorite && !favoriteCache[vid]) {
        favoriteCache[vid] = {
            liked: storeFavorite.liked,
            total: storeFavorite.likeTotal,
            topUsers: storeFavorite.likes
        };
    }
    // 优先使用前端缓存，避免重复请求
    if (favoriteCache[vid]) {
        const cached = favoriteCache[vid];
        liked.value = cached.liked || false;
        likeTotal.value = cached.total || 0;
        likes.value = mapUsers(cached.topUsers || []).slice(0, 2);
        syncedLiked.value = liked.value;
        return;
    }
    if (favoriteLoadingIds.has(vid)) return;
    favoriteLoadingIds.add(vid);
    try {
        const resp = await fetchFavoriteSummary(vid);
        favoriteCache[vid] = resp || {};
        liked.value = resp?.liked || false;
        likeTotal.value = resp?.total || 0;
        likes.value = mapUsers(resp?.topUsers || []).slice(0, 2);
        syncedLiked.value = liked.value;
        store.commit('vehicles/SET_VEHICLE_FAVORITE_STATE', {
            vehicleId: vid,
            liked: liked.value,
            likeTotal: likeTotal.value,
            likes: likes.value
        });
    } catch (e) {
        // ignore
    } finally {
        favoriteLoadingIds.delete(vid);
    }
}

const syncLike = async (id) => {
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
        favoriteCache[id] = resp || {};
        store.commit('vehicles/SET_VEHICLE_FAVORITE_STATE', {
            vehicleId: id,
            liked: liked.value,
            likeTotal: likeTotal.value,
            likes: likes.value
        });
    } catch (error) {
        const fallback = favoriteCache[id];
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
            scheduleLikeSync(id);
        }
    }
};

const scheduleLikeSync = (id) => {
    if (likeDebounceTimer) {
        clearTimeout(likeDebounceTimer);
    }
    likeDebounceTimer = setTimeout(() => syncLike(id), 250);
};

async function toggleFavoriteAction() {
    if (!vehicle.value?.id) return;
    if (!isAuthenticated.value) {
        ElMessage.info('请先登录再收藏');
        return;
    }
    const id = vehicle.value.id;
    const nextLiked = !liked.value;
    liked.value = nextLiked;
    likeTotal.value = Math.max(0, likeTotal.value + (nextLiked ? 1 : -1));
    if (nextLiked && currentUser.value) {
        const exists = likes.value.some((u) => u.id === currentUser.value.id);
        if (!exists) {
            likes.value = [
                {
                    id: currentUser.value.id,
                    name: currentUser.value.displayName || currentUser.value.username || '我',
                    isSelf: true
                },
                ...likes.value
            ].slice(0, 2);
        }
    }
    if (!nextLiked && currentUser.value) {
        likes.value = likes.value.filter((u) => u.id !== currentUser.value.id);
    }

    scheduleLikeSync(id);
}
const exifVisible = ref(false);

const loadComments = async () => {
    const id = vehicle.value?.id;
    if (!id || !isAuthenticated.value) {
        comments.value = [];
        commentsTotal.value = 0;
        return;
    }
    commentsLoading.value = true;
    try {
        const resp = await fetchVehicleComments(id, { page: 1, size: 50 });
        const records = resp?.records || resp?.items || resp?.data || resp || [];
        comments.value = records;
        commentsTotal.value = resp?.total ?? records.length ?? 0;
    } catch (error) {
        ElMessage.error(error.message || '加载评论失败');
    } finally {
        commentsLoading.value = false;
    }
};

const toggleComments = async () => {
    if (!isAuthenticated.value) {
        ElMessage.info('请先登录再查看评论');
        return;
    }
    commentsOpen.value = !commentsOpen.value;
    if (commentsOpen.value) {
        await loadComments();
    }
};

const submitComment = async () => {
    if (!isAuthenticated.value) {
        ElMessage.info('请先登录再发表评论');
        return;
    }
    const id = vehicle.value?.id;
    const text = commentInput.value?.trim();
    if (!id || !text) {
        ElMessage.warning('请输入评论内容');
        return;
    }
    commentSubmitting.value = true;
    try {
        await createVehicleComment(id, text);
        commentInput.value = '';
        await loadComments();
        ElMessage.success('评论已发布');
    } catch (error) {
        ElMessage.error(error.message || '发布失败');
    } finally {
        commentSubmitting.value = false;
    }
};

const commentsOpen = ref(false);
const comments = ref([]);
const commentsTotal = ref(0);
const commentsLoading = ref(false);
const commentInput = ref('');
const commentSubmitting = ref(false);

watch(
    () => props.visible,
    async (val) => {
        toggleBodyScroll(val);
        if (val && commentsOpen.value && isAuthenticated.value) {
            await loadComments();
        }
        if (!val) {
            commentsOpen.value = false;
            exifVisible.value = false;
        }
    },
    { immediate: true }
);

watch(
    () => vehicle.value?.id,
    async () => {
        likes.value = [];
        likeTotal.value = 0;
        liked.value = false;
        if (props.visible) {
            await loadFavoriteSummary();
        }
        if (props.visible && commentsOpen.value && isAuthenticated.value) {
            await loadComments();
        }
    },
    { immediate: true }
);

watch(
    isAuthenticated,
    async (authed) => {
        likes.value = [];
        likeTotal.value = 0;
        liked.value = false;
        if (props.visible) {
            await loadFavoriteSummary();
        }
        if (authed && commentsOpen.value && props.visible) {
            await loadComments();
        }
        if (!authed) {
            comments.value = [];
            commentsTotal.value = 0;
        }
    }
);

const toSnakeCase = (value = '') =>
    value
        .replace(/([A-Z])/g, (_, char) => _)
        .replace(/^_/, '');

const getValueByPath = (source, path) => {
    if (!source || !path) return undefined;
    return path.split('.').reduce((curr, segment) => {
        if (curr === undefined || curr === null) return undefined;
        if (curr[segment] !== undefined) return curr[segment];
        const snake = toSnakeCase(segment);
        return snake !== segment ? curr[snake] : undefined;
    }, source);
};

const getValueByPaths = (source, paths) => {
    const candidates = Array.isArray(paths) ? paths : [paths];
    for (const candidate of candidates) {
        const value = getValueByPath(source, candidate);
        if (value !== undefined && value !== null) {
            return value;
        }
    }
    if (source?.remark) {
        const remark = source.remark;
        if (candidates.includes('model.brand.name') || candidates.includes('brandName')) {
            const brandMatch = remark.match(/品牌：([^\n]+)/);
            if (brandMatch) return brandMatch[1];
        }
        if (candidates.includes('model.name') || candidates.includes('modelName')) {
            const modelMatch = remark.match(/车型：([^\n]+)/);
            if (modelMatch) return modelMatch[1];
        }
    }
    return undefined;
};

const extractVehicleField = (key) => {
    if (!vehicle.value) return undefined;
    switch (key) {
        case 'brandName':
            return getValueByPaths(vehicle.value, ['model.brand.name', 'model.brandName', 'brand.name', 'brandName']);
        case 'modelName':
            return getValueByPaths(vehicle.value, ['model.name', 'modelName']);
        case 'companyName':
            return getValueByPaths(vehicle.value, ['company.name', 'companyName']);
        case 'regionName':
            return getValueByPaths(vehicle.value, ['region.name', 'regionName']);
        case 'factoryDate':
            return getValueByPaths(vehicle.value, ['factoryDate', 'factory_date']);
        case 'launchDate':
            return getValueByPaths(vehicle.value, ['launchDate', 'launch_date']);
        case 'airConditioned':
            return getValueByPaths(vehicle.value, ['airConditioned', 'air_conditioned']);
        default:
            return getValueByPaths(vehicle.value, [key]);
    }
};

const formatVehicleField = (field, rawValue) => {
    if (field.type === 'date') {
        return rawValue ? formatYearMonth(rawValue) : '-';
    }
    if (field.type === 'boolean') {
        return formatBoolean(rawValue);
    }
    return rawValue ?? '-';
};

const rawFuelType = computed(() => {
    const fuel = config.value?.fuelType || config.value?.fuel_type || '';
    return String(fuel).toLowerCase();
});

const configFieldDefinitions = computed(() => {
    const fuel = rawFuelType.value;
    const hasElectric = fuel.includes('electric');
    const hasCombustion =
        fuel.includes('diesel') ||
        fuel.includes('gasoline') ||
        fuel.includes('oil') ||
        fuel.includes('lng') ||
        fuel.includes('cng') ||
        fuel.includes('gas');
    return CONFIG_INFO_FIELDS.filter((field) => {
        if (field.key === 'motor') {
            return hasElectric;
        }
        if (field.key === 'engine') {
            return hasCombustion;
        }
        return true;
    });
});

const extractConfigField = (key) => {
    if (!config.value) return undefined;
    switch (key) {
        case 'brandName':
            return (
                getValueByPaths(config.value, ['brand.name', 'brandName']) ||
                getValueByPaths(vehicle.value, ['model.brand.name', 'brandName'])
            );
        case 'modelName':
            return (
                getValueByPaths(config.value, ['model.name', 'modelName']) ||
                getValueByPaths(vehicle.value, ['model.name', 'modelName'])
            );
        default:
            return getValueByPaths(config.value, [key]);
    }
};

const formatConfigField = (field, rawValue) => {
    if (field.type === 'boolean') {
        return formatBoolean(rawValue);
    }
    if (field.key === 'fuelType') {
        return formatFuelType(rawValue);
    }
    if (field.type === 'date') {
        return rawValue ? formatYearMonth(rawValue) : '-';
    }
    return rawValue ?? '-';
};

const hasDisplayValue = (value, type) => {
    if (type === 'boolean') {
        return value !== null && value !== undefined;
    }
    if (value === null || value === undefined) return false;
    if (typeof value === 'string') {
        return value.trim().length > 0;
    }
    return true;
};

const vehicleInfoCards = computed(() =>
    VEHICLE_INFO_FIELDS.map((field) => {
        const raw = extractVehicleField(field.key);
        return {
            ...field,
            raw,
            value: formatVehicleField(field, raw)
        };
    }).filter((field) => hasDisplayValue(field.raw, field.type))
);

const configInfoCards = computed(() =>
    configFieldDefinitions.value
        .map((field) => {
            const raw = extractConfigField(field.key);
            return {
                ...field,
                raw,
                value: formatConfigField(field, raw)
            };
        })
        .filter((field) => hasDisplayValue(field.raw, field.type))
);

const getVehicleLink = (field) => {
    if (!field.link) return null;
    switch (field.link) {
        case 'region': {
            const id = vehicle.value?.region?.id;
            return id ? { name: 'RegionCatalog', params: { regionId: id } } : null;
        }
        case 'company': {
            const id = vehicle.value?.company?.id;
            return id ? { name: 'CompanyCatalog', params: { companyId: id } } : null;
        }
        default:
            return null;
    }
};

const getConfigLink = (field) => {
    if (!field.link) return null;
    switch (field.link) {
        case 'brand': {
            const id = config.value?.brandId || vehicle.value?.model?.brandId;
            return id ? { name: 'BrandCatalog', params: { brandId: id } } : null;
        }
        case 'model': {
            const id = config.value?.modelId || vehicle.value?.model?.id;
            return id ? { name: 'ModelCatalog', params: { modelId: id } } : null;
        }
        default:
            return null;
    }
};

const handleClose = () => {
    exifVisible.value = false;
    commentsOpen.value = false;
    emit('close');
};
</script>

<style scoped lang="scss">
:global(body.vehicle-detail-modal-open) {
    overflow: hidden;
    margin: 0;
}

.modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.8);
    backdrop-filter: blur(16px);
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    min-height: 100vh;
    overflow: hidden;
}

.modal {
    width: min(95%, 900px);
    background: rgba(255, 255, 255, 0.98);
    border-radius: 28px;
    padding: 24px;
    box-shadow: 0 30px 60px rgba(15, 23, 42, 0.25);
    display: flex;
    flex-direction: column;
    max-height: min(900px, calc(100vh - 48px));
    overflow-y: auto;
    position: relative;
}

.modal__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 12px;
    margin-bottom: 16px;
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.favorite-btn {
    border: 1px solid #fde68a;
    background: #fffbeb;
    color: #b45309;
    padding: 6px 10px;
    border-radius: 10px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s;
}

.favorite-btn:hover {
    border-color: #fbbf24;
    box-shadow: 0 10px 20px rgba(245, 158, 11, 0.16);
}

.favorite-btn:disabled {
    cursor: not-allowed;
    opacity: 0.6;
}

.favorite-count {
    margin-left: 4px;
    color: #92400e;
}

.comment-entry {
    border: 1px solid #2563eb;
    background: #eef2ff;
    color: #1d4ed8;
    border-radius: 10px;
    padding: 6px 10px;
    cursor: pointer;
    font-weight: 600;
}

.modal__headline {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.title-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.modal__eyebrow {
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.2em;
    font-size: 0.75rem;
    margin-bottom: 4px;
}

.info-badge {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: 1px solid rgba(148, 163, 184, 0.8);
    background: #fff;
    color: #64748b;
    font-weight: 600;
    font-size: 0.85rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s, border-color 0.2s;

    &:hover {
        background: rgba(148, 163, 184, 0.12);
        border-color: rgba(37, 99, 235, 0.5);
        color: #2563eb;
    }
}

.close-btn {
    border: none;
    background: #f1f5f9;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    font-size: 1.4rem;
    cursor: pointer;
}

.modal__content {
    flex: 1;
    overflow-y: auto;
    padding-right: 4px;
    scrollbar-width: none;
    -ms-overflow-style: none;
}

.modal__content::-webkit-scrollbar {
    display: none;
}

.modal__body {
    display: flex;
    flex-direction: column;
    gap: 24px;
    padding-bottom: 8px;
}

.comment-drawer {
    position: absolute;
    top: 16px;
    right: 16px;
    width: 320px;
    height: calc(100% - 32px);
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.15);
    display: flex;
    flex-direction: column;
    padding: 12px;
    z-index: 50;
}

.slide-enter-active,
.slide-leave-active {
    transition: all 0.2s ease;
}

.slide-enter-from,
.slide-leave-to {
    opacity: 0;
    transform: translateX(12px);
}

.comment-drawer__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
}

.comment-drawer__eyebrow {
    margin: 0;
    color: #94a3b8;
    font-size: 12px;
}

.comment-drawer__body {
    flex: 1;
    overflow-y: auto;
    padding-right: 4px;
}

.comment-item + .comment-item {
    margin-top: 10px;
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

.comment-empty {
    text-align: center;
    color: #94a3b8;
    margin: 12px 0;
}

.comment-drawer__footer {
    border-top: 1px solid #e2e8f0;
    padding-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.comment-submit {
    align-self: flex-end;
}

.image-section__viewer {
    width: 100%;
    background: radial-gradient(circle at top, rgba(15, 23, 42, 0.9), rgba(15, 23, 42, 0.98));
    border-radius: 24px;
    padding: clamp(12px, 2vw, 20px);
    min-height: clamp(240px, 40vh, 480px);
    box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.15);
    position: relative;
}

.image-section__empty {
    padding: 64px 0;
    text-align: center;
    color: #94a3b8;
    border: 1px dashed #dbe3f3;
    border-radius: 20px;
}

.favorite-users {
    margin-top: 10px;
    color: #475569;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    font-size: 14px;
}

.favorite-label {
    font-weight: 600;
    color: #0f172a;
}

.favorite-user {
    background: #eef2ff;
    color: #1d4ed8;
    padding: 2px 8px;
    border-radius: 10px;
    font-weight: 600;
}

.favorite-more {
    color: #94a3b8;
}

.variant-nav {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 44px;
    height: 44px;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.25);
    background: rgba(15, 23, 42, 0.75);
    color: #fff;
    font-size: 1.6rem;
    cursor: pointer;
    z-index: 6;
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.25);
}

.variant-nav--prev {
    left: 12px;
}

.variant-nav--next {
    right: 12px;
}

.info-section h3 {
    margin-bottom: 10px;
    color: #0f172a;
    font-size: 1rem;
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
    gap: 12px;
}

.info-item {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    min-height: 72px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.info-item span {
    display: block;
    font-size: 0.8rem;
    color: #94a3b8;
}

.info-item strong {
    display: block;
    margin-top: 6px;
    font-size: 0.95rem;
    word-break: break-word;
    color: #0f172a;
}

.info-link,
.info-link:visited {
    color: #2563eb;
    text-decoration: none;
}

.info-value {
    color: #0f172a;
}

.modal__state {
    padding: 60px 0;
    text-align: center;
    color: #475569;
}

.exif-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.7);
    backdrop-filter: blur(6px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    z-index: 120;
}

.exif-modal {
    width: min(400px, 100%);
    background: #fff;
    border-radius: 24px;
    padding: 20px;
    box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.exif-modal__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
}

.exif-modal__body {
    max-height: 60vh;
    overflow-y: auto;
}

.exif-row {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 8px 0;
    border-bottom: 1px solid #f1f5f9;

    dt {
        font-weight: 600;
        color: #475569;
    }

    dd {
        margin: 0;
        color: #1f2937;
        text-align: right;
    }
}

.exif-empty {
    text-align: center;
    color: #94a3b8;
    margin: 0;
}
</style>
