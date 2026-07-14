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
                        <button
                            class="favorite-btn"
                            type="button"
                            :disabled="likeLoading"
                            :aria-label="liked ? '取消收藏' : '收藏车辆'"
                            @click="toggleFavoriteAction"
                        >
                            <span class="btn-icon" aria-hidden="true">{{ liked ? '★' : '☆' }}</span>
                            <span class="btn-text">{{ liked ? '已收藏' : '收藏' }}</span>
                            <span v-if="likeTotal" class="favorite-count">({{ likeTotal }})</span>
                        </button>
                        <button class="close-btn close-btn--modal" type="button" @click="handleClose">×</button>
                    </div>
                </header>

                <div class="modal__content" :class="{ 'modal__content--with-comments': !isMobileCommentsMode }">
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

                        <GroupBuyBridgeCard
                            :class="[
                                'trade-bridge-card',
                                { 'trade-bridge-card--hide-teams': tradeVisibleTeams.length > 0 }
                            ]"
                            :context="tradeBridgeContext"
                            :market-config="tradeMarketConfig"
                            :active-teams="tradeVisibleTeams"
                            :loading="tradeLoading"
                            @buy="handleTradeBuy"
                            @group-buy="handleTradeGroupBuy"
                            @join-team="handleTradeJoinTeam"
                        />
                        <section v-if="tradeVisibleTeams.length" class="trade-team-times">
                            <p class="trade-team-times__title">正在拼团（截止时间）</p>
                            <div class="trade-team-times__list">
                                <article v-for="team in tradeVisibleTeams" :key="`detail-team-${team.teamId}`" class="trade-team-times__item">
                                    <div class="trade-team-times__meta">
                                        <span>团号 {{ team.teamId }}</span>
                                        <span>进度 {{ team.completeCount || 0 }}/{{ team.targetCount || '-' }} 人</span>
                                        <span>截止 {{ formatTradeEndTime(team.validEndTime) }}</span>
                                        <span class="trade-team-times__countdown">{{ formatTradeCountdown(team.validEndTime) }}</span>
                                    </div>
                                    <button type="button" class="trade-team-times__join" @click="handleTradeJoinTeam(team.teamId)">
                                        参与该团
                                    </button>
                                </article>
                            </div>
                        </section>

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

                        <div class="info-section" v-if="routeCards.length">
                            <h3>运营线路</h3>
                            <div class="route-list">
                                <div v-for="r in routeCards" :key="'r-' + r.routeId"
                                     class="route-item"
                                     :class="{ 'route-item--history': !r.isCurrent }">
                                    <div class="route-item__head">
                                        <router-link v-if="r.routeId" class="route-link" :to="{ name: 'RouteDetail', params: { routeId: r.routeId } }">
                                            {{ r.routeNumber }}
                                        </router-link>
                                        <strong v-else>{{ r.routeNumber }}</strong>
                                        <el-tag size="small" :type="r.isCurrent ? 'success' : 'info'">
                                            {{ r.isCurrent ? '当前' : '历史' }}
                                        </el-tag>
                                        <el-tag v-if="r.subType" size="small" type="warning">
                                            {{ subTypeLabel(r.subType) }}
                                        </el-tag>
                                        <el-tag v-if="r.isLoop" size="small" type="info">环线</el-tag>
                                    </div>
                                    <div class="route-item__stops">
                                        <template v-if="r.downStartStop || r.downEndStop">
                                            上行 {{ r.startStop || '?' }} → {{ r.endStop || '?' }}
                                            | 下行 {{ r.downStartStop || r.endStop || '?' }} → {{ r.downEndStop || r.startStop || '?' }}
                                        </template>
                                        <template v-else>
                                            {{ r.startStop || '?' }} ↔ {{ r.endStop || '?' }}
                                        </template>
                                    </div>
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

                        <section
                            v-if="isMobileCommentsMode"
                            class="info-section mobile-comments"
                        >
                            <div class="mobile-comments__head">
                                <h3>评论</h3>
                                <span class="mobile-comments__count">{{ commentsTotal || 0 }} 条</span>
                            </div>
                            <div class="mobile-comments__body" v-loading="commentsLoading">
                                <p v-if="!isAuthenticated" class="comment-empty">登录后查看评论</p>
                                <template v-else>
                                    <p v-if="!comments.length && !commentsLoading" class="comment-empty">暂无评论</p>
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
                                </template>
                            </div>
                            <div class="mobile-comments__footer" v-if="isAuthenticated">
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
                        </section>
                    </section>
                    <aside v-if="!isMobileCommentsMode" class="comment-drawer">
                        <header class="comment-drawer__header">
                            <div>
                                <p class="comment-drawer__eyebrow">评论</p>
                                <h4>{{ commentsTotal || 0 }} 条</h4>
                            </div>
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
                </div>
            </div>
        </div>
        <el-dialog
            v-model="directCheckoutVisible"
            title="订单结算确认"
            width="520px"
            append-to-body
            destroy-on-close
        >
            <div class="direct-checkout-panel">
                <p>购买方式：<strong>{{ tradeCheckoutMode === 'direct-buy' ? '直接下单' : '拼团购买' }}</strong></p>
                <p>支付金额：<strong>{{ centsToYuan(checkoutPayCents) }}</strong></p>
                <p>当前余额：<strong>{{ centsToYuan(balanceCents) }}</strong></p>

                <div class="direct-payment-options">
                    <button
                        v-for="option in directPaymentOptions"
                        :key="option.value"
                        type="button"
                        class="direct-payment-option"
                        :class="{ active: directPaymentMethod === option.value, disabled: !option.available }"
                        @click="selectDirectPaymentMethod(option)"
                    >
                        <span>{{ option.label }}</span>
                        <small>{{ option.desc }}</small>
                    </button>
                </div>

                <el-alert
                    v-if="directPaymentMethod !== 'BALANCE'"
                    type="warning"
                    show-icon
                    :closable="false"
                    title="支付宝/微信暂未接入，请使用余额支付"
                />
                <el-alert
                    v-else-if="!directHasSufficientBalance"
                    type="warning"
                    show-icon
                    :closable="false"
                    title="余额不足，请充值后再试"
                />

                <el-input
                    v-model="directPayPassword"
                    type="password"
                    show-password
                    maxlength="64"
                    placeholder="请输入登录密码进行支付验证"
                />
            </div>
            <template #footer>
                <el-button @click="directCheckoutVisible = false">取消</el-button>
                <el-button
                    type="primary"
                    :loading="directSubmitting"
                    :disabled="!directReadyToSubmit"
                    @click="submitTradeCheckout"
                >
                    确认支付
                </el-button>
            </template>
        </el-dialog>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import ImageCarousel from './ImageCarousel.vue';
import GroupBuyBridgeCard from '@/components/Trade/GroupBuyBridgeCard.vue';
import { setFavorite } from '@/api/vehicles';
import http from '@/api/axiosInstance';
import {
    directBuyCheckout,
    groupBuyCheckout,
} from '@/api/tradeBridge';
import { verifyPassword } from '@/api/auth';
import { CONFIG_INFO_FIELDS, VEHICLE_INFO_FIELDS } from '@/utils/constants';
import { formatBoolean, formatFuelType, formatYearMonth } from '@/utils/formatters';
import { isCombustionFuel, isElectricFuel, normalizeFuelType } from '@/utils/fuel';

const props = defineProps({
    visible: Boolean,
    detail: {
        type: Object,
        default: () => null
    },
    initialImageId: {
        type: [Number, String],
        default: null
    },
    loading: Boolean
});

const emit = defineEmits(['close']);
const router = useRouter();
const store = useStore();
const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const currentUser = computed(() => store.state.auth.profile);
const currentUserId = computed(() => currentUser.value?.id || currentUser.value?.userId || null);
const currentUserRole = computed(() => currentUser.value?.role || '');
const balanceCents = computed(() => Number(currentUser.value?.balanceCents || 0));
const unwrapData = (payload) => payload?.data ?? payload ?? null;
const isCanceledError = (error) =>
    error?.name === 'CanceledError' || error?.name === 'AbortError' || error?.code === 'ERR_CANCELED';

let tradeRequestSeq = 0;
let favoriteRequestSeq = 0;
let commentsRequestSeq = 0;
let tradeAbortController = null;
let favoriteAbortController = null;
let commentsAbortController = null;

const resetAbortController = (controllerRefName) => {
    if (controllerRefName === 'trade') {
        if (tradeAbortController) tradeAbortController.abort();
        tradeAbortController = new AbortController();
        return tradeAbortController;
    }
    if (controllerRefName === 'favorite') {
        if (favoriteAbortController) favoriteAbortController.abort();
        favoriteAbortController = new AbortController();
        return favoriteAbortController;
    }
    if (commentsAbortController) commentsAbortController.abort();
    commentsAbortController = new AbortController();
    return commentsAbortController;
};

const abortModalRequests = () => {
    if (tradeAbortController) tradeAbortController.abort();
    if (favoriteAbortController) favoriteAbortController.abort();
    if (commentsAbortController) commentsAbortController.abort();
    tradeAbortController = null;
    favoriteAbortController = null;
    commentsAbortController = null;
    tradeLoading.value = false;
    commentsLoading.value = false;
};

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
const findVariantIndexByVehicleId = (vehicleId) =>
    variants.value.findIndex((variant) => variant?.vehicle?.id === vehicleId);

const currentImageIndex = ref(0);
const currentVariantIndex = ref(0);

const vehicle = computed(() => variants.value[currentVariantIndex.value]?.vehicle || null);
const config = computed(
    () => variants.value[currentVariantIndex.value]?.vehicleConfig || props.detail?.vehicleConfig || {}
);
const tradeResolved = reactive({
    goodsId: null,
    activityId: null
});
const tradeMarketConfig = ref(null);
const tradeActiveTeams = ref([]);
const tradeLoading = ref(false);
const tradeNowTimestamp = ref(Date.now());
const TRADE_CLOCK_INTERVAL_MS = 1000;
const TRADE_SNAPSHOT_POLL_INTERVAL_MS = 20000;
let tradeClockTimer = null;
let tradeSnapshotPollTimer = null;
const directCheckoutVisible = ref(false);
const tradeCheckoutMode = ref('direct-buy');
const directPaymentMethod = ref('BALANCE');
const directPayPassword = ref('');
const directSubmitting = ref(false);
const directPaymentOptions = [
    { value: 'ALIPAY', label: '支付宝', desc: '扫码支付', available: false },
    { value: 'WECHAT', label: '微信支付', desc: '扫码支付', available: false },
    { value: 'BALANCE', label: '余额支付', desc: '即时扣款', available: true }
];
const checkoutPayCents = computed(() =>
    tradeCheckoutMode.value === 'direct-buy'
        ? Number(tradeMarketConfig.value?.originalPriceCents || 0)
        : Number(tradeMarketConfig.value?.groupPriceCents || 0)
);
const directHasSufficientBalance = computed(() => balanceCents.value >= checkoutPayCents.value);
const directReadyToSubmit = computed(() => {
    if (!tradeBridgeContext.value?.goodsId || !tradeBridgeContext.value?.activityId) return false;
    if (directPaymentMethod.value !== 'BALANCE') return false;
    if (!directHasSufficientBalance.value) return false;
    if (!directPayPassword.value.trim()) return false;
    if (directSubmitting.value) return false;
    return true;
});

const tradeBridgeContext = computed(() => {
    const currentImage = images.value[currentImageIndex.value] || images.value[0] || null;
    const currentVehicle = vehicle.value;
    const modelId = currentVehicle?.model?.id || config.value?.modelId || null;
    const brandId = currentVehicle?.model?.brandId || config.value?.brandId || null;
    const candidateGoodsId =
        tradeResolved.goodsId ||
        currentImage?.goodsId ||
        currentImage?.tradeGoodsId ||
        currentVehicle?.goodsId ||
        currentVehicle?.tradeGoodsId ||
        null;
    const candidateActivityId =
        tradeResolved.activityId ||
        currentImage?.activityId ||
        currentImage?.tradeActivityId ||
        currentVehicle?.activityId ||
        currentVehicle?.tradeActivityId ||
        null;

    return {
        vehicleId: currentVehicle?.id || null,
        plateNumber: currentVehicle?.plateNumber || '',
        imageId: currentImage?.id || null,
        goodsId: candidateGoodsId,
        activityId: candidateActivityId,
        source: 'content',
        channel: 'web',
        meta: {
            modelId,
            brandId
        }
    };
});

const parseDateMs = (value) => {
    if (!value) return 0;
    if (typeof value === 'number') return Number.isFinite(value) ? value : 0;
    if (Array.isArray(value)) {
        const year = Number(value[0] || 0);
        const month = Number(value[1] || 0);
        const day = Number(value[2] || 0);
        const hour = Number(value[3] || 0);
        const minute = Number(value[4] || 0);
        const second = Number(value[5] || 0);
        if (year > 0 && month > 0 && day > 0) {
            const dt = new Date(year, Math.max(0, month - 1), day, hour, minute, second);
            const ts = dt.getTime();
            return Number.isFinite(ts) ? ts : 0;
        }
        return 0;
    }
    const t = Date.parse(value);
    return Number.isFinite(t) ? t : 0;
};

const isTradeTeamActive = (team) => {
    const targetCount = Number(team?.targetCount || 0);
    const completeCount = Number(team?.completeCount || 0);
    if (targetCount > 0 && completeCount >= targetCount) {
        return false;
    }
    const endMs = parseDateMs(team?.validEndTime);
    if (endMs > 0 && endMs <= tradeNowTimestamp.value) {
        return false;
    }
    return true;
};

const tradeVisibleTeams = computed(() =>
    (Array.isArray(tradeActiveTeams.value) ? tradeActiveTeams.value : [])
        .filter((team) => isTradeTeamActive(team))
);

const formatTradeEndTime = (endTime) => {
    const ts = parseDateMs(endTime);
    if (!ts) return '--';
    const dt = new Date(ts);
    const y = dt.getFullYear();
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const d = String(dt.getDate()).padStart(2, '0');
    const hh = String(dt.getHours()).padStart(2, '0');
    const mm = String(dt.getMinutes()).padStart(2, '0');
    const ss = String(dt.getSeconds()).padStart(2, '0');
    return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
};

const formatTradeCountdown = (endTime) => {
    const endMs = parseDateMs(endTime);
    if (!endMs) return '倒计时 --:--';
    const diff = Math.floor((endMs - tradeNowTimestamp.value) / 1000);
    if (diff <= 0) return '已结束';
    const hours = Math.floor(diff / 3600);
    const minutes = Math.floor((diff % 3600) / 60);
    const seconds = diff % 60;
    if (hours > 0) {
        return `剩余 ${hours}h ${String(minutes).padStart(2, '0')}m`;
    }
    return `剩余 ${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
};

const resetFocus = () => {
    let targetVariant = 0;
    let targetImageIndex = -1;

    if (props.initialImageId != null) {
        targetImageIndex = images.value.findIndex(
            (image) => String(image?.id) === String(props.initialImageId)
        );
        if (targetImageIndex >= 0) {
            targetVariant = images.value[targetImageIndex]?.__variantIndex || 0;
        }
    }

    if (targetImageIndex < 0) {
        const detailVehicleId = props.detail?.vehicle?.id;
        const variantIndex = findVariantIndexByVehicleId(detailVehicleId);
        targetVariant = variantIndex >= 0 ? variantIndex : 0;
        targetImageIndex = findFirstImageIndexForVariant(targetVariant);
    }

    currentVariantIndex.value = targetVariant;
    currentImageIndex.value = targetImageIndex >= 0 ? targetImageIndex : 0;
};

watch(
    () => [props.detail, props.initialImageId],
    () => {
        tradeResolved.goodsId = null;
        tradeResolved.activityId = null;
        tradeMarketConfig.value = null;
        tradeActiveTeams.value = [];
        resetFocus();
    }
);

const requestTradeBinding = (imageId, payload, signal) =>
    http.post(`/bridge/images/${imageId}/binding`, payload || {}, { signal }).then(unwrapData);

const requestTradeConfig = (payload, signal) =>
    http.post('/bridge/index/config', payload || {}, { signal }).then(unwrapData);

const requestActiveTeams = (activityId, limit, signal) =>
    http.get('/bridge/portal/teams', {
        params: activityId ? { activityId, limit } : { limit },
        signal
    }).then(unwrapData);

const loadTradeSnapshot = async () => {
    if (!props.visible) return;
    const imageId = tradeBridgeContext.value?.imageId;
    const vehicleId = tradeBridgeContext.value?.vehicleId;
    if (!imageId) {
        tradeResolved.goodsId = null;
        tradeResolved.activityId = null;
        tradeMarketConfig.value = null;
        tradeActiveTeams.value = [];
        return;
    }
    const requestSeq = ++tradeRequestSeq;
    const controller = resetAbortController('trade');
    const signal = controller.signal;
    tradeLoading.value = true;
    try {
        let goodsId = tradeBridgeContext.value?.goodsId ? String(tradeBridgeContext.value.goodsId) : null;
        let activityId = tradeBridgeContext.value?.activityId ? String(tradeBridgeContext.value.activityId) : null;

        if ((!goodsId || !activityId) && isAuthenticated.value) {
            const binding = await requestTradeBinding(
                Number(imageId),
                vehicleId ? { vehicleId: Number(vehicleId) } : {},
                signal
            );
            if (requestSeq !== tradeRequestSeq || signal.aborted || !props.visible) {
                return;
            }
            goodsId = binding?.goodsId ? String(binding.goodsId) : goodsId;
            activityId = binding?.activityId ? String(binding.activityId) : activityId;
            if (binding) {
                tradeMarketConfig.value = {
                    goodsId: binding.goodsId,
                    activityId: binding.activityId,
                    goodsTitle: binding.goodsTitle,
                    originalPriceCents: binding.originalPriceCents,
                    groupPriceCents: binding.groupPriceCents,
                    targetCount: binding.targetCount,
                    validMinutes: binding.validMinutes,
                    activeTeamCount: binding.activeTeamCount
                };
            }
        }

        if (goodsId) {
            const configResp = await requestTradeConfig({ goodsId }, signal);
            if (requestSeq !== tradeRequestSeq || signal.aborted || !props.visible) {
                return;
            }
            if (configResp) {
                tradeMarketConfig.value = configResp;
                activityId = configResp.activityId ? String(configResp.activityId) : activityId;
            }
        }

        tradeResolved.goodsId = goodsId;
        tradeResolved.activityId = activityId;

        if (activityId) {
            let teams = await requestActiveTeams(Number(activityId), 20, signal).catch((error) => {
                if (isCanceledError(error)) {
                    throw error;
                }
                return [];
            });
            if ((!Array.isArray(teams) || !teams.length) && !signal.aborted) {
                const globalTeams = await requestActiveTeams(null, 100, signal).catch((error) => {
                    if (isCanceledError(error)) {
                        throw error;
                    }
                    return [];
                });
                teams = Array.isArray(globalTeams)
                    ? globalTeams.filter((team) => Number(team?.activityId || 0) === Number(activityId || 0))
                    : [];
            }
            if (requestSeq !== tradeRequestSeq || signal.aborted || !props.visible) {
                return;
            }
            tradeActiveTeams.value = Array.isArray(teams) ? teams : [];
        } else {
            tradeActiveTeams.value = [];
        }
    } catch (error) {
        if (isCanceledError(error)) {
            return;
        }
        if (requestSeq !== tradeRequestSeq) {
            return;
        }
        tradeActiveTeams.value = [];
    } finally {
        if (requestSeq === tradeRequestSeq && !signal.aborted) {
            tradeLoading.value = false;
        }
    }
};

const startTradeClock = () => {
    if (tradeClockTimer) return;
    tradeClockTimer = setInterval(() => {
        if (!props.visible) return;
        tradeNowTimestamp.value = Date.now();
    }, TRADE_CLOCK_INTERVAL_MS);
};

const stopTradeClock = () => {
    if (tradeClockTimer) {
        clearInterval(tradeClockTimer);
        tradeClockTimer = null;
    }
};

const startTradeSnapshotPolling = () => {
    if (tradeSnapshotPollTimer || !isDocumentVisible()) return;
    tradeSnapshotPollTimer = setInterval(() => {
        if (!props.visible || !isDocumentVisible()) return;
        loadTradeSnapshot().catch(() => {});
    }, TRADE_SNAPSHOT_POLL_INTERVAL_MS);
};

const stopTradeSnapshotPolling = () => {
    if (tradeSnapshotPollTimer) {
        clearInterval(tradeSnapshotPollTimer);
        tradeSnapshotPollTimer = null;
    }
};

watch(
    () => [props.visible, tradeBridgeContext.value?.imageId, tradeBridgeContext.value?.vehicleId],
    ([visible]) => {
        if (!visible) return;
        loadTradeSnapshot();
    },
    { immediate: true }
);

watch(
    () => isAuthenticated.value,
    (val) => {
        if (val && props.visible) {
            loadTradeSnapshot();
        }
    }
);

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
    const nextVariantIndex = (currentVariantIndex.value - 1 + total) % total;
    currentVariantIndex.value = nextVariantIndex;
    const firstIdx = findFirstImageIndexForVariant(nextVariantIndex);
    if (firstIdx >= 0) {
        currentImageIndex.value = firstIdx;
    }
};

const nextVariant = () => {
    const total = variants.value.length;
    if (total < 2) return;
    const nextVariantIndex = (currentVariantIndex.value + 1) % total;
    currentVariantIndex.value = nextVariantIndex;
    const firstIdx = findFirstImageIndexForVariant(nextVariantIndex);
    if (firstIdx >= 0) {
        currentImageIndex.value = firstIdx;
    }
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
    const requestSeq = ++favoriteRequestSeq;
    const controller = resetAbortController('favorite');
    const signal = controller.signal;
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
    if (favoriteCache[vid]) {
        const cached = favoriteCache[vid];
        liked.value = cached.liked || false;
        likeTotal.value = cached.total || 0;
        likes.value = mapUsers(cached.topUsers || []).slice(0, 2);
        syncedLiked.value = liked.value;
    }
    const shouldFetchRemote = isAuthenticated.value || !favoriteCache[vid];
    if (!shouldFetchRemote || favoriteLoadingIds.has(vid)) return;
    favoriteLoadingIds.add(vid);
    try {
        const resp = await http.get(`/favorites/${vid}/summary`, { signal }).then(unwrapData);
        if (requestSeq !== favoriteRequestSeq || signal.aborted || !props.visible || Number(vehicle.value?.id || 0) !== Number(vid)) {
            return;
        }
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
        if (isCanceledError(e)) {
            return;
        }
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
        const resp = await setFavorite(id, liked.value);
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

const buildTradeActionPayload = (mode, teamId) => ({
    mode,
    ...tradeBridgeContext.value,
    teamId: teamId || null,
    userId: currentUserId.value
});

const centsToYuan = (cents) => `${(Number(cents || 0) / 100).toFixed(2)} 元`;

const selectDirectPaymentMethod = (option) => {
    if (!option.available) {
        ElMessage.info(`${option.label}暂未接入，请使用余额支付`);
        return;
    }
    directPaymentMethod.value = option.value;
};

const openDirectCheckoutDialog = () => {
    if (!isAuthenticated.value) {
        ElMessage.info('请先登录后再下单');
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } });
        return;
    }
    const goodsId = tradeBridgeContext.value?.goodsId;
    const activityId = tradeBridgeContext.value?.activityId;
    if (!goodsId || !activityId) {
        ElMessage.warning('当前图片还未绑定交易信息');
        return;
    }
    tradeCheckoutMode.value = 'direct-buy';
    directPaymentMethod.value = 'BALANCE';
    directPayPassword.value = '';
    directCheckoutVisible.value = true;
};

const openGroupCheckoutDialog = () => {
    if (!isAuthenticated.value) {
        ElMessage.info('请先登录后再下单');
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } });
        return;
    }
    const goodsId = tradeBridgeContext.value?.goodsId;
    const activityId = tradeBridgeContext.value?.activityId;
    if (!goodsId || !activityId) {
        ElMessage.warning('当前图片还未绑定交易信息');
        return;
    }
    tradeCheckoutMode.value = 'group-buy';
    directPaymentMethod.value = 'BALANCE';
    directPayPassword.value = '';
    directCheckoutVisible.value = true;
};

const submitTradeCheckout = async () => {
    if (!directReadyToSubmit.value) {
        ElMessage.warning('请先完成支付方式和密码校验');
        return;
    }
    directSubmitting.value = true;
    try {
        await verifyPassword({ currentPassword: directPayPassword.value.trim() });
        const payload = {
            goodsId: String(tradeBridgeContext.value.goodsId),
            activityId: Number(tradeBridgeContext.value.activityId),
            source: 'content',
            channel: 'balance'
        };
        const result = tradeCheckoutMode.value === 'direct-buy'
            ? await directBuyCheckout(payload)
            : await groupBuyCheckout({ ...payload, teamId: null });
        directCheckoutVisible.value = false;
        directPayPassword.value = '';
        await store.dispatch('auth/fetchProfile').catch(() => {});
        await loadTradeSnapshot();

        if (tradeCheckoutMode.value === 'group-buy') {
            ElMessage.success(result?.message || '支付成功，已进入拼团大厅');
            emit('close');
            router.push({
                name: 'GroupBuyMarket',
                query: {
                    vehicleId: tradeBridgeContext.value?.vehicleId ? String(tradeBridgeContext.value.vehicleId) : undefined,
                    imageId: tradeBridgeContext.value?.imageId ? String(tradeBridgeContext.value.imageId) : undefined,
                    plateNumber: tradeBridgeContext.value?.plateNumber || undefined,
                    goodsId: tradeBridgeContext.value?.goodsId ? String(tradeBridgeContext.value.goodsId) : undefined,
                    activityId: tradeBridgeContext.value?.activityId ? String(tradeBridgeContext.value.activityId) : undefined,
                    teamId: result?.teamId ? String(result.teamId) : undefined,
                    source: 'content',
                    channel: 'web'
                }
            });
            return;
        }

        if (result?.canDownload && result?.recordId) {
            ElMessage.success('支付成功，可在“查看购买记录”中下载原图');
            return;
        }
        ElMessage.success(result?.message || '支付成功');
    } catch (error) {
        ElMessage.error(error?.message || '支付失败');
    } finally {
        directSubmitting.value = false;
    }
};

const openGroupTradePage = (mode, teamId) => {
    const payload = buildTradeActionPayload(mode, teamId);
    if (!payload.imageId) {
        ElMessage.warning('当前图片还未绑定交易信息');
        return;
    }
    const query = {
        action: mode,
        autoCheckout: mode === 'direct-buy' || Boolean(teamId) ? '1' : undefined,
        vehicleId: payload.vehicleId ? String(payload.vehicleId) : undefined,
        imageId: payload.imageId ? String(payload.imageId) : undefined,
        plateNumber: payload.plateNumber || undefined,
        goodsId: payload.goodsId ? String(payload.goodsId) : undefined,
        activityId: payload.activityId ? String(payload.activityId) : undefined,
        teamId: payload.teamId ? String(payload.teamId) : undefined,
        source: payload.source || 'content',
        channel: payload.channel || 'web'
    };
    router.push({ name: 'GroupBuyMarket', query });
    emit('close');
};

const handleTradeBuy = () => {
    openDirectCheckoutDialog();
};

const handleTradeGroupBuy = () => {
    openGroupCheckoutDialog();
};

const handleTradeJoinTeam = (teamId) => {
    openGroupTradePage('group-buy', teamId);
};

const SUB_TYPE_LABELS = { INTERVAL: '区间', BRANCH: '支线', EXPRESS: '快线', NIGHT: '夜班', DIRECT: '直达' };
const subTypeLabel = (v) => SUB_TYPE_LABELS[v] || v;

const routeCards = computed(() => {
    const routes = vehicle.value?.routes || [];
    return routes.map(r => ({
        ...r,
        isCurrent: r.isCurrent
    }));
});

const exifVisible = ref(false);

const COMMENT_PAGE_SIZE = 50;
const COMMENT_POLL_INTERVAL_MS = 25000;
let commentPollTimer = null;
const isDocumentVisible = () =>
    typeof document === 'undefined' || document.visibilityState === 'visible';
const MOBILE_BREAKPOINT = 768;
const isMobileCommentsMode = ref(false);

const updateMobileCommentsMode = () => {
    if (typeof window === 'undefined') return;
    isMobileCommentsMode.value = window.innerWidth <= MOBILE_BREAKPOINT;
};

const syncCommentsFromCache = (vehicleId) => {
    const cached = store.state.vehicles?.commentCache?.[vehicleId];
    if (!cached) return;
    comments.value = cached.records || [];
    commentsTotal.value = cached.total ?? cached.records?.length ?? 0;
};

const loadComments = async ({ force = false } = {}) => {
    const id = vehicle.value?.id;
    if (!id || !isAuthenticated.value) {
        comments.value = [];
        commentsTotal.value = 0;
        return;
    }
    const requestSeq = ++commentsRequestSeq;
    const controller = resetAbortController('comments');
    const signal = controller.signal;
    commentsLoading.value = true;
    try {
        const resp = await store.dispatch('vehicles/loadVehicleComments', {
            vehicleId: id,
            page: 1,
            size: COMMENT_PAGE_SIZE,
            force,
            signal
        });
        if (requestSeq !== commentsRequestSeq || signal.aborted || !props.visible || Number(vehicle.value?.id || 0) !== Number(id)) {
            return;
        }
        comments.value = resp?.records || [];
        commentsTotal.value = resp?.total ?? comments.value.length ?? 0;
    } catch (error) {
        if (isCanceledError(error)) {
            return;
        }
        ElMessage.error(error.message || '加载评论失败');
    } finally {
        if (requestSeq === commentsRequestSeq && !signal.aborted) {
            commentsLoading.value = false;
        }
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
        await store.dispatch('vehicles/addVehicleComment', { vehicleId: id, content: text });
        commentInput.value = '';
        syncCommentsFromCache(id);
        ElMessage.success('评论已发布');
    } catch (error) {
        ElMessage.error(error.message || '发布失败');
    } finally {
        commentSubmitting.value = false;
    }
};

const comments = ref([]);
const commentsTotal = ref(0);
const commentsLoading = ref(false);
const commentInput = ref('');
const commentSubmitting = ref(false);
const deletingCommentIds = ref(new Set());

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
    const vehicleId = vehicle.value?.id;
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

const startCommentPolling = () => {
    if (commentPollTimer || !isDocumentVisible()) return;
    commentPollTimer = setInterval(() => {
        if (!props.visible || !isAuthenticated.value || !isDocumentVisible()) return;
        loadComments({ force: true });
    }, COMMENT_POLL_INTERVAL_MS);
};

const stopCommentPolling = () => {
    if (commentPollTimer) {
        clearInterval(commentPollTimer);
        commentPollTimer = null;
    }
};

const onDocumentVisibilityChange = () => {
    if (!props.visible) {
        stopCommentPolling();
        stopTradeSnapshotPolling();
        return;
    }
    if (!isDocumentVisible()) {
        stopCommentPolling();
        stopTradeSnapshotPolling();
        return;
    }
    if (isAuthenticated.value) {
        startCommentPolling();
        loadComments({ force: true }).catch(() => {});
    } else {
        stopCommentPolling();
    }
    startTradeSnapshotPolling();
    loadTradeSnapshot().catch(() => {});
};

watch(
    () => props.visible,
    async (val) => {
        toggleBodyScroll(val);
        if (val) {
            tradeNowTimestamp.value = Date.now();
            startTradeClock();
            startTradeSnapshotPolling();
            await loadFavoriteSummary();
            if (isAuthenticated.value) {
                await loadComments();
                startCommentPolling();
            }
        }
        if (!val) {
            exifVisible.value = false;
            directCheckoutVisible.value = false;
            directPayPassword.value = '';
            tradeCheckoutMode.value = 'direct-buy';
            stopTradeClock();
            stopTradeSnapshotPolling();
            stopCommentPolling();
            abortModalRequests();
        }
    },
    { immediate: true }
);

watch(
    () => directCheckoutVisible.value,
    (val) => {
        if (!val) {
            directPayPassword.value = '';
            directPaymentMethod.value = 'BALANCE';
            tradeCheckoutMode.value = 'direct-buy';
        }
    }
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
        if (props.visible && isAuthenticated.value) {
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
        if (authed && props.visible) {
            await loadComments();
            startCommentPolling();
        }
        if (!authed) {
            comments.value = [];
            commentsTotal.value = 0;
            stopCommentPolling();
        }
    },
    { immediate: true }
);

onMounted(() => {
    updateMobileCommentsMode();
    if (typeof window !== 'undefined') {
        window.addEventListener('resize', updateMobileCommentsMode, { passive: true });
    }
    if (typeof document !== 'undefined') {
        document.addEventListener('visibilitychange', onDocumentVisibilityChange);
    }
});

watch(
    isMobileCommentsMode,
    async () => {
        if (!props.visible) return;
        if (isAuthenticated.value) {
            await loadComments();
            startCommentPolling();
        }
    },
    { immediate: true }
);

onBeforeUnmount(() => {
    if (typeof window !== 'undefined') {
        window.removeEventListener('resize', updateMobileCommentsMode);
    }
    if (typeof document !== 'undefined') {
        document.removeEventListener('visibilitychange', onDocumentVisibilityChange);
    }
    stopCommentPolling();
    stopTradeClock();
    stopTradeSnapshotPolling();
    abortModalRequests();
    if (likeDebounceTimer) {
        clearTimeout(likeDebounceTimer);
        likeDebounceTimer = null;
    }
});

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
            return getValueByPaths(vehicle.value, ['model.brand.chnName', 'model.brandChnName', 'model.brand.name', 'model.brandName', 'brand.name', 'brandName']);
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
    return normalizeFuelType(fuel);
});

const configFieldDefinitions = computed(() => {
    const fuel = rawFuelType.value;
    const hasElectric = isElectricFuel(fuel);
    const hasCombustion = isCombustionFuel(fuel);
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
                getValueByPaths(config.value, ['brand.chnName', 'brandChnName', 'brand.name', 'brandName']) ||
                getValueByPaths(vehicle.value, ['model.brand.chnName', 'model.brandChnName', 'model.brand.name', 'brandName'])
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
    abortModalRequests();
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
    width: min(95%, 1340px);
    background: rgba(255, 255, 255, 0.98);
    border-radius: 28px;
    padding: 24px;
    box-shadow: 0 30px 60px rgba(15, 23, 42, 0.25);
    display: flex;
    flex-direction: column;
    max-height: min(900px, calc(100vh - 48px));
    overflow: hidden;
    position: relative;
}

.modal__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 12px;
    margin-bottom: 16px;
    position: relative;
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

.btn-icon {
    line-height: 1;
}

.btn-text {
    white-space: nowrap;
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
    flex-wrap: nowrap;
    min-width: 0;
}

.title-row h2 {
    margin: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
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
    flex-shrink: 0;
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
    min-height: 0;
    display: flex;
    flex-direction: column;
}

.modal__content--with-comments {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: 0;
}

.modal__body {
    display: flex;
    flex-direction: column;
    gap: 24px;
    padding-bottom: 8px;
    min-height: 0;
}

.modal__content--with-comments .modal__body {
    overflow-y: auto;
    padding-right: 18px;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}

.comment-drawer {
    min-height: 0;
    display: flex;
    flex-direction: column;
    border-left: 1px solid #d1d5db;
    padding-left: 16px;
    margin-left: 2px;
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

.comment-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 6px;
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

.mobile-comments {
    border-top: 1px solid #e2e8f0;
    padding-top: 10px;
}

.mobile-comments__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.mobile-comments__head h3 {
    margin: 0;
}

.mobile-comments__count {
    color: #64748b;
    font-size: 0.84rem;
}

.mobile-comments__body {
    margin-top: 8px;
    max-height: min(44vh, 360px);
    overflow-y: auto;
    padding-right: 4px;
}

.mobile-comments__footer {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 8px;
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
    width: 40px;
    height: 40px;
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
    left: 20px;
}

.variant-nav--next {
    right: 20px;
}

.trade-bridge-card {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 14px 16px;
    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.trade-bridge-card--hide-teams :deep(.group-buy-bridge__teams) {
    display: none;
}

.trade-team-times {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 10px 12px;
    background: #f8fafc;
}

.trade-team-times__title {
    margin: 0;
    color: #0f172a;
    font-size: 13px;
    font-weight: 600;
}

.trade-team-times__list {
    margin-top: 8px;
    display: grid;
    gap: 8px;
}

.trade-team-times__item {
    border: 1px dashed #cbd5e1;
    border-radius: 10px;
    padding: 8px 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.trade-team-times__meta {
    display: grid;
    gap: 2px;
    font-size: 12px;
    color: #334155;
}

.trade-team-times__countdown {
    color: #0f766e;
    font-weight: 600;
}

.trade-team-times__join {
    border: 1px solid #93c5fd;
    background: #eff6ff;
    color: #1d4ed8;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
    padding: 6px 10px;
    cursor: pointer;
}

.direct-checkout-panel {
    display: grid;
    gap: 12px;
}

.direct-checkout-panel p {
    margin: 0;
    color: #334155;
}

.direct-payment-options {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
}

.direct-payment-option {
    border: 1px solid #d1d5db;
    border-radius: 10px;
    padding: 8px;
    background: #fff;
    text-align: left;
    cursor: pointer;
    display: grid;
    gap: 2px;
}

.direct-payment-option span {
    font-weight: 600;
    color: #111827;
}

.direct-payment-option small {
    color: #6b7280;
}

.direct-payment-option.active {
    border-color: #1d4ed8;
    background: #eff6ff;
}

.direct-payment-option.disabled {
    opacity: 0.6;
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

@media (max-width: 768px) {
    .modal {
        width: 100%;
        max-height: calc(100vh - 20px);
        border-radius: 18px;
        padding: 16px;
        overflow-y: auto;
    }

    .modal__headline {
        padding-right: 48px;
        min-width: 0;
    }

    .header-actions {
        gap: 6px;
    }

    .favorite-btn {
        width: 36px;
        height: 36px;
        padding: 0;
        border-radius: 50%;
        display: inline-flex;
        align-items: center;
        justify-content: center;
    }

    .favorite-btn .btn-text {
        display: none;
    }

    .modal__content,
    .modal__content--with-comments {
        display: flex;
    }

    .modal__body {
        overflow: visible;
        padding-right: 0;
    }

    .direct-payment-options {
        grid-template-columns: 1fr;
    }

    .close-btn--modal {
        position: absolute;
        top: 0;
        right: 0;
        z-index: 2;
    }
}

.route-list { display: flex; flex-direction: column; gap: 8px; }
.route-item {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 10px 14px;
    &--history { opacity: 0.7; background: #f8fafc; }
}
.route-item__head {
    display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
    strong { color: #0f172a; }
}
.route-link { color: #2563eb; font-weight: 700; text-decoration: none; font-size: 15px;
    &:hover { text-decoration: underline; }
}
.route-item__stops { color: #475569; font-size: 13px; margin-top: 4px; }

@media (max-width: 430px) {
    .favorite-btn .favorite-count {
        display: none;
    }
}
</style>
