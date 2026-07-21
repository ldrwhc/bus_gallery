<template>
    <div class="user-profile-page">
        <section v-if="profile" class="card profile-card">
            <div class="profile-top">
                <div class="avatar">{{ avatarInitial }}</div>
                <div class="profile-info">
                    <div class="display-name-row">
                        <template v-if="isSelf && editingDisplayName">
                            <el-input
                                v-model.trim="displayNameDraft"
                                class="display-name-input"
                                maxlength="128"
                                @keydown.enter.prevent="submitDisplayNameEdit"
                                @keydown.esc.prevent="cancelDisplayNameEdit"
                            />
                            <el-tooltip :content="roleMeta.tip" placement="top">
                                <span class="role-badge" :class="roleMeta.className">{{ roleMeta.label }}</span>
                            </el-tooltip>
                            <button
                                class="name-action-btn is-confirm"
                                type="button"
                                :disabled="!canSubmitDisplayName"
                                aria-label="提交昵称修改"
                                @click="submitDisplayNameEdit"
                            >
                                <el-icon><Check /></el-icon>
                            </button>
                            <button
                                class="name-action-btn is-cancel"
                                type="button"
                                :disabled="displayNameSaving"
                                aria-label="取消昵称修改"
                                @click="cancelDisplayNameEdit"
                            >
                                <el-icon><Close /></el-icon>
                            </button>
                        </template>
                        <template v-else>
                            <h1>{{ profile.displayName || '未命名用户' }}</h1>
                            <el-tooltip :content="roleMeta.tip" placement="top">
                                <span class="role-badge" :class="roleMeta.className">{{ roleMeta.label }}</span>
                            </el-tooltip>
                            <button
                                v-if="isSelf"
                                class="name-action-btn"
                                type="button"
                                aria-label="修改昵称"
                                @click="startDisplayNameEdit"
                            >
                                <el-icon><EditPen /></el-icon>
                            </button>
                        </template>
                    </div>
                    <p class="muted">@{{ profile.username || 'unknown' }}</p>
                </div>
                <el-button v-if="isSelf" class="profile-cta" type="primary" @click="openPurchaseIncentives">查看购买记录</el-button>
            </div>
            <template v-if="isSelf">
                <hr class="profile-divider" />
                <div class="stats-row">
                    <div class="stat-col">
                        <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
                        </svg>
                        <span class="stat-label">累计上传</span>
                        <strong class="stat-value">{{ profile.uploadsCount || 0 }} 张</strong>
                    </div>
                    <div class="stat-col">
                        <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                        </svg>
                        <span class="stat-label">绑定邮箱</span>
                        <strong class="stat-value">{{ profile.emailVerified ? (profile.emailMasked || '已绑定') : '未绑定' }}</strong>
                        <el-button size="small" @click="openBindDialog">
                            {{ profile.emailVerified ? '更换' : '绑定' }}
                        </el-button>
                    </div>
                    <div class="stat-col">
                        <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                        </svg>
                        <span class="stat-label">密码安全</span>
                        <strong class="stat-value">{{ profile.emailVerified ? '已启用' : '需绑定邮箱' }}</strong>
                        <el-button size="small" type="primary" :disabled="!profile.emailVerified" @click="openPasswordDialog">
                            修改
                        </el-button>
                    </div>
                    <div class="stat-col">
                        <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <ellipse cx="12" cy="5" rx="9" ry="3" /><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" /><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
                        </svg>
                        <span class="stat-label">钱包</span>
                        <strong class="stat-value">{{ walletBalanceText }}</strong>
                        <span class="stat-sublabel">可用于下单和拼团</span>
                    </div>
                </div>
            </template>
            <template v-else>
                <p class="muted profile-guest-stat">累计上传：{{ profile.uploadsCount || 0 }} 张</p>
            </template>
        </section>

        <section class="card">
            <header class="head">
                <h2>图片档案</h2>
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
            <div v-else class="grid">
                <article v-for="image in images" :key="image.id" class="item" @click="openImage(image)">
                    <img :src="image.thumbnailUrl" :alt="image.objectName || 'upload'" />
                    <p class="muted">{{ formatDate(image.createTime) }}</p>
                    <div v-if="isSelf" class="item-actions">
                        <button
                            class="edit-btn"
                            type="button"
                            :disabled="!image.vehicleId"
                            @click.stop="openEditDialog(image)"
                        >
                            {{ resolveEditButtonText(image) }}
                        </button>
                        <button
                            class="delete-img-btn"
                            type="button"
                            @click.stop="confirmDeleteImage(image)"
                        >
                            删除
                        </button>
                    </div>
                </article>
            </div>
        </section>

        <section class="card">
            <header class="head">
                <h2>{{ isSelf ? '我的喜爱' : '收藏' }}</h2>
                <el-pagination
                    v-if="favoritePagination.total > favoritePagination.size"
                    layout="prev, pager, next"
                    background
                    :page-size="favoritePagination.size"
                    :current-page="favoritePagination.page"
                    :total="favoritePagination.total"
                    @current-change="handleFavoritePageChange"
                />
            </header>
            <div v-if="favoritesLoading || profileLoading" class="state">收藏加载中...</div>
            <div v-else-if="!favorites.length && isSelf" class="state">还没有收藏车辆</div>
            <div v-else-if="!favorites.length && !isSelf" class="state">该用户暂无公开收藏</div>
            <div v-else class="grid">
                <article v-for="fav in favorites" :key="fav.vehicle?.id" class="item" @click="openFavorite(fav)">
                    <img
                        :src="fav.images?.[0]?.thumbnailUrl || ''"
                        :alt="fav.vehicle?.plateNumber || fav.vehicle?.modelName || 'favorite vehicle'"
                    />
                    <p class="muted">{{ fav.vehicle?.plateNumber || '未命名车辆' }}</p>
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

        <TradeRecordsDialog
            v-if="isSelf"
            v-model="tradeRecordsVisible"
            :records="tradeRecords"
            :loading="tradeRecordsLoading"
            :canceling-ids="cancelingTradeRecordIds"
            :page-size="30"
            @refresh="loadTradeRecords"
            @cancel-group="cancelTradeRecord"
            @download="openTradeDownload"
        />

        <el-dialog v-model="editVisible" title="修改图片关联车辆信息" width="900px" destroy-on-close>
            <el-form label-position="top" :model="editForm">
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="车牌号" required><el-input v-model="editForm.plateNumber" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="自编号"><el-input v-model="editForm.customNumber" /></el-form-item></el-col>
                </el-row>

                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="品牌（库内选择）">
                            <el-select v-model="editForm.brandId" clearable filterable placeholder="可搜索品牌">
                                <el-option v-for="option in brandOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="品牌名称（手动输入）"><el-input v-model="editForm.brandName" /></el-form-item></el-col>
                </el-row>

                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="车型（库内选择）">
                            <el-select v-model="editForm.modelId" clearable filterable placeholder="可搜索车型">
                                <el-option v-for="option in modelOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="车型名称（必填，可手填）" required><el-input v-model="editForm.modelName" /></el-form-item></el-col>
                </el-row>

                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="运营公司（库内选择）">
                            <el-select v-model="editForm.companyId" clearable filterable placeholder="可搜索公司">
                                <el-option v-for="option in filteredCompanyOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="运营公司名称（必填，可手填）" required><el-input v-model="editForm.companyName" /></el-form-item></el-col>
                </el-row>

                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="地区（省/市）">
                            <el-cascader
                                v-model="editRegionPath"
                                :options="allChinaRegionOptions"
                                :props="{ checkStrictly: true, emitPath: true }"
                                filterable
                                clearable
                                placeholder="请选择省份 / 城市"
                                style="width: 100%"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="地区显示"><el-input :model-value="regionDisplay" disabled /></el-form-item></el-col>
                </el-row>

                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="出厂日期"><el-date-picker v-model="editForm.factoryDate" type="month" value-format="YYYY-MM" placeholder="选择出厂年月" style="width:100%" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="上线日期"><el-date-picker v-model="editForm.launchDate" type="month" value-format="YYYY-MM" placeholder="选择上线年月" style="width:100%" /></el-form-item></el-col>
                </el-row>

                <el-form-item label="空调"><el-switch v-model="editForm.airConditioned" /></el-form-item>
                <el-form-item label="来源"><el-input v-model="editForm.source" /></el-form-item>

                <el-row v-if="showEditMotorField || showEditEngineField" :gutter="12">
                    <el-col v-if="showEditMotorField" :sm="showEditEngineField ? 12 : 24" :xs="24"><el-form-item label="电机"><el-input v-model="editForm.config.motor" /></el-form-item></el-col>
                    <el-col v-if="showEditEngineField" :sm="showEditMotorField ? 12 : 24" :xs="24"><el-form-item label="发动机"><el-input v-model="editForm.config.engine" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="燃料类型（中文）">
                            <el-select v-model="editForm.config.fuelType" clearable filterable placeholder="请选择燃料">
                                <el-option v-for="option in fuelOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="踏步"><el-input v-model="editForm.config.stepType" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="变速系统"><el-input v-model="editForm.config.transmissionSystem" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="悬挂"><el-input v-model="editForm.config.suspension" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="车桥"><el-input v-model="editForm.config.axle" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="其他配置"><el-input v-model="editForm.config.otherConfigs" /></el-form-item></el-col>
                </el-row>
                <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" /></el-form-item>
                <el-form-item label="关联线路">
                    <div style="display:flex;flex-direction:column;gap:8px;width:100%">
                        <div v-for="(ra, idx) in editForm.routes" :key="idx" style="display:flex;gap:8px;align-items:center">
                            <el-autocomplete
                                v-model="ra.routeNumber"
                                placeholder="输入或搜索线路号"
                                :fetch-suggestions="(kw, cb) => queryRouteSuggestions(kw, cb)"
                                @select="(item) => selectEditRoute(idx, item)"
                                style="flex:1"
                                clearable
                            />
                            <el-switch v-model="ra.isCurrent" active-text="当前" size="small" />
                            <el-button type="danger" size="small" text @click="editForm.routes.splice(idx,1)">×</el-button>
                        </div>
                        <el-button size="small" @click="editForm.routes.push({ routeId: null, routeNumber: '', isCurrent: true, remark: '' })">+ 关联已有线路</el-button>
                        <el-button size="small" type="primary" @click="openProfileRouteCreate">+ 创建新线路</el-button>
                    </div>
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="editVisible = false">取消</el-button>
                <el-button type="primary" :loading="editSubmitting" @click="submitEdit">{{ isNormalUser ? '提交审核' : '直接保存' }}</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="bindDialogVisible" title="绑定邮箱" width="520px" destroy-on-close>
            <el-form label-position="top">
                <el-form-item label="邮箱">
                    <el-input v-model.trim="bindForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="当前密码">
                    <el-input v-model="bindForm.currentPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="邮箱验证码">
                    <div class="inline-code">
                        <el-input v-model.trim="bindForm.emailCode" placeholder="请输入邮箱验证码" />
                        <el-button :loading="bindSendingCode" :disabled="!canSendBindCode" @click="sendBindCode">
                            {{ bindCodeText }}
                        </el-button>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="bindDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="bindSubmitting" @click="confirmBind">确认绑定</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="passwordDialogVisible" title="修改密码" width="520px" destroy-on-close>
            <el-form label-position="top">
                <el-form-item label="当前密码">
                    <el-input v-model="passwordForm.currentPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="邮箱验证码">
                    <div class="inline-code">
                        <el-input v-model.trim="passwordForm.emailCode" placeholder="请输入邮箱验证码" />
                        <el-button :loading="passwordSendingCode" :disabled="!canSendPasswordCode" @click="sendPasswordCode">
                            {{ passwordCodeText }}
                        </el-button>
                    </div>
                </el-form-item>
                <el-form-item label="新密码">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少 8 位" />
                </el-form-item>
                <el-form-item label="确认新密码">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="passwordDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="passwordSubmitting" @click="confirmPasswordChangeSubmit">确认修改</el-button>
            </template>
        </el-dialog>

        <RouteCreateDialog v-model="profileRouteCreateVisible" :prefillRouteNumber="profileRoutePrefillNumber"
            :prefillRegionId="editForm.regionId" :prefillCompanyId="editForm.companyId"
            @created="onProfileRouteCreated" />
    </div>
</template>

<script setup>
import { computed, defineAsyncComponent, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Check, Close, EditPen } from '@element-plus/icons-vue';
import { fetchUserImages, fetchUserProfile, updateMyDisplayName } from '@/api/users';
import { deleteImage } from '@/api/images';
import { fetchFavorites } from '@/api/vehicles';
import { fetchReviewInbox, submitVehicleUpdateReview } from '@/api/reviews';
import { fetchRoutes } from '@/api/routes';
import RouteCreateDialog from '@/components/Route/RouteCreateDialog.vue';
import { updateVehicle } from '@/api/vehicles';
import { fetchPortalRecords, refundTradeBridgeOrder } from '@/api/tradeBridge';
import {
    confirmBindEmail,
    confirmPasswordChange,
    sendBindEmailCode,
    sendPasswordChangeEmailCode
} from '@/api/auth';
import { formatDate } from '@/utils/formatters';
import { FUEL_OPTIONS, isCombustionFuel, isElectricFuel, normalizeFuelType } from '@/utils/fuel';
import { PROVINCE_CITY_DATA } from '@/utils/regionData';
import {
    formatRegionLabel,
    splitProvinceCity,
    findRegionIdByProvinceCity
} from '@/utils/region';

const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));
const TradeRecordsDialog = defineAsyncComponent(() => import('@/components/Trade/TradeRecordsDialog.vue'));
const route = useRoute();
const router = useRouter();
const store = useStore();

const targetUserId = computed(() => Number(route.params.userId) || null);
const profile = ref(null);
const profileLoading = ref(false);
const images = ref([]);
const imagesLoading = ref(false);
const favorites = ref([]);
const favoritesLoading = ref(false);
const tradeRecords = ref([]);
const tradeRecordsLoading = ref(false);
const tradeRecordsVisible = ref(false);
const cancelingTradeRecordIds = ref(new Set());
const pendingImageIds = ref(new Set());
const pagination = reactive({ page: 1, size: 12, total: 0 });
const favoritePagination = reactive({ page: 1, size: 12, total: 0 });
const avatarInitial = computed(() => (profile.value?.displayName || profile.value?.username || '?').slice(0, 1).toUpperCase());
const isNormalUser = computed(() => store.state.auth.profile?.role === 'USER');
const isSelf = computed(() => Number(store.state.auth.profile?.id || 0) === Number(profile.value?.id || 0));
const editingDisplayName = ref(false);
const displayNameDraft = ref('');
const displayNameSaving = ref(false);
const canSubmitDisplayName = computed(() => {
    if (displayNameSaving.value) return false;
    const next = displayNameDraft.value.trim();
    const current = (profile.value?.displayName || '').trim();
    return next.length >= 2 && next.length <= 128 && next !== current;
});
const roleMeta = computed(() => {
    const role = profile.value?.role || 'USER';
    if (role === 'STATION') {
        return { label: '站长', tip: '站长：可配置审核员权限与目标审核地区', className: 'is-station' };
    }
    if (role === 'REVIEWER') {
        return { label: '审核', tip: '审核员：可审核上传并修改车辆信息', className: 'is-reviewer' };
    }
    return { label: '普通', tip: '普通用户：上传与修改信息需经审核通过', className: 'is-user' };
});

const activeVehicleId = ref(null);
const detailVisible = computed(() => Boolean(activeVehicleId.value));
const activeVehicleDetail = computed(() => (activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] : null));
const detailLoading = computed(() => Boolean(activeVehicleId.value && store.state.vehicles.detailLoadingMap[activeVehicleId.value]));

const fuelOptions = FUEL_OPTIONS;
const brandOptions = computed(() => store.getters['brands/brandOptions'] || []);
const modelOptions = computed(() => store.getters['models/modelOptions'] || []);
const companyOptions = computed(() => store.getters['companies/companyOptions'] || []);
const filteredCompanyOptions = computed(() => {
    const list = companyOptions.value || [];
    if (editForm.regionId) return list.filter(o => o.regionId === editForm.regionId || o.regionId == null);
    return list;
});
const routeOptions = computed(() => store.getters['routes/routeOptions'] || []);

const profileRouteCreateVisible = ref(false);
const profileRoutePrefillNumber = ref('');
let profileRouteAbort = null;

const queryRouteSuggestions = (keyword, cb) => {
    if (profileRouteAbort) profileRouteAbort.abort();
    const controller = new AbortController();
    profileRouteAbort = controller;
    const kw = (keyword || '').trim();
    const params = { keyword: kw, size: 8, isActive: true };
    if (editForm.regionId) params.regionId = editForm.regionId;
    if (editForm.companyId) params.companyId = editForm.companyId;
    fetchRoutes(params, controller.signal)
        .then(resp => {
            const items = (Array.isArray(resp?.records) ? resp.records : (Array.isArray(resp) ? resp : [])).map(r => ({
                value: r.routeNumber, id: r.id, routeNumber: r.routeNumber,
                startStop: r.startStop, endStop: r.endStop, companyName: r.companyName
            }));
            if (!items.length && kw) {
                items.push({ value: kw, id: '__new__', routeNumber: kw, __hint: true });
            }
            cb(items);
        })
        .catch(() => { if (controller.signal.aborted) return; cb([]); });
};

const selectEditRoute = (idx, item) => {
    if (!item) return;
    if (item.id === '__new__') {
        profileRoutePrefillNumber.value = item.routeNumber || '';
        profileRouteCreateVisible.value = true;
        editForm.routes.splice(idx, 1);
        return;
    }
    editForm.routes[idx].routeId = item.id;
    editForm.routes[idx].routeNumber = item.routeNumber || item.value;
};

const openProfileRouteCreate = () => {
    profileRoutePrefillNumber.value = '';
    profileRouteCreateVisible.value = true;
};

const onProfileRouteCreated = (saved) => {
    if (saved?.id) {
        editForm.routes.push({ routeId: saved.id, routeNumber: saved.routeNumber, isCurrent: true, remark: '' });
    }
};

const regions = computed(() => store.state.regions.list || []);
const allChinaRegionOptions = computed(() =>
    (PROVINCE_CITY_DATA || []).map((province) => {
        const cities = Array.isArray(province.cities) && province.cities.length ? province.cities : [province.province];
        return {
            value: province.province,
            label: province.province,
            children: cities.map((city) => ({ value: city, label: city }))
        };
    })
);
const editRegionPath = ref([]);
const editRegionProvinceName = ref('');
const editRegionCityName = ref('');

const syncEditRegionFromPath = () => {
    const [provinceName, cityNameRaw] = Array.isArray(editRegionPath.value) ? editRegionPath.value : [];
    const cityName = cityNameRaw || provinceName || '';
    editRegionProvinceName.value = provinceName || '';
    editRegionCityName.value = cityName || '';
    editForm.regionId = findRegionIdByProvinceCity(regions.value, provinceName, cityName);
};

const editVisible = ref(false);
const editSubmitting = ref(false);
const editTarget = ref(null);
const editForm = reactive({
    vehicleId: null,
    plateNumber: '',
    customNumber: '',
    brandId: null,
    brandName: '',
    modelId: null,
    modelName: '',
    companyId: null,
    companyName: '',
    regionId: null,
    factoryDate: '',
    launchDate: '',
    airConditioned: false,
    source: '',
    remark: '',
    imageIds: [],
    routes: [],
    config: { brandId: null, modelId: null, motor: '', engine: '', fuelType: '', stepType: '', transmissionSystem: '', suspension: '', axle: '', otherConfigs: '' }
});

const bindDialogVisible = ref(false);
const bindSendingCode = ref(false);
const bindSubmitting = ref(false);
const bindCountdown = ref(0);
const bindForm = reactive({
    email: '',
    currentPassword: '',
    emailCode: '',
    challengeId: ''
});

const passwordDialogVisible = ref(false);
const passwordSendingCode = ref(false);
const passwordSubmitting = ref(false);
const passwordCountdown = ref(0);
const passwordForm = reactive({
    currentPassword: '',
    emailCode: '',
    challengeId: '',
    newPassword: '',
    confirmPassword: ''
});

let bindTimer = null;
let passwordTimer = null;

const regionDisplay = computed(() => {
    return formatRegionLabel(editForm.regionId, regions.value);
});

const canSendBindCode = computed(() =>
    !bindSendingCode.value &&
    bindCountdown.value <= 0 &&
    Boolean(bindForm.email && bindForm.currentPassword)
);
const bindCodeText = computed(() => (bindCountdown.value > 0 ? `${bindCountdown.value}s 后重发` : '发送验证码'));

const canSendPasswordCode = computed(() =>
    !passwordSendingCode.value &&
    passwordCountdown.value <= 0 &&
    Boolean(passwordForm.currentPassword)
);
const passwordCodeText = computed(() => (passwordCountdown.value > 0 ? `${passwordCountdown.value}s 后重发` : '发送验证码'));
const walletBalanceText = computed(() => `¥${(Number(profile.value?.balanceCents || 0) / 100).toFixed(2)}`);
const showEditMotorField = computed(() => isElectricFuel(editForm.config.fuelType));
const showEditEngineField = computed(() => isCombustionFuel(editForm.config.fuelType));

const ensureUserId = () => {
    if (!targetUserId.value) {
        ElMessage.error('未找到有效用户');
        router.replace({ name: 'Home' });
        return false;
    }
    return true;
};

const startDisplayNameEdit = () => {
    if (!isSelf.value || !profile.value) return;
    displayNameDraft.value = (profile.value.displayName || '').trim();
    editingDisplayName.value = true;
};

const cancelDisplayNameEdit = () => {
    editingDisplayName.value = false;
    displayNameDraft.value = '';
};

const submitDisplayNameEdit = async () => {
    if (!isSelf.value || !profile.value) return;
    const next = displayNameDraft.value.trim();
    const current = (profile.value.displayName || '').trim();
    if (next.length < 2 || next.length > 128) {
        ElMessage.warning('昵称长度需在 2-128 个字符之间');
        return;
    }
    if (!next || next === current) {
        cancelDisplayNameEdit();
        return;
    }
    displayNameSaving.value = true;
    try {
        const updated = await updateMyDisplayName({ displayName: next });
        profile.value = updated || profile.value;
        await store.dispatch('auth/fetchProfile');
        editingDisplayName.value = false;
        ElMessage.success('昵称已更新，正在刷新页面');
        window.setTimeout(() => window.location.reload(), 200);
    } catch (error) {
        ElMessage.error(error?.message || '更新昵称失败');
    } finally {
        displayNameSaving.value = false;
    }
};

const loadProfile = async () => {
    if (!ensureUserId()) return;
    profileLoading.value = true;
    try {
        profile.value = await fetchUserProfile(targetUserId.value);
    } finally {
        profileLoading.value = false;
    }
};

const loadImages = async () => {
    if (!ensureUserId()) return;
    imagesLoading.value = true;
    try {
        const data = await fetchUserImages(targetUserId.value, pagination);
        images.value = data.records || [];
        pagination.total = data.total || 0;
    } catch (error) {
        ElMessage.error(error?.message || '加载图片失败');
    } finally {
        imagesLoading.value = false;
    }
};

const loadFavorites = async () => {
    favoritesLoading.value = true;
    try {
        const pageData = await fetchFavorites({
            userId: targetUserId.value,
            page: favoritePagination.page,
            size: favoritePagination.size
        });
        favorites.value = Array.isArray(pageData?.records) ? pageData.records : [];
        favoritePagination.total = Number(pageData?.total || 0);
        favoritePagination.page = Number(pageData?.page || favoritePagination.page || 1);
        favoritePagination.size = Number(pageData?.size || favoritePagination.size || 12);
    } catch (error) {
        ElMessage.error(error?.message || '加载收藏失败');
        favorites.value = [];
        favoritePagination.total = 0;
    } finally {
        favoritesLoading.value = false;
    }
};

const loadTradeRecords = async () => {
    if (!isSelf.value) {
        tradeRecords.value = [];
        return;
    }
    tradeRecordsLoading.value = true;
    try {
        const list = await fetchPortalRecords(600);
        tradeRecords.value = Array.isArray(list) ? list : [];
    } catch (error) {
        tradeRecords.value = [];
        ElMessage.error(error?.message || '加载购买记录失败');
    } finally {
        tradeRecordsLoading.value = false;
    }
};

const loadPendingInbox = async () => {
    if (!isSelf.value) {
        pendingImageIds.value = new Set();
        return;
    }
    try {
        const list = await fetchReviewInbox();
        const next = new Set();
        (Array.isArray(list) ? list : []).forEach((item) => {
            if (item?.status === 'PENDING' && item?.imageId != null) {
                next.add(Number(item.imageId));
            }
        });
        pendingImageIds.value = next;
    } catch (error) {
        pendingImageIds.value = new Set();
    }
};

const handlePageChange = (page) => {
    pagination.page = page;
    loadImages();
};

const handleFavoritePageChange = (page) => {
    favoritePagination.page = page;
    loadFavorites();
};

const openImage = async (image) => {
    if (!image?.vehicleId) return;
    activeVehicleId.value = image.vehicleId;
    await store.dispatch('vehicles/loadVehicleDetail', { vehicleId: image.vehicleId, force: true });
};

const openFavorite = async (detail) => {
    if (!detail?.vehicle?.id) return;
    activeVehicleId.value = detail.vehicle.id;
    await store.dispatch('vehicles/loadVehicleDetail', { vehicleId: detail.vehicle.id, force: true });
};

const closeVehicleDetail = () => {
    activeVehicleId.value = null;
};

const openPurchaseIncentives = async () => {
    tradeRecordsVisible.value = true;
    if (!tradeRecords.value.length) {
        await loadTradeRecords();
    }
};

const goUpload = () => router.push({ name: 'Upload' });

const canCancelGroupRecord = (record) =>
    Number(record?.orderMode || 0) !== 2 &&
    Number(record?.tradeStatus || 0) === 0 &&
    !record?.canDownload &&
    Boolean(record?.outTradeNo);

const cancelTradeRecord = async (record) => {
    if (!canCancelGroupRecord(record)) {
        ElMessage.warning('当前记录不可取消');
        return;
    }
    const key = String(record.recordId || record.outTradeNo || '');
    if (cancelingTradeRecordIds.value.has(key)) return;
    const next = new Set(cancelingTradeRecordIds.value);
    next.add(key);
    cancelingTradeRecordIds.value = next;
    try {
        await refundTradeBridgeOrder({
            outTradeNo: String(record.outTradeNo),
            source: 'profile',
            channel: 'web'
        });
        ElMessage.success('取消拼团成功，金额已原路退回');
        await Promise.all([
            store.dispatch('auth/fetchProfile').catch(() => {}),
            loadProfile(),
            loadTradeRecords()
        ]);
    } catch (error) {
        ElMessage.error(error?.message || '取消拼团失败');
    } finally {
        const release = new Set(cancelingTradeRecordIds.value);
        release.delete(key);
        cancelingTradeRecordIds.value = release;
    }
};

const openTradeDownload = (record) => {
    if (record?.recordId) {
        router.push({ name: 'TradeDownload', query: { recordId: record.recordId } });
        return;
    }
    if (record?.downloadUrl) {
        window.open(record.downloadUrl, '_blank', 'noopener');
    }
};

const isImagePendingReview = (image) => pendingImageIds.value.has(Number(image?.id || 0));
const resolveEditButtonText = (image) => {
    if (image?.vehicleId) return '修改信息';
    return isImagePendingReview(image) ? '审核中' : '未关联车辆';
};

const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const startCountdown = (type, seconds = 60) => {
    const safe = Number(seconds) > 0 ? Number(seconds) : 60;
    if (type === 'bind') {
        bindCountdown.value = safe;
        if (bindTimer) clearInterval(bindTimer);
        bindTimer = setInterval(() => {
            bindCountdown.value -= 1;
            if (bindCountdown.value <= 0) {
                clearInterval(bindTimer);
                bindTimer = null;
            }
        }, 1000);
        return;
    }
    passwordCountdown.value = safe;
    if (passwordTimer) clearInterval(passwordTimer);
    passwordTimer = setInterval(() => {
        passwordCountdown.value -= 1;
        if (passwordCountdown.value <= 0) {
            clearInterval(passwordTimer);
            passwordTimer = null;
        }
    }, 1000);
};

const openBindDialog = () => {
    bindForm.email = '';
    bindForm.currentPassword = '';
    bindForm.emailCode = '';
    bindForm.challengeId = '';
    bindDialogVisible.value = true;
};

const sendBindCode = async () => {
    if (!bindForm.email || !bindForm.currentPassword) {
        ElMessage.warning('请先填写邮箱和当前密码');
        return;
    }
    if (!emailReg.test(bindForm.email)) {
        ElMessage.warning('邮箱格式不正确');
        return;
    }
    bindSendingCode.value = true;
    try {
        const data = await sendBindEmailCode({
            email: bindForm.email,
            currentPassword: bindForm.currentPassword
        });
        bindForm.challengeId = data?.challengeId || '';
        startCountdown('bind', data?.resendAfterSeconds || 60);
        ElMessage.success('验证码已发送到邮箱');
    } catch (error) {
        ElMessage.error(error?.message || '发送验证码失败');
    } finally {
        bindSendingCode.value = false;
    }
};

const confirmBind = async () => {
    if (!bindForm.challengeId || !bindForm.emailCode || !bindForm.email) {
        ElMessage.warning('请先完成邮箱验证');
        return;
    }
    bindSubmitting.value = true;
    try {
        await confirmBindEmail({
            challengeId: bindForm.challengeId,
            email: bindForm.email,
            emailCode: bindForm.emailCode
        });
        await loadProfile();
        bindDialogVisible.value = false;
        ElMessage.success('邮箱绑定成功');
    } catch (error) {
        ElMessage.error(error?.message || '邮箱绑定失败');
    } finally {
        bindSubmitting.value = false;
    }
};

const openPasswordDialog = () => {
    passwordForm.currentPassword = '';
    passwordForm.emailCode = '';
    passwordForm.challengeId = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    passwordDialogVisible.value = true;
};

const sendPasswordCode = async () => {
    if (!passwordForm.currentPassword) {
        ElMessage.warning('请先输入当前密码');
        return;
    }
    passwordSendingCode.value = true;
    try {
        const data = await sendPasswordChangeEmailCode({
            currentPassword: passwordForm.currentPassword
        });
        passwordForm.challengeId = data?.challengeId || '';
        startCountdown('password', data?.resendAfterSeconds || 60);
        ElMessage.success('验证码已发送到绑定邮箱');
    } catch (error) {
        ElMessage.error(error?.message || '发送验证码失败');
    } finally {
        passwordSendingCode.value = false;
    }
};

const confirmPasswordChangeSubmit = async () => {
    if (!passwordForm.challengeId || !passwordForm.emailCode) {
        ElMessage.warning('请先获取并填写验证码');
        return;
    }
    if (!passwordForm.newPassword || !passwordForm.confirmPassword) {
        ElMessage.warning('请输入新密码并确认');
        return;
    }
    if (passwordForm.newPassword.length < 8) {
        ElMessage.warning('新密码至少 8 位');
        return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        ElMessage.warning('两次输入的新密码不一致');
        return;
    }
    passwordSubmitting.value = true;
    try {
        await confirmPasswordChange({
            challengeId: passwordForm.challengeId,
            emailCode: passwordForm.emailCode,
            newPassword: passwordForm.newPassword,
            confirmPassword: passwordForm.confirmPassword
        });
        await store.dispatch('auth/logout');
        ElMessage.success('密码已修改，请重新登录');
        passwordDialogVisible.value = false;
        router.replace({ name: 'Login' });
    } catch (error) {
        ElMessage.error(error?.message || '修改密码失败');
    } finally {
        passwordSubmitting.value = false;
    }
};

const openEditDialog = async (image) => {
    if (!isSelf.value || !image?.vehicleId) return;
    await Promise.all([
        store.dispatch('regions/loadRegions').catch(() => {}),
        store.dispatch('brands/loadBrands').catch(() => {}),
        store.dispatch('models/loadModels').catch(() => {}),
        store.dispatch('companies/loadCompanies').catch(() => {}),
        store.dispatch('routes/loadRoutes', { size: 500 }).catch(() => {})
    ]);
    const detail = await store.dispatch('vehicles/loadVehicleDetail', { vehicleId: image.vehicleId, force: true });
    const vehicle = detail?.vehicle || {};
    const config = detail?.vehicleConfig || {};
    editForm.vehicleId = vehicle.id || null;
    editForm.plateNumber = vehicle.plateNumber || '';
    editForm.customNumber = vehicle.customNumber || '';
    editForm.brandId = config.brandId || vehicle.model?.brandId || null;
    editForm.brandName = config.brandName || vehicle.model?.brandName || '';
    editForm.modelId = vehicle.model?.id || config.modelId || null;
    editForm.modelName = vehicle.model?.name || config.modelName || '';
    editForm.companyId = vehicle.company?.id || null;
    editForm.companyName = vehicle.company?.name || '';
    editForm.regionId = vehicle.region?.id || null;
    const { provinceName, cityName } = splitProvinceCity(editForm.regionId, regions.value);
    if (provinceName) {
        editRegionPath.value = [provinceName, cityName || provinceName];
    } else {
        editRegionPath.value = [];
        editRegionProvinceName.value = '';
        editRegionCityName.value = '';
    }
    editForm.factoryDate = toMonthValue(vehicle.factoryDate);
    editForm.launchDate = toMonthValue(vehicle.launchDate);
    editForm.airConditioned = Boolean(vehicle.airConditioned);
    editForm.source = vehicle.source || '';
    editForm.remark = vehicle.remark || '';
    editForm.imageIds = (detail?.images || []).map((item) => item?.id).filter((id) => id != null);
    editForm.config = {
        brandId: editForm.brandId,
        modelId: editForm.modelId,
        motor: config.motor || '',
        engine: config.engine || '',
        fuelType: normalizeFuelType(config.fuelType || ''),
        stepType: config.stepType || '',
        transmissionSystem: config.transmissionSystem || '',
        suspension: config.suspension || '',
        axle: config.axle || '',
        otherConfigs: config.otherConfigs || ''
    };
    editForm.routes = (Array.isArray(vehicle.routes) ? vehicle.routes : []).map(vr => ({
        routeId: vr.routeId,
        routeNumber: vr.routeNumber || '',
        isCurrent: vr.isCurrent != null ? vr.isCurrent : true,
        remark: ''
    }));
    editTarget.value = { imageId: image.id, vehicleId: image.vehicleId };
    editVisible.value = true;
};

const confirmDeleteImage = async (image) => {
    if (!isSelf.value || !image?.id) return;
    const vehicleInfo = image.vehiclePlateNumber
        ? `关联车辆：${image.vehiclePlateNumber}${image.modelName ? ' · ' + image.modelName : ''}`
        : (image.vehicleId ? `已关联车辆 (ID: ${image.vehicleId})` : '未关联车辆');
    const message = h('div', { style: 'text-align:center' }, [
        h('img', { src: image.thumbnailUrl, style: 'width:100%;max-width:260px;border-radius:8px;margin-bottom:10px;display:block;margin-left:auto;margin-right:auto' }),
        h('p', { style: 'margin:4px 0;color:#64748b;font-size:13px' }, formatDate(image.createTime)),
        h('p', { style: 'margin:4px 0;font-weight:600' }, vehicleInfo),
        h('p', { style: 'margin:8px 0 0;color:#b91c1c;font-size:13px' }, '删除后不可恢复，确定要删除这张图片吗？'),
    ]);
    try {
        await ElMessageBox.confirm(
            message,
            '确认删除',
            { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: false }
        );
    } catch {
        return; // user cancelled
    }
    try {
        await deleteImage(image.id);
        ElMessage.success('图片已删除');
        await loadImages();
    } catch (error) {
        ElMessage.error(error?.message || '删除失败');
    }
};

const cleanText = (value) => {
    const text = String(value || '').trim();
    return text || null;
};

const toMonthValue = (value) => {
    const text = String(value || '').trim();
    if (!text) return '';
    if (/^\d{4}-\d{2}$/.test(text)) return text;
    if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return text.slice(0, 7);
    if (/^\d{4}-\d{2}/.test(text)) return text.slice(0, 7);
    return '';
};

const normalizeMonthToDate = (value) => {
    const month = toMonthValue(value);
    return month ? `${month}-01` : null;
};

const submitEdit = async () => {
    if (!editTarget.value?.vehicleId) return;
    const fallbackRegion = splitProvinceCity(editForm.regionId, regions.value);
    const provinceName = editRegionProvinceName.value || fallbackRegion.provinceName;
    const rawCityName = editRegionCityName.value || fallbackRegion.cityName;
    const cityName = rawCityName && rawCityName !== provinceName ? rawCityName : null;
    const payload = {
        vehicleId: editForm.vehicleId,
        plateNumber: cleanText(editForm.plateNumber),
        customNumber: cleanText(editForm.customNumber),
        brandId: editForm.brandId || null,
        brandName: cleanText(editForm.brandName),
        modelId: editForm.modelId || null,
        modelName: cleanText(editForm.modelName),
        companyId: editForm.companyId || null,
        companyName: cleanText(editForm.companyName),
        regionId: editForm.regionId || null,
        regionProvince: provinceName || null,
        regionCity: cityName || null,
        factoryDate: normalizeMonthToDate(editForm.factoryDate),
        launchDate: normalizeMonthToDate(editForm.launchDate),
        airConditioned: Boolean(editForm.airConditioned),
        source: cleanText(editForm.source),
        remark: cleanText(editForm.remark),
        imageIds: Array.isArray(editForm.imageIds) ? editForm.imageIds : [],
        routes: (editForm.routes || []).filter(r => r.routeId || r.routeNumber).map(r => ({
            routeId: r.routeId || null,
            routeNumber: r.routeNumber || null,
            isCurrent: r.isCurrent != null ? r.isCurrent : true,
            remark: r.remark || ''
        })),
        config: {
            brandId: editForm.brandId || null,
            modelId: editForm.modelId || null,
            motor: cleanText(editForm.config.motor),
            engine: cleanText(editForm.config.engine),
            fuelType: normalizeFuelType(cleanText(editForm.config.fuelType)),
            stepType: cleanText(editForm.config.stepType),
            transmissionSystem: cleanText(editForm.config.transmissionSystem),
            suspension: cleanText(editForm.config.suspension),
            axle: cleanText(editForm.config.axle),
            otherConfigs: cleanText(editForm.config.otherConfigs)
        }
    };
    if (!payload.plateNumber || (!payload.modelId && !payload.modelName) || (!payload.companyId && !payload.companyName)) {
        ElMessage.warning('请完善车辆基础信息');
        return;
    }

    editSubmitting.value = true;
    try {
        if (isNormalUser.value) {
            await submitVehicleUpdateReview(editTarget.value.vehicleId, editTarget.value.imageId, payload);
            ElMessage.success('修改申请已提交，等待审核');
        } else {
            const updatedDetail = await updateVehicle(editTarget.value.vehicleId, payload);
            store.commit('vehicles/SYNC_VEHICLE_DETAIL', updatedDetail);
            ElMessage.success('车辆信息已更新');
        }
        await Promise.all([
            store.dispatch('regions/loadRegions').catch(() => {}),
            store.dispatch('brands/loadBrands').catch(() => {}),
            store.dispatch('models/loadModels').catch(() => {}),
            store.dispatch('companies/loadCompanies').catch(() => {})
        ]);
        await loadImages();
        editVisible.value = false;
    } catch (error) {
        ElMessage.error(error?.message || '提交失败');
    } finally {
        editSubmitting.value = false;
    }
};

const findOptionIdByLabel = (options, label) => {
    const text = String(label || '').trim();
    if (!text) return null;
    const hit = (options || []).find((item) => String(item.label || '').trim() === text);
    return hit ? hit.value : null;
};

watch(
    () => editRegionPath.value,
    () => {
        syncEditRegionFromPath();
    },
    { deep: true }
);

watch(
    () => regions.value,
    () => {
        if (!editRegionPath.value.length && editForm.regionId) {
            const { provinceName, cityName } = splitProvinceCity(editForm.regionId, regions.value);
            if (provinceName) {
                editRegionPath.value = [provinceName, cityName || provinceName];
            }
        }
        syncEditRegionFromPath();
    }
);

watch(() => editForm.brandId, (id) => {
    const hit = brandOptions.value.find((item) => item.value === id);
    if (hit) editForm.brandName = hit.label;
    editForm.config.brandId = id || null;
});
watch(
    () => editForm.brandName,
    (name) => {
        const matchedId = findOptionIdByLabel(brandOptions.value, name);
        if (editForm.brandId !== matchedId) {
            editForm.brandId = matchedId;
            editForm.config.brandId = matchedId;
        }
    }
);
watch(() => editForm.modelId, (id) => {
    const hit = modelOptions.value.find((item) => item.value === id);
    if (hit) editForm.modelName = hit.label;
    editForm.config.modelId = id || null;
});
watch(
    () => editForm.modelName,
    (name) => {
        const matchedId = findOptionIdByLabel(modelOptions.value, name);
        if (editForm.modelId !== matchedId) {
            editForm.modelId = matchedId;
            editForm.config.modelId = matchedId;
        }
    }
);
watch(() => editForm.companyId, (id) => {
    const hit = companyOptions.value.find((item) => item.value === id);
    if (hit) editForm.companyName = hit.label;
});
watch(
    () => editForm.companyName,
    (name) => {
        const matchedId = findOptionIdByLabel(companyOptions.value, name);
        if (editForm.companyId !== matchedId) {
            editForm.companyId = matchedId;
        }
    }
);

watch(
    () => route.params.userId,
    async () => {
        pagination.page = 1;
        favoritePagination.page = 1;
        await loadProfile();
        await loadImages();
        await loadPendingInbox();
        await loadFavorites();
        await loadTradeRecords();
    },
    { immediate: true }
);

watch(
    () => isSelf.value,
    async (val) => {
        if (!val) {
            tradeRecordsVisible.value = false;
            tradeRecords.value = [];
            cancelingTradeRecordIds.value = new Set();
            return;
        }
        await loadTradeRecords();
    }
);

onMounted(() => {
    if (!store.state.auth.profile) store.dispatch('auth/fetchProfile').catch(() => {});
    store.dispatch('regions/loadRegions').catch(() => {});
    store.dispatch('brands/loadBrands').catch(() => {});
    store.dispatch('models/loadModels').catch(() => {});
    store.dispatch('companies/loadCompanies').catch(() => {});
});

onBeforeUnmount(() => {
    if (bindTimer) clearInterval(bindTimer);
    if (passwordTimer) clearInterval(passwordTimer);
});
</script>

<style scoped lang="scss">
.user-profile-page { width: min(1100px, 100%); margin: 0 auto; padding: 16px; display: flex; flex-direction: column; gap: 20px; }
.card { background: #fff; border-radius: 20px; padding: 20px; box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08); }
.profile-top { display: flex; align-items: center; gap: 14px; }
.profile-info { min-width: 0; flex: 1; }
.profile-cta { flex-shrink: 0; }
.profile-divider { border: 0; border-top: 1px solid #e2e8f0; margin: 16px 0; }
.profile-guest-stat { margin-top: 8px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0; }
.stat-col {
    display: flex; flex-direction: column; align-items: flex-start; gap: 4px;
    padding: 0 16px; border-right: 1px solid #e2e8f0;
}
.stat-col:first-child { padding-left: 0; }
.stat-col:last-child { border-right: none; padding-right: 0; }
.stat-icon { width: 18px; height: 18px; color: #94a3b8; flex-shrink: 0; }
.stat-label { font-size: 12px; color: #94a3b8; }
.stat-value { color: #0f172a; font-size: 14px; }
.stat-sublabel { font-size: 11px; color: #94a3b8; }
.display-name-row { display: flex; align-items: center; gap: 8px; min-width: 0; flex-wrap: wrap; }
.display-name-row h1 { margin: 0; line-height: 1.2; }
.display-name-input { width: min(280px, 60vw); }
.role-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 24px;
    padding: 0 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    border: 1px solid transparent;
    cursor: help;
    user-select: none;
}
.role-badge.is-station {
    color: #9a3412;
    background: #ffedd5;
    border-color: #fdba74;
}
.role-badge.is-reviewer {
    color: #1d4ed8;
    background: #dbeafe;
    border-color: #93c5fd;
}
.role-badge.is-user {
    color: #0f766e;
    background: #ccfbf1;
    border-color: #5eead4;
}
.name-action-btn {
    width: 30px;
    height: 30px;
    border-radius: 8px;
    border: 1px solid #cbd5e1;
    background: #fff;
    color: #334155;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    padding: 0;
}
.name-action-btn.is-confirm { color: #15803d; border-color: rgba(21, 128, 61, 0.35); background: rgba(240, 253, 244, 0.9); }
.name-action-btn.is-cancel { color: #b91c1c; border-color: rgba(185, 28, 28, 0.35); background: rgba(254, 242, 242, 0.95); }
.name-action-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.avatar { width: 56px; height: 56px; border-radius: 50%; background: #1d4ed8; color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.muted { color: #64748b; margin: 4px 0; }
.inline-code { display: grid; grid-template-columns: 1fr auto; gap: 10px; width: 100%; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; margin-top: 12px; }
.item { background: #f8fafc; border-radius: 14px; padding: 10px; cursor: pointer; box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.2); }
.item img { width: 100%; aspect-ratio: 16 / 10; height: auto; object-fit: cover; border-radius: 10px; margin-bottom: 8px; display: block; }
.edit-btn { flex: 1; border: 1px solid rgba(37, 99, 235, 0.28); background: rgba(37, 99, 235, 0.08); color: #1d4ed8; border-radius: 10px; padding: 6px; font-size: 12px; cursor: pointer; white-space: nowrap; }
.edit-btn:disabled { cursor: not-allowed; color: #94a3b8; border-color: #e2e8f0; background: #f8fafc; }
.delete-img-btn { flex: 1; border: 1px solid rgba(220, 38, 38, 0.28); background: rgba(220, 38, 38, 0.06); color: #b91c1c; border-radius: 10px; padding: 6px; font-size: 12px; cursor: pointer; transition: background .15s; white-space: nowrap; }
.delete-img-btn:hover { background: rgba(220, 38, 38, 0.14); }
.item-actions { display: flex; gap: 6px; margin-top: 6px; }
.state { text-align: center; padding: 40px 0; color: #94a3b8; }

@media (max-width: 768px) {
    .stats-row {
        grid-template-columns: repeat(2, 1fr);
        row-gap: 12px;
    }
    .stat-col:nth-child(2) { border-right: none; }
    .stat-col:nth-child(3) { padding-left: 0; }
}

@media (max-width: 560px) {
    .profile-top { gap: 10px; }
    .avatar { display: none; }
    .display-name-input { width: 100%; }
    .stats-row { grid-template-columns: 1fr; gap: 10px; }
    .stat-col { border-right: none; padding: 0 0 10px 0; border-bottom: 1px solid #e2e8f0; }
    .stat-col:last-child { border-bottom: none; padding-bottom: 0; }
    .stat-col:nth-child(2) { padding-left: 0; }
    .stat-col:nth-child(3) { padding-left: 0; }
    .profile-cta { width: 100%; }
}
</style>

