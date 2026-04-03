<template>
    <div class="group-market-page">
        <section class="hero-card">
            <div class="hero-main">
                <p class="eyebrow">Group Buy</p>
                <h1>拼团大厅</h1>
                <p class="desc">
                    默认先展示正在拼团的信息和倒计时，确认后再进入结算支付。
                </p>
            </div>
        </section>

        <section class="card">
            <header class="section-head">
                <h2>正在拼团</h2>
                <el-button text :loading="loadingTeams" @click="loadActiveTeams">刷新</el-button>
            </header>
            <div v-if="loadingTeams && !activeTeamRows.length" class="state">正在加载拼团列表...</div>
            <div v-else-if="!activeTeamRows.length" class="state">
                {{ contextReady ? '当前没有进行中的拼团，请稍后刷新。' : '当前未绑定商品信息，可在“我的拼团进度”中点击继续拼团，或从图片详情进入。' }}
            </div>
            <div v-else class="team-list">
                <article v-for="team in activeTeamRows" :key="team.teamId" class="team-row">
                    <div>
                        <p class="team-title">团号 {{ team.teamId }}</p>
                        <p class="team-meta">进度 {{ team.completeCount }}/{{ team.targetCount || '-' }} 人</p>
                    </div>
                    <div class="team-actions">
                        <span class="countdown">{{ formatCountdown(team.validEndTime) }}</span>
                        <el-button text size="small" @click="openGoodsDetail(team)">商品详情</el-button>
                        <el-button size="small" :disabled="isTeamOrderBlocked(team)" @click="openTeamCheckout(team)">
                            {{ resolveTeamActionText(team) }}
                        </el-button>
                    </div>
                </article>
            </div>
        </section>

        <section class="card">
            <header class="section-head">
                <h2>我的拼团进度</h2>
                <el-button text :loading="loadingMyGroups" @click="loadMyPendingGroups">刷新</el-button>
            </header>
            <div v-if="loadingMyGroups" class="state">正在加载我的拼团记录...</div>
            <div v-else-if="!myPendingGroups.length" class="state">
                暂无可继续的拼团记录。你也可以从图片详情页进入本页面并参与拼团。
            </div>
            <div v-else class="my-group-list">
                <article v-for="record in myPendingGroups" :key="record.recordId" class="my-group-row">
                    <div>
                        <p class="team-title">{{ record.title || '图片商品' }}</p>
                        <p class="team-meta">
                            团号 {{ record.teamId }} · 当前 {{ record.completeCount || 0 }}/{{ record.targetCount || '-' }} 人 · {{ centsToYuan(record.payPriceCents) }} · {{ resolvePendingStatusText(record) }}
                        </p>
                    </div>
                    <div class="team-actions">
                        <span class="countdown">{{ formatCountdown(record.validEndTime) }}</span>
                        <el-button v-if="canContinueGroupRecord(record)" size="small" @click="handleRecordPrimaryAction(record)">
                            {{ resolveRecordPrimaryActionText(record) }}
                        </el-button>
                        <el-button
                            size="small"
                            type="danger"
                            plain
                            :loading="cancelingRecordIds.has(String(record.recordId || record.outTradeNo || ''))"
                            :disabled="!canCancelGroupRecord(record)"
                            @click="cancelGroupRecord(record)"
                        >
                            取消拼团
                        </el-button>
                    </div>
                </article>
            </div>
        </section>

        <el-dialog
            v-model="checkoutVisible"
            title="订单结算确认"
            width="540px"
            destroy-on-close
        >
            <div class="checkout-panel">
                <p>购买方式：<strong>{{ checkoutAction === 'direct-buy' ? '直接下单' : '拼团购买' }}</strong></p>
                <p>支付金额：<strong>{{ centsToYuan(expectedPayCents) }}</strong></p>
                <p>当前余额：<strong>{{ centsToYuan(balanceCents) }}</strong></p>

                <div class="payment-options">
                    <button
                        v-for="option in paymentOptions"
                        :key="option.value"
                        type="button"
                        class="payment-option"
                        :class="{ active: paymentMethod === option.value, disabled: !option.available }"
                        @click="selectPaymentMethod(option)"
                    >
                        <span>{{ option.label }}</span>
                        <small>{{ option.desc }}</small>
                    </button>
                </div>

                <el-alert
                    v-if="paymentMethod !== 'BALANCE'"
                    type="warning"
                    show-icon
                    :closable="false"
                    title="支付宝/微信暂未接入，请使用余额支付"
                />
                <el-alert
                    v-else-if="!hasSufficientBalance"
                    type="warning"
                    show-icon
                    :closable="false"
                    title="余额不足，请充值后再试"
                />

                <el-input
                    v-model="payPassword"
                    type="password"
                    show-password
                    placeholder="请输入登录密码进行支付验证"
                    maxlength="64"
                />
            </div>
            <template #footer>
                <el-button @click="checkoutVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" :disabled="!readyToSubmit" @click="submitCheckout">
                    确认支付
                </el-button>
            </template>
        </el-dialog>

        <section v-if="checkoutResult" class="card result-card">
            <h2>最近一次下单结果</h2>
            <p><strong>订单号：</strong>{{ checkoutResult.orderId || '-' }}</p>
            <p><strong>团队状态：</strong>{{ checkoutResult.teamStatus || '-' }}</p>
            <p><strong>结果：</strong>{{ checkoutResult.message || '-' }}</p>
        </section>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import {
    directBuyCheckout,
    fetchPortalActiveTeams,
    fetchPortalRecords,
    fetchTradeBridgeConfig,
    refundTradeBridgeOrder,
    groupBuyCheckout,
    resolveTradeBindingByImage
} from '@/api/tradeBridge';
import { verifyPassword } from '@/api/auth';

const route = useRoute();
const router = useRouter();
const store = useStore();
const MARKET_CONTEXT_CACHE_KEY_PREFIX = 'busGallery:group-market-context';
const ACTIVE_TEAM_LIMIT = 100;

const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);
const profile = computed(() => store.state.auth.profile || null);
const currentUserId = computed(() => String(profile.value?.id || 'guest'));
const balanceCents = computed(() => Number(profile.value?.balanceCents || 0));

const pageContext = reactive({
    vehicleId: '',
    imageId: '',
    plateNumber: ''
});

const form = reactive({
    goodsId: '',
    activityId: '',
    source: 'content',
    channel: 'balance'
});

const marketConfig = ref(null);
const activeTeams = ref([]);
const myPendingGroups = ref([]);
const loadingBinding = ref(false);
const loadingTeams = ref(false);
const loadingMyGroups = ref(false);
const submitting = ref(false);
const checkoutVisible = ref(false);
const checkoutAction = ref('group-buy');
const selectedTeamId = ref('');
const payPassword = ref('');
const paymentMethod = ref('BALANCE');
const checkoutResult = ref(null);
const lastAutoCheckoutKey = ref('');
const cancelingRecordIds = ref(new Set());
const userScopeVersion = ref(0);

const nowTimestamp = ref(Date.now());
let timer = null;

const paymentOptions = [
    { value: 'ALIPAY', label: '支付宝', desc: '扫码支付', available: false },
    { value: 'WECHAT', label: '微信支付', desc: '扫码支付', available: false },
    { value: 'BALANCE', label: '余额支付', desc: '即时扣款', available: true }
];

const contextReady = computed(() => Boolean(form.goodsId && form.activityId));
const expectedPayCents = computed(() => {
    if (!marketConfig.value) return 0;
    if (checkoutAction.value === 'direct-buy') {
        return Number(marketConfig.value.originalPriceCents || 0);
    }
    return Number(marketConfig.value.groupPriceCents || 0);
});

const hasSufficientBalance = computed(() => balanceCents.value >= expectedPayCents.value);

const readyToSubmit = computed(() => {
    if (!contextReady.value) return false;
    if (paymentMethod.value !== 'BALANCE') return false;
    if (!hasSufficientBalance.value) return false;
    if (!payPassword.value.trim()) return false;
    if (submitting.value) return false;
    return true;
});

const centsToYuan = (cents) => `${(Number(cents || 0) / 100).toFixed(2)} 元`;

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

const isTrueLike = (value) => value === true || value === 1 || value === '1' || value === 'true';

const isGroupRecordWaiting = (record) =>
    Number(record?.orderMode || 0) !== 2 &&
    Number(record?.tradeStatus || 0) === 0 &&
    !isTrueLike(record?.canDownload);

const canContinueGroupRecord = (record) =>
    Number(record?.orderMode || 0) !== 2 &&
    !isTrueLike(record?.canDownload) &&
    [0, 2].includes(Number(record?.tradeStatus || 0)) &&
    Boolean(record?.goodsId || record?.imageId);

const isPendingTradeRecord = (record) =>
    Boolean(
        record &&
        (
            record?.recordId ||
            record?.outTradeNo ||
            record?.tradeStatus !== undefined ||
            record?.orderMode !== undefined
        )
    );

const findSelfWaitingRecord = (goodsId = '', activityId = '') => {
    const targetGoodsId = String(goodsId || '');
    const targetActivityId = String(activityId || '');
    return myPendingGroups.value.find(
        (record) => {
            if (!isGroupRecordWaiting(record)) return false;
            const recordGoodsId = String(record?.goodsId || '');
            const recordActivityId = String(record?.activityId || '');
            if (targetGoodsId && recordGoodsId !== targetGoodsId) return false;
            if (targetActivityId && recordActivityId !== targetActivityId) return false;
            return Boolean(targetGoodsId || targetActivityId);
        }
    ) || null;
};

const hasSelfQueueForTarget = (goodsId = '', activityId = '') =>
    Boolean(findSelfWaitingRecord(goodsId, activityId));

const isTeamStillActive = (team) => {
    const targetCount = Number(team?.targetCount || 0);
    const completeCount = Number(team?.completeCount || 0);
    if (targetCount > 0 && completeCount >= targetCount) {
        return false;
    }
    const endMs = parseDateMs(team?.validEndTime);
    if (endMs > 0 && endMs <= nowTimestamp.value) {
        return false;
    }
    return true;
};

const activeTeamRows = computed(() => {
    const teamMap = new Map();
    const upsertTeam = (raw, pendingRecord = null) => {
        if (!isTeamStillActive(raw)) return;
        const teamId = String(raw?.teamId || '');
        if (!teamId) return;
        const exists = teamMap.get(teamId) || null;
        teamMap.set(teamId, {
            teamId,
            targetCount: Number(raw?.targetCount || exists?.targetCount || 0),
            completeCount: Number(raw?.completeCount || exists?.completeCount || 0),
            validEndTime: raw?.validEndTime || exists?.validEndTime || null,
            title: raw?.title || exists?.title || '',
            goodsId: String(raw?.goodsId || exists?.goodsId || form.goodsId || ''),
            activityId: String(raw?.activityId || exists?.activityId || form.activityId || ''),
            imageId: String(raw?.imageId || exists?.imageId || pageContext.imageId || ''),
            vehicleId: String(raw?.vehicleId || exists?.vehicleId || pageContext.vehicleId || ''),
            pendingRecord: pendingRecord || exists?.pendingRecord || null
        });
    };

    activeTeams.value.forEach((team) => upsertTeam(team));

    myPendingGroups.value
        .filter((record) => isGroupRecordWaiting(record))
        .forEach((record) => {
            const endMs = parseDateMs(record?.validEndTime);
            if (endMs > 0 && endMs <= nowTimestamp.value) {
                return;
            }
            upsertTeam(record, record);
        });

    return Array.from(teamMap.values());
});

const formatCountdown = (endTime) => {
    const endMs = parseDateMs(endTime);
    if (!endMs) return '倒计时 --:--';
    const diff = Math.floor((endMs - nowTimestamp.value) / 1000);
    if (diff <= 0) return '已结束';
    const hours = Math.floor(diff / 3600);
    const minutes = Math.floor((diff % 3600) / 60);
    const seconds = diff % 60;
    if (hours > 0) {
        return `剩余 ${hours}h ${String(minutes).padStart(2, '0')}m`;
    }
    return `剩余 ${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
};

const bumpUserScopeVersion = () => {
    userScopeVersion.value += 1;
    return userScopeVersion.value;
};

const isSameUserScope = (scopeUserId, scopeVersion) =>
    scopeUserId === currentUserId.value && scopeVersion === userScopeVersion.value;

const resolveMarketContextCacheKey = () => `${MARKET_CONTEXT_CACHE_KEY_PREFIX}:${currentUserId.value}`;

const resetUserScopedState = () => {
    bumpUserScopeVersion();
    myPendingGroups.value = [];
    activeTeams.value = [];
    loadingMyGroups.value = false;
    loadingTeams.value = false;
    selectedTeamId.value = '';
    checkoutVisible.value = false;
    checkoutResult.value = null;
    lastAutoCheckoutKey.value = '';
    cancelingRecordIds.value = new Set();
};

const loadCachedMarketContext = () => {
    if (typeof window === 'undefined') return null;
    try {
        const raw = window.localStorage.getItem(resolveMarketContextCacheKey());
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        return parsed && typeof parsed === 'object' ? parsed : null;
    } catch (error) {
        return null;
    }
};

const saveCachedMarketContext = () => {
    if (typeof window === 'undefined') return;
    if (!form.goodsId && !form.activityId) return;
    const payload = {
        goodsId: String(form.goodsId || ''),
        activityId: String(form.activityId || ''),
        source: String(form.source || 'content'),
        imageId: String(pageContext.imageId || ''),
        vehicleId: String(pageContext.vehicleId || '')
    };
    window.localStorage.setItem(resolveMarketContextCacheKey(), JSON.stringify(payload));
};

const syncFromRoute = () => {
    const cached = loadCachedMarketContext();
    pageContext.vehicleId = String(route.query.vehicleId || cached?.vehicleId || '');
    pageContext.imageId = String(route.query.imageId || cached?.imageId || '');
    pageContext.plateNumber = String(route.query.plateNumber || '');

    const routeGoodsId = String(route.query.goodsId || '');
    const routeActivityId = String(route.query.activityId || '');
    const routeHasContext = Boolean(routeGoodsId || routeActivityId);

    form.goodsId = routeHasContext ? routeGoodsId : String(cached?.goodsId || '');
    form.activityId = routeHasContext ? routeActivityId : String(cached?.activityId || '');
    form.source = String(route.query.source || cached?.source || 'content');
    form.channel = 'balance';

    selectedTeamId.value = String(route.query.teamId || '');
    if (routeHasContext) {
        saveCachedMarketContext();
    }
};

const ensureProfile = async () => {
    if (!isAuthenticated.value) return;
    if (!profile.value) {
        await store.dispatch('auth/fetchProfile').catch(() => {});
    }
};

const loadBindingAndConfig = async () => {
    if (!pageContext.imageId && !form.goodsId) {
        marketConfig.value = null;
        return;
    }
    loadingBinding.value = true;
    try {
        if ((!form.goodsId || !form.activityId) && pageContext.imageId) {
            const binding = await resolveTradeBindingByImage(
                Number(pageContext.imageId),
                pageContext.vehicleId ? { vehicleId: Number(pageContext.vehicleId) } : {}
            );
            if (binding?.goodsId) form.goodsId = String(binding.goodsId);
            if (binding?.activityId) form.activityId = String(binding.activityId);
            if (binding) {
                marketConfig.value = {
                    goodsId: binding.goodsId,
                    activityId: binding.activityId,
                    goodsTitle: binding.goodsTitle,
                    originalPriceCents: binding.originalPriceCents,
                    groupPriceCents: binding.groupPriceCents,
                    targetCount: binding.targetCount,
                    validMinutes: binding.validMinutes,
                    activeTeamCount: binding.activeTeamCount
                };
                saveCachedMarketContext();
            }
        }

        if (form.goodsId) {
            const config = await fetchTradeBridgeConfig({ goodsId: form.goodsId });
            if (config) {
                marketConfig.value = config;
                if (config.activityId) {
                    form.activityId = String(config.activityId);
                }
                saveCachedMarketContext();
            }
        }
    } catch (error) {
        ElMessage.error(error?.message || '加载交易配置失败');
    } finally {
        loadingBinding.value = false;
    }
};

const loadActiveTeams = async () => {
    const scopeUserId = currentUserId.value;
    const scopeVersion = userScopeVersion.value;
    loadingTeams.value = true;
    try {
        const list = await fetchPortalActiveTeams(null, ACTIVE_TEAM_LIMIT);
        if (!isSameUserScope(scopeUserId, scopeVersion)) {
            return;
        }
        activeTeams.value = Array.isArray(list) ? list : [];
    } catch (error) {
        if (!isSameUserScope(scopeUserId, scopeVersion)) {
            return;
        }
        activeTeams.value = [];
    } finally {
        if (isSameUserScope(scopeUserId, scopeVersion)) {
            loadingTeams.value = false;
        }
    }
};

const loadMyPendingGroups = async () => {
    const scopeUserId = currentUserId.value;
    const scopeVersion = userScopeVersion.value;
    if (!isAuthenticated.value) {
        myPendingGroups.value = [];
        return;
    }
    loadingMyGroups.value = true;
    try {
        const records = await fetchPortalRecords(200);
        if (!isSameUserScope(scopeUserId, scopeVersion)) {
            return;
        }
        const list = Array.isArray(records) ? records : [];
        const pending = list.filter((item) => canContinueGroupRecord(item));

        const configCache = new Map();
        const activityTeamsCache = new Map();
        const ensureConfig = async (goodsId) => {
            const key = String(goodsId || '');
            if (!key) return null;
            if (!configCache.has(key)) {
                const confPromise = fetchTradeBridgeConfig({ goodsId: key }).catch(() => null);
                configCache.set(key, confPromise);
            }
            return configCache.get(key);
        };
        const ensureActivityTeams = async (activityId) => {
            const key = String(activityId || '');
            if (!key) return [];
            if (!activityTeamsCache.has(key)) {
                const teamPromise = fetchPortalActiveTeams(Number(activityId), ACTIVE_TEAM_LIMIT)
                    .then((list) => (Array.isArray(list) ? list : []))
                    .catch(() => []);
                activityTeamsCache.set(key, teamPromise);
            }
            return activityTeamsCache.get(key);
        };

        const rows = await Promise.all(
            pending.map(async (item) => {
                const config = await ensureConfig(item.goodsId);
                let validEndTime = null;
                let completeCount = Number(item?.completeCount || 0);
                let targetCount = Number(item?.targetCount || config?.targetCount || marketConfig.value?.targetCount || 0);
                const activityId = Number(item?.activityId || config?.activityId || 0);
                if (item?.teamId && activityId > 0) {
                    const teams = await ensureActivityTeams(activityId);
                    const hit = teams.find((team) => String(team?.teamId || '') === String(item.teamId || ''));
                    if (hit?.validEndTime) {
                        validEndTime = hit.validEndTime;
                    }
                    if (hit) {
                        completeCount = Number(hit.completeCount || completeCount || 0);
                        targetCount = Number(hit.targetCount || targetCount || 0);
                    }
                }
                if (!validEndTime) {
                    const validMinutes = Number(config?.validMinutes || marketConfig.value?.validMinutes || 0);
                    const createdAtMs = parseDateMs(item.createdAt);
                    validEndTime = validMinutes > 0 && createdAtMs > 0
                        ? new Date(createdAtMs + validMinutes * 60 * 1000).toISOString()
                        : null;
                }
                return {
                    ...item,
                    goodsId: String(item?.goodsId || ''),
                    activityId: activityId > 0 ? String(activityId) : '',
                    validEndTime,
                    completeCount,
                    targetCount
                };
            })
        );

        if (!isSameUserScope(scopeUserId, scopeVersion)) {
            return;
        }
        myPendingGroups.value = rows;

        const selectedRecord = selectedTeamId.value
            ? rows.find((item) => String(item?.teamId || '') === String(selectedTeamId.value || ''))
            : null;
        const preferred = selectedRecord || rows.find((item) => item?.goodsId) || null;
        const shouldAdoptPreferredContext = Boolean(
            preferred?.goodsId &&
            (
                !contextReady.value ||
                (
                    selectedRecord &&
                    String(form.goodsId || '') !== String(selectedRecord.goodsId || '')
                )
            )
        );
        if (shouldAdoptPreferredContext) {
            form.goodsId = String(preferred.goodsId);
            if (!pageContext.imageId && preferred.imageId) {
                pageContext.imageId = String(preferred.imageId);
            }
            if (!pageContext.vehicleId && preferred.vehicleId) {
                pageContext.vehicleId = String(preferred.vehicleId);
            }
            if (preferred.teamId) {
                selectedTeamId.value = String(preferred.teamId);
            }
            const config = await ensureConfig(preferred.goodsId);
            if (!isSameUserScope(scopeUserId, scopeVersion)) {
                return;
            }
            if (config) {
                marketConfig.value = config;
                if (config.activityId) {
                    form.activityId = String(config.activityId);
                }
                saveCachedMarketContext();
            }
        }
    } catch (error) {
        if (!isSameUserScope(scopeUserId, scopeVersion)) {
            return;
        }
        myPendingGroups.value = [];
    } finally {
        if (isSameUserScope(scopeUserId, scopeVersion)) {
            loadingMyGroups.value = false;
        }
    }
};

const selectPaymentMethod = (option) => {
    if (!option.available) {
        ElMessage.info(`${option.label}暂未接入，请使用余额支付`);
        return;
    }
    paymentMethod.value = option.value;
};

const resolvePendingStatusText = (record) => {
    const status = Number(record?.tradeStatus || 0);
    if (status === 2) return '拼团失败（可重新拼团）';
    if (status === 1) return '拼团成功';
    return '拼团进行中';
};

const isTeamOrderBlocked = (team) => {
    const teamRecord = team?.pendingRecord || null;
    if (teamRecord && isGroupRecordWaiting(teamRecord)) {
        return true;
    }
    return false;
};

const resolveTeamActionText = (team) => (isTeamOrderBlocked(team) ? '已在队列' : '加入拼团');

const resolveRecordPrimaryActionText = (record) =>
    Number(record?.tradeStatus || 0) === 2 ? '重新拼团' : '查看队伍';

const openCheckout = async (mode, teamId = '', record = null) => {
    if (!isAuthenticated.value) {
        router.push({ name: 'Login', query: { redirect: route.fullPath } });
        return;
    }

    const recordGoodsId = String(record?.goodsId || '');
    const recordActivityId = String(record?.activityId || '');
    const shouldSyncRecordContext = Boolean(
        recordGoodsId &&
        (
            !form.goodsId ||
            !form.activityId ||
            String(form.goodsId || '') !== recordGoodsId ||
            (recordActivityId && String(form.activityId || '') !== recordActivityId)
        )
    );
    if (shouldSyncRecordContext) {
        form.goodsId = recordGoodsId;
        if (recordActivityId) form.activityId = recordActivityId;
        if (record?.imageId) pageContext.imageId = String(record.imageId);
        if (record?.vehicleId) pageContext.vehicleId = String(record.vehicleId);
        saveCachedMarketContext();
        await loadBindingAndConfig();
    }
    if (!recordGoodsId && record?.imageId && !contextReady.value) {
        pageContext.imageId = String(record.imageId);
        if (record?.vehicleId) {
            pageContext.vehicleId = String(record.vehicleId);
        }
        await loadBindingAndConfig();
    }

    if (!contextReady.value) {
        ElMessage.warning('当前未绑定交易信息，请从图片详情进入后重试');
        return;
    }

    checkoutAction.value = mode === 'direct-buy' ? 'direct-buy' : 'group-buy';
    selectedTeamId.value = teamId ? String(teamId) : '';
    paymentMethod.value = 'BALANCE';
    payPassword.value = '';
    checkoutVisible.value = true;
};

const openTeamCheckout = async (team) => {
    const teamRecord = team?.pendingRecord || team || null;
    await continueGroup(teamRecord);
};

const consumeAutoCheckoutQuery = () => {
    const nextQuery = { ...route.query };
    delete nextQuery.action;
    delete nextQuery.autoCheckout;
    router.replace({ query: nextQuery }).catch(() => {});
};

const resolveGoodsImageId = (team = null) => {
    if (team?.imageId) return String(team.imageId);
    if (pageContext.imageId) return String(pageContext.imageId);
    const fromPending = myPendingGroups.value.find((item) => item?.imageId);
    return fromPending?.imageId ? String(fromPending.imageId) : '';
};

const openGoodsDetail = (team = null) => {
    const teamRecord = team?.pendingRecord || team || null;
    const targetGoodsId = String(teamRecord?.goodsId || form.goodsId || '');
    const targetActivityId = String(teamRecord?.activityId || form.activityId || '');
    if (!targetGoodsId && !targetActivityId && !contextReady.value) {
        ElMessage.warning('当前无可查看的商品信息');
        return;
    }
    if (targetGoodsId) {
        form.goodsId = targetGoodsId;
    }
    if (targetActivityId) {
        form.activityId = targetActivityId;
    }
    if (teamRecord?.imageId) {
        pageContext.imageId = String(teamRecord.imageId);
    }
    if (teamRecord?.vehicleId) {
        pageContext.vehicleId = String(teamRecord.vehicleId);
    }
    saveCachedMarketContext();
    const imageId = resolveGoodsImageId(teamRecord);
    router.push({
        name: 'GroupGoodsDetail',
        query: {
            goodsId: targetGoodsId || undefined,
            activityId: targetActivityId || undefined,
            imageId: imageId || undefined,
            vehicleId: pageContext.vehicleId || undefined
        }
    });
};

const resolveContinueTeamId = async (record) => {
    const preferredTeamId = String(record?.teamId || '');
    const activityId = Number(record?.activityId || form.activityId || 0);
    if (activityId <= 0) {
        if (Number(record?.tradeStatus || 0) === 2 && preferredTeamId) {
            ElMessage.info('原拼团队伍已结束，将发起新的拼团');
            return '';
        }
        return preferredTeamId;
    }
    let teams = [];
    try {
        const list = await fetchPortalActiveTeams(activityId, ACTIVE_TEAM_LIMIT);
        teams = Array.isArray(list) ? list : [];
    } catch (error) {
        teams = [];
    }
    if (!teams.length) {
        if (preferredTeamId) {
            ElMessage.info('原拼团队伍已结束，将发起新的拼团');
        }
        return '';
    }
    if (preferredTeamId) {
        const current = teams.find((team) => String(team?.teamId || '') === preferredTeamId);
        if (current) return preferredTeamId;
        ElMessage.info('原拼团队伍已不可加入，已切换到同活动其他进行中队伍');
    }
    return String(teams[0]?.teamId || '');
};

const continueGroup = async (record) => {
    if (!record) {
        await openCheckout('group-buy');
        return;
    }
    if (isPendingTradeRecord(record) && isGroupRecordWaiting(record)) {
        ElMessage.info('你已在该商品拼团队列中，无需重复下单');
        openGoodsDetail(record);
        return;
    }
    if (isPendingTradeRecord(record) && !canContinueGroupRecord(record)) {
        ElMessage.warning('当前记录不可继续拼团');
        return;
    }
    const nextTeamId = await resolveContinueTeamId(record);
    await openCheckout('group-buy', nextTeamId, record);
};

const handleRecordPrimaryAction = async (record) => {
    if (isPendingTradeRecord(record) && isGroupRecordWaiting(record)) {
        ElMessage.info('你已在该商品拼团队列中，无需重复下单');
        openGoodsDetail(record);
        return;
    }
    await continueGroup(record);
};

const canCancelGroupRecord = (record) =>
    Boolean(record?.outTradeNo) &&
    isGroupRecordWaiting(record);

const cancelGroupRecord = async (record) => {
    if (!canCancelGroupRecord(record)) {
        ElMessage.warning('当前记录不可取消');
        return;
    }
    const key = String(record.recordId || record.outTradeNo || '');
    if (cancelingRecordIds.value.has(key)) return;
    cancelingRecordIds.value.add(key);
    try {
        await refundTradeBridgeOrder({
            outTradeNo: String(record.outTradeNo),
            source: 'portal',
            channel: 'web'
        });
        ElMessage.success('取消拼团成功，资金已按规则退回');
        await store.dispatch('auth/fetchProfile').catch(() => {});
        await Promise.all([loadMyPendingGroups(), loadActiveTeams()]);
    } catch (error) {
        ElMessage.error(error?.message || '取消拼团失败');
    } finally {
        const next = new Set(cancelingRecordIds.value);
        next.delete(key);
        cancelingRecordIds.value = next;
    }
};

const tryAutoOpenCheckoutFromRoute = async () => {
    const action = String(route.query.action || '').toLowerCase();
    const shouldAuto = String(route.query.autoCheckout || '').toLowerCase();
    if (action !== 'direct-buy' && action !== 'group-buy') {
        return;
    }
    if (shouldAuto && shouldAuto !== '1' && shouldAuto !== 'true') {
        return;
    }
    if (!contextReady.value) {
        return;
    }
    const key = `${action}|${route.fullPath}|${form.goodsId}|${form.activityId}|${selectedTeamId.value || ''}`;
    if (lastAutoCheckoutKey.value === key) {
        return;
    }
    lastAutoCheckoutKey.value = key;
    await openCheckout(action, selectedTeamId.value || '');
    consumeAutoCheckoutQuery();
};

const submitCheckout = async () => {
    if (!readyToSubmit.value) {
        ElMessage.warning('请先完成支付方式和密码校验');
        return;
    }

    submitting.value = true;
    try {
        await verifyPassword({ currentPassword: payPassword.value.trim() });

        const payload = {
            goodsId: form.goodsId,
            activityId: Number(form.activityId),
            teamId: checkoutAction.value === 'group-buy' ? (selectedTeamId.value || null) : null,
            source: form.source,
            channel: 'balance'
        };

        const result = checkoutAction.value === 'direct-buy'
            ? await directBuyCheckout(payload)
            : await groupBuyCheckout(payload);

        checkoutResult.value = result;
        checkoutVisible.value = false;
        payPassword.value = '';

        await store.dispatch('auth/fetchProfile').catch(() => {});
        await Promise.all([loadActiveTeams(), loadMyPendingGroups()]);

        if (result?.canDownload && result?.recordId) {
            ElMessage.success('支付成功，正在跳转下载页');
            router.replace({ name: 'TradeDownload', query: { recordId: result.recordId } });
            return;
        }
        ElMessage.success(result?.message || '支付成功');
    } catch (error) {
        ElMessage.error(error?.message || '支付失败');
    } finally {
        submitting.value = false;
    }
};

watch(
    () => route.query,
    async () => {
        await ensureProfile();
        syncFromRoute();
        await loadBindingAndConfig();
        await loadMyPendingGroups();
        await loadActiveTeams();
        await tryAutoOpenCheckoutFromRoute();
    },
    { immediate: true }
);

watch(
    () => isAuthenticated.value,
    async (val) => {
        if (!val) {
            resetUserScopedState();
            return;
        }
        await ensureProfile();
        await loadMyPendingGroups();
        await loadActiveTeams();
        await tryAutoOpenCheckoutFromRoute();
    }
);

watch(
    () => profile.value?.id,
    async (nextId, prevId) => {
        const nextUser = String(nextId || '');
        const prevUser = String(prevId || '');
        if (!isAuthenticated.value || !nextUser || nextUser === prevUser) {
            return;
        }
        resetUserScopedState();
        syncFromRoute();
        await loadBindingAndConfig();
        await loadMyPendingGroups();
        await loadActiveTeams();
        await tryAutoOpenCheckoutFromRoute();
    }
);

timer = setInterval(() => {
    nowTimestamp.value = Date.now();
}, 1000);

onBeforeUnmount(() => {
    if (timer) {
        clearInterval(timer);
        timer = null;
    }
});
</script>

<style scoped lang="scss">
.group-market-page {
    min-height: calc(100vh - 70px);
    padding: 24px 12px 48px;
    background: #f1f5f9;
    display: grid;
    gap: 4px;
}

.card {
    width: min(980px, 100%);
    margin: 0 auto;
    border-radius: 16px;
    background: #fff;
    border: 1px solid #e2e8f0;
    box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
    padding: 16px;
}

.hero-card {
    width: min(980px, 100%);
    margin: 0 auto;
    padding: 0 2px 1px;
    background: transparent;
    border: 0;
    box-shadow: none;
}

.hero-main {
    min-height: 0;
    display: grid;
    gap: 0;
}

.hero-main h1 {
    margin: 0;
    color: #0f172a;
}

.eyebrow {
    margin: 0;
    font-size: 12px;
    letter-spacing: 0.06em;
    color: #64748b;
}

.desc {
    margin: 2px 0 0;
    color: #475569;
    font-size: 14px;
    line-height: 1.35;
}

.section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 10px;
}

.section-head h2 {
    margin: 0;
    font-size: 18px;
    color: #0f172a;
}

.state {
    color: #64748b;
    font-size: 14px;
    padding: 8px 2px;
}

.team-list,
.my-group-list {
    display: grid;
    gap: 10px;
}

.team-row,
.my-group-row {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.team-title {
    margin: 0;
    color: #0f172a;
    font-weight: 600;
}

.team-meta {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 13px;
}

.team-actions {
    display: flex;
    align-items: center;
    gap: 10px;
}

.countdown {
    color: #0f766e;
    font-weight: 600;
    font-size: 13px;
}

.checkout-panel {
    display: grid;
    gap: 12px;
}

.checkout-panel p {
    margin: 0;
    color: #334155;
}

.payment-options {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
}

.payment-option {
    border: 1px solid #d1d5db;
    border-radius: 10px;
    padding: 8px;
    background: #fff;
    text-align: left;
    cursor: pointer;
    display: grid;
    gap: 2px;
}

.payment-option span {
    font-weight: 600;
    color: #111827;
}

.payment-option small {
    color: #6b7280;
}

.payment-option.active {
    border-color: #1d4ed8;
    background: #eff6ff;
}

.payment-option.disabled {
    opacity: 0.6;
}

.result-card h2 {
    margin: 0 0 10px;
    font-size: 18px;
}

.result-card p {
    margin: 0 0 6px;
    color: #334155;
}

@media (max-width: 768px) {
    .payment-options {
        grid-template-columns: 1fr;
    }

    .team-row,
    .my-group-row {
        flex-direction: column;
        align-items: flex-start;
    }

    .team-actions {
        width: 100%;
        justify-content: space-between;
    }
}
</style>

