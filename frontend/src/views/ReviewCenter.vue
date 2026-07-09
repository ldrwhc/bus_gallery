<template>
    <div class="review-page">
        <main class="review-main">
            <div class="panel-switch">
                <button
                    type="button"
                    :class="['switch-btn', { active: activePanel === 'review' }]"
                    @click="switchPanel('review')"
                >
                    车辆审核
                </button>
                <button
                    type="button"
                    :class="['switch-btn', { active: activePanel === 'manage' }]"
                    @click="switchPanel('manage')"
                >
                    车辆管理
                </button>
            </div>

            <section v-if="activePanel === 'review'" key="review" class="panel">
                <header class="panel-head">
                    <div>
                        <p class="eyebrow">Review</p>
                        <h1>审核中心</h1>
                        <p class="muted">当前审核地区：{{ currentReviewRegionLabel }}</p>
                    </div>
                    <el-button type="primary" :loading="loading" @click="loadPending">刷新列表</el-button>
                </header>

                <div class="layout">
                    <aside class="list">
                        <h2>待审核</h2>
                        <div v-if="loading" class="state">加载中...</div>
                        <div v-else-if="!pendingList.length" class="state">当前没有待审核记录</div>
                        <button
                            v-for="item in pendingList"
                            :key="item.id"
                            type="button"
                            :class="['row', { active: selected?.id === item.id }]"
                            @click="selectSubmission(item)"
                        >
                            <strong>#{{ item.id }}</strong>
                            <span>{{ item.requestPayload?.plateNumber || '未填写车牌' }}</span>
                        </button>
                    </aside>

                    <section class="detail">
                        <template v-if="selected">
                            <div class="preview">
                                <el-image
                                    v-if="selected.imageThumbnailUrl || selected.imageUrl"
                                    class="preview-image"
                                    :src="selected.imageUrl || selected.imageThumbnailUrl"
                                    alt="submission"
                                    fit="contain"
                                    :preview-src-list="previewImageList"
                                    preview-teleported
                                />
                                <p v-else class="state">暂无图片预览</p>
                                <p v-if="previewImageList.length" class="preview-tip">点击图片查看大图</p>
                            </div>

                            <el-form label-position="top" :model="form">
                                <h3>车辆基础信息</h3>
                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24"><el-form-item label="车牌号" required><el-input v-model="form.plateNumber" /></el-form-item></el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="自编号"><el-input v-model="form.customNumber" /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="品牌（库内选择）">
                                            <el-select v-model="form.brandId" clearable filterable placeholder="可搜索品牌">
                                                <el-option v-for="option in brandOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="品牌名称（手动输入）"><el-input v-model="form.brandName" /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="车型（库内选择）">
                                            <el-select v-model="form.modelId" clearable filterable placeholder="可搜索车型">
                                                <el-option v-for="option in modelOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="车型名称（必填，可手填）" required><el-input v-model="form.modelName" /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="运营公司（库内选择）">
                                            <el-select v-model="form.companyId" clearable filterable placeholder="可搜索公司">
                                                <el-option v-for="option in companyOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="运营公司名称（必填，可手填）" required><el-input v-model="form.companyName" /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="地区（省/市）">
                                            <RegionSelector v-model="form.regionId" :regions="regions" />
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="地区显示"><el-input :model-value="regionDisplay" disabled /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24"><el-form-item label="出厂日期"><el-date-picker v-model="form.factoryDate" type="month" value-format="YYYY-MM" placeholder="选择出厂年月" style="width:100%" /></el-form-item></el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="上线日期"><el-date-picker v-model="form.launchDate" type="month" value-format="YYYY-MM" placeholder="选择上线年月" style="width:100%" /></el-form-item></el-col>
                                </el-row>
                                <el-form-item label="空调"><el-switch v-model="form.airConditioned" /></el-form-item>
                                <el-form-item label="来源"><el-input v-model="form.source" /></el-form-item>
                                <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>

                                <h3>配置信息</h3>
                                <el-row v-if="showMotorField || showEngineField" :gutter="12">
                                    <el-col v-if="showMotorField" :sm="showEngineField ? 12 : 24" :xs="24"><el-form-item label="电机"><el-input v-model="form.config.motor" /></el-form-item></el-col>
                                    <el-col v-if="showEngineField" :sm="showMotorField ? 12 : 24" :xs="24"><el-form-item label="发动机"><el-input v-model="form.config.engine" /></el-form-item></el-col>
                                </el-row>
                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="燃料类型（中文）">
                                            <el-select v-model="form.config.fuelType" clearable filterable placeholder="请选择燃料">
                                                <el-option v-for="option in fuelOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="踏步"><el-input v-model="form.config.stepType" /></el-form-item></el-col>
                                </el-row>
                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24"><el-form-item label="变速系统"><el-input v-model="form.config.transmissionSystem" /></el-form-item></el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="悬挂"><el-input v-model="form.config.suspension" /></el-form-item></el-col>
                                </el-row>
                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24"><el-form-item label="车桥"><el-input v-model="form.config.axle" /></el-form-item></el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="其他配置"><el-input v-model="form.config.otherConfigs" /></el-form-item></el-col>
                                </el-row>

                                <div class="actions">
                                    <el-button type="success" :loading="submitting" @click="handleApprove">同意入库</el-button>
                                    <el-button type="danger" plain :loading="submitting" @click="rejectOpen = !rejectOpen">拒绝</el-button>
                                </div>
                                <div v-if="submitting || submitProgress > 0" class="submit-progress">
                                    <span class="submit-progress__label">{{ submitProgressLabel }}</span>
                                    <el-progress
                                        :percentage="submitProgress"
                                        :status="submitProgressStatus"
                                        :stroke-width="8"
                                        :show-text="false"
                                    />
                                </div>

                                <div v-if="rejectOpen" class="reject-box">
                                    <el-select v-model="rejectCode" placeholder="请选择拒绝理由" style="width: 100%">
                                        <el-option v-for="item in rejectReasons" :key="item.code" :label="item.label" :value="item.code" />
                                    </el-select>
                                    <el-input
                                        v-if="rejectCode === 'OTHER'"
                                        v-model="rejectCustomReason"
                                        type="textarea"
                                        placeholder="请输入自定义拒绝原因"
                                    />
                                    <el-button type="danger" :loading="submitting" @click="handleReject">确认拒绝</el-button>
                                </div>
                            </el-form>
                        </template>
                        <div v-else class="state">请选择一条待审核记录</div>
                    </section>
                </div>
            </section>

            <section v-else key="manage" class="panel manage-panel-wrap">
                <header class="panel-head">
                    <div>
                        <p class="eyebrow">Vehicle Manage</p>
                        <h2>车辆管理</h2>
                        <p class="muted" v-if="canSelectManageRegion">站长可查询全站车辆</p>
                        <p class="muted" v-else>审核范围：{{ currentReviewRegionLabel }}</p>
                    </div>
                    <el-button type="primary" :loading="manageLoading" @click="loadManageList(true)">刷新车辆</el-button>
                </header>

                <div class="manage-toolbar">
                    <el-input
                        v-model="manageFilters.keyword"
                        placeholder="按车牌/自编号搜索"
                        clearable
                        @keyup.enter="loadManageList(true)"
                    />
                    <div class="manage-region" v-if="canSelectManageRegion">
                        <RegionSelector v-model="manageFilters.regionId" :regions="regions" />
                    </div>
                    <el-button type="primary" :loading="manageLoading" @click="loadManageList(true)">查询</el-button>
                    <el-button
                        type="danger"
                        plain
                        :loading="manageBatchDeleting"
                        :disabled="!selectedManageVehicleIds.length"
                        @click="confirmBatchDeleteVehicles"
                    >
                        批量删除
                    </el-button>
                </div>

                <el-table
                    :data="manageRecords"
                    v-loading="manageLoading"
                    stripe
                    border
                    :row-key="manageRowKey"
                    @selection-change="handleManageSelectionChange"
                    empty-text="暂无可管理车辆"
                >
                    <el-table-column type="selection" width="52" reserve-selection />
                    <el-table-column label="ID" width="90">
                        <template #default="{ row }">{{ row.vehicle?.id || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="车牌" min-width="130">
                        <template #default="{ row }">{{ row.vehicle?.plateNumber || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="地区" min-width="140">
                        <template #default="{ row }">{{ row.vehicle?.region?.name || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="运营公司" min-width="160">
                        <template #default="{ row }">{{ row.vehicle?.company?.name || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="车型" min-width="160">
                        <template #default="{ row }">{{ row.vehicle?.model?.name || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="170" fixed="right">
                        <template #default="{ row }">
                            <div class="manage-actions">
                                <el-button type="primary" text size="small" @click="openManageEdit(row)">编辑</el-button>
                                <el-button type="danger" text size="small" @click="confirmDeleteVehicle(row)">删除</el-button>
                            </div>
                        </template>
                    </el-table-column>
                </el-table>

                <div class="manage-footer">
                    <span>已加载 {{ manageRecords.length }} / {{ manageTotal }}</span>
                    <el-button
                        v-if="manageHasMore"
                        size="small"
                        :loading="manageLoading"
                        @click="loadManageList(false)"
                    >加载更多</el-button>
                </div>
            </section>
        </main>

        <el-dialog
            v-model="manageDialogVisible"
            title="编辑车辆"
            width="min(860px, 92vw)"
            :close-on-click-modal="true"
            destroy-on-close
        >
            <el-form v-loading="manageEditLoading" label-position="top" :model="manageForm">
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="车牌号" required><el-input v-model="manageForm.plateNumber" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="自编号"><el-input v-model="manageForm.customNumber" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="品牌（库内选择）">
                            <el-select v-model="manageForm.brandId" clearable filterable placeholder="可搜索品牌">
                                <el-option v-for="option in brandOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="品牌名称（手动输入）"><el-input v-model="manageForm.brandName" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="车型（库内选择）">
                            <el-select v-model="manageForm.modelId" clearable filterable placeholder="可搜索车型">
                                <el-option v-for="option in modelOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="车型名称（必填，可手填）" required><el-input v-model="manageForm.modelName" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="运营公司（库内选择）">
                            <el-select v-model="manageForm.companyId" clearable filterable placeholder="可搜索公司">
                                <el-option v-for="option in companyOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="运营公司名称（必填，可手填）" required><el-input v-model="manageForm.companyName" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="地区（省/市）">
                            <RegionSelector v-model="manageForm.regionId" :regions="regions" />
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="地区显示"><el-input :model-value="manageRegionDisplay" disabled /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="出厂日期"><el-date-picker v-model="manageForm.factoryDate" type="month" value-format="YYYY-MM" placeholder="选择出厂年月" style="width:100%" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="上线日期"><el-date-picker v-model="manageForm.launchDate" type="month" value-format="YYYY-MM" placeholder="选择上线年月" style="width:100%" /></el-form-item></el-col>
                </el-row>
                <el-form-item label="空调"><el-switch v-model="manageForm.airConditioned" /></el-form-item>
                <el-form-item label="来源"><el-input v-model="manageForm.source" /></el-form-item>
                <el-form-item label="备注"><el-input v-model="manageForm.remark" type="textarea" /></el-form-item>
                <el-form-item label="关联线路">
                    <div style="display:flex;flex-direction:column;gap:8px;width:100%">
                        <div v-for="(ra, idx) in manageForm.routes" :key="idx" style="display:flex;gap:8px;align-items:center">
                            <el-autocomplete
                                v-model="ra.routeNumber"
                                placeholder="输入或搜索线路号"
                                :fetch-suggestions="(kw, cb) => queryManageRouteSuggestions(kw, cb)"
                                @select="(item) => selectManageRoute(idx, item)"
                                style="flex:1"
                                :disabled="manageEditLoading"
                                clearable
                            />
                            <el-switch v-model="ra.isCurrent" active-text="当前" size="small" :disabled="manageEditLoading" />
                            <el-button type="danger" size="small" text @click="manageForm.routes.splice(idx,1)" :disabled="manageEditLoading">×</el-button>
                        </div>
                        <el-button size="small" @click="manageForm.routes.push({ routeId: null, routeNumber: '', isCurrent: true, remark: '' })" :disabled="manageEditLoading">+ 关联已有线路</el-button>
                        <el-button size="small" type="primary" @click="openManageRouteCreate" :disabled="manageEditLoading">+ 创建新线路</el-button>
                    </div>
                </el-form-item>
                <h3>配置信息</h3>
                <el-row v-if="showManageMotorField || showManageEngineField" :gutter="12">
                    <el-col v-if="showManageMotorField" :sm="showManageEngineField ? 12 : 24" :xs="24"><el-form-item label="电机"><el-input v-model="manageForm.config.motor" /></el-form-item></el-col>
                    <el-col v-if="showManageEngineField" :sm="showManageMotorField ? 12 : 24" :xs="24"><el-form-item label="发动机"><el-input v-model="manageForm.config.engine" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24">
                        <el-form-item label="燃料类型（中文）">
                            <el-select v-model="manageForm.config.fuelType" clearable filterable placeholder="请选择燃料">
                                <el-option v-for="option in fuelOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="踏步"><el-input v-model="manageForm.config.stepType" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="变速系统"><el-input v-model="manageForm.config.transmissionSystem" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="悬挂"><el-input v-model="manageForm.config.suspension" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :sm="12" :xs="24"><el-form-item label="车桥"><el-input v-model="manageForm.config.axle" /></el-form-item></el-col>
                    <el-col :sm="12" :xs="24"><el-form-item label="其他配置"><el-input v-model="manageForm.config.otherConfigs" /></el-form-item></el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="manageDialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="manageSaving" @click="submitManageUpdate">保存修改</el-button>
                </div>
            </template>
        </el-dialog>
        <RouteCreateDialog v-model="manageRouteCreateVisible" :prefillRouteNumber="manageRoutePrefillNumber"
            :prefillRegionId="manageForm.regionId" :prefillCompanyId="manageForm.companyId"
            @created="onManageRouteCreated" />
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { approveSubmission, fetchPendingSubmissions, rejectSubmission } from '@/api/reviews';
import { fetchRoutes } from '@/api/routes';
import RouteCreateDialog from '@/components/Route/RouteCreateDialog.vue';
import { batchDeleteVehicles, deleteVehicle, fetchManageVehiclePage, fetchVehicleGalleryDetail, updateVehicle } from '@/api/vehicles';
import { FUEL_OPTIONS, isCombustionFuel, isElectricFuel, normalizeFuelType } from '@/utils/fuel';
import RegionSelector from '@/components/Region/RegionSelector.vue';
import {
    formatRegionLabel,
    splitProvinceCity
} from '@/utils/region';

const store = useStore();
const route = useRoute();
const activePanel = ref(route.query.panel === 'manage' ? 'manage' : 'review');

const loading = ref(false);
const submitting = ref(false);
const pendingList = ref([]);
const selected = ref(null);
const rejectOpen = ref(false);
const rejectCode = ref('');
const rejectCustomReason = ref('');
const submitProgress = ref(0);
const submitProgressStatus = ref('');
const submitProgressLabel = ref('');
let submitProgressTimer = null;

const manageLoading = ref(false);
const manageRecords = ref([]);
const manageTotal = ref(0);
const manageHasMore = ref(false);
const manageCursor = reactive({ nextLaunch: null, nextId: null });
const manageFilters = reactive({ keyword: '', regionId: null });
const manageDialogVisible = ref(false);
const manageEditLoading = ref(false);
const manageSaving = ref(false);
const manageEditingVehicleId = ref(null);
const manageBatchDeleting = ref(false);
const selectedManageVehicleIds = ref([]);

const switchPanel = (panel) => {
    if (activePanel.value === panel) {
        return;
    }
    activePanel.value = panel;
    if (panel === 'manage') {
        loadManageList(true);
    } else {
        loadPending();
    }
};

const fuelOptions = FUEL_OPTIONS;
const brandOptions = computed(() => store.getters['brands/brandOptions'] || []);
const modelOptions = computed(() => store.getters['models/modelOptions'] || []);
const companyOptions = computed(() => store.getters['companies/companyOptions'] || []);
const routeOptions = computed(() => store.getters['routes/routeOptions'] || []);

const manageRouteCreateVisible = ref(false);
const manageRoutePrefillNumber = ref('');
let manageRouteAbort = null;

const queryManageRouteSuggestions = (keyword, cb) => {
    if (manageRouteAbort) manageRouteAbort.abort();
    const controller = new AbortController();
    manageRouteAbort = controller;
    const kw = (keyword || '').trim();
    const params = { keyword: kw, size: 8, isActive: true };
    if (manageForm.regionId) params.regionId = manageForm.regionId;
    if (manageForm.companyId) params.companyId = manageForm.companyId;
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

const selectManageRoute = (idx, item) => {
    if (!item) return;
    if (item.id === '__new__') {
        manageRoutePrefillNumber.value = item.routeNumber || '';
        manageRouteCreateVisible.value = true;
        manageForm.routes.splice(idx, 1);
        return;
    }
    manageForm.routes[idx].routeId = item.id;
    manageForm.routes[idx].routeNumber = item.routeNumber || item.value;
};

const openManageRouteCreate = () => {
    manageRoutePrefillNumber.value = '';
    manageRouteCreateVisible.value = true;
};

const onManageRouteCreated = (saved) => {
    if (saved?.id) {
        manageForm.routes.push({ routeId: saved.id, routeNumber: saved.routeNumber, isCurrent: true, remark: '' });
    }
};
const regions = computed(() => store.state.regions.list || []);
const previewImageList = computed(() => {
    const imageUrl = selected.value?.imageUrl;
    const thumbnailUrl = selected.value?.imageThumbnailUrl;
    if (imageUrl) return [imageUrl];
    if (thumbnailUrl) return [thumbnailUrl];
    return [];
});
const regionDisplay = computed(() => {
    return formatRegionLabel(form.regionId, regions.value);
});
const currentReviewRegionLabel = computed(() => {
    const profile = store.state.auth.profile || {};
    if (profile.role === 'STATION') return '全部地区（站长）';
    if (profile.role !== 'REVIEWER') return '未登录审核身份';
    const reviewRegionId = profile.reviewRegionId;
    if (!reviewRegionId) return '未分配';
    const label = formatRegionLabel(reviewRegionId, regions.value);
    return label === '-' ? `ID ${reviewRegionId}` : label;
});
const canSelectManageRegion = computed(() => (store.state.auth.profile || {}).role === 'STATION');

const rejectReasons = [
    { code: 'BLURRY', label: '图片模糊不可用' },
    { code: 'MISMATCH', label: '图片与车辆信息不一致' },
    { code: 'DUPLICATE', label: '与现有记录重复' },
    { code: 'INCOMPLETE', label: '关键字段不完整' },
    { code: 'OTHER', label: '其他' }
];

const blankPayload = () => ({
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
    regionProvince: '',
    regionCity: '',
    factoryDate: '',
    launchDate: '',
    airConditioned: false,
    source: '',
    remark: '',
    imageIds: [],
    config: {
        brandId: null,
        modelId: null,
        motor: '',
        engine: '',
        fuelType: '',
        stepType: '',
        transmissionSystem: '',
        suspension: '',
        axle: '',
        otherConfigs: ''
    },
    routes: []
});

const form = reactive(blankPayload());
const manageForm = reactive(blankPayload());
const manageRegionDisplay = computed(() => formatRegionLabel(manageForm.regionId, regions.value));
const showMotorField = computed(() => isElectricFuel(form.config.fuelType));
const showEngineField = computed(() => isCombustionFuel(form.config.fuelType));
const showManageMotorField = computed(() => isElectricFuel(manageForm.config.fuelType));
const showManageEngineField = computed(() => isCombustionFuel(manageForm.config.fuelType));

const fillForm = (payload = {}) => {
    const base = blankPayload();
    Object.assign(base, payload || {});
    base.config = { ...blankPayload().config, ...(payload?.config || {}) };
    base.config.fuelType = normalizeFuelType(base.config.fuelType || '');
    base.factoryDate = toMonthValue(base.factoryDate);
    base.launchDate = toMonthValue(base.launchDate);
    Object.assign(form, base);
    form.config = base.config;
};

const fillManageForm = (payload = {}) => {
    const base = blankPayload();
    Object.assign(base, payload || {});
    base.config = { ...blankPayload().config, ...(payload?.config || {}) };
    base.config.fuelType = normalizeFuelType(base.config.fuelType || '');
    base.factoryDate = toMonthValue(base.factoryDate);
    base.launchDate = toMonthValue(base.launchDate);
    base.routes = Array.isArray(payload?.routes) ? payload.routes.map(r => ({...r})) : [];
    Object.assign(manageForm, base);
    manageForm.config = base.config;
    manageForm.routes = base.routes;
};

const selectSubmission = (item) => {
    selected.value = item;
    fillForm(item.requestPayload || {});
    rejectOpen.value = false;
    rejectCode.value = '';
    rejectCustomReason.value = '';
};

const loadPending = async () => {
    loading.value = true;
    try {
        const list = await fetchPendingSubmissions();
        pendingList.value = Array.isArray(list) ? list : [];
        if (!pendingList.value.length) {
            selected.value = null;
            return;
        }
        const targetId = Number(route.query.submissionId);
        if (targetId) {
            const target = pendingList.value.find((item) => Number(item.id) === targetId);
            if (target) {
                selectSubmission(target);
                return;
            }
        }
        if (!selected.value || !pendingList.value.find((item) => item.id === selected.value.id)) {
            selectSubmission(pendingList.value[0]);
        }
    } catch (error) {
        ElMessage.error(error?.message || '加载待审核列表失败');
    } finally {
        loading.value = false;
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

const buildPayloadFrom = (sourceForm, extra = {}) => {
    const copy = JSON.parse(JSON.stringify(sourceForm));
    copy.vehicleId = extra.vehicleId || copy.vehicleId || null;
    copy.config.fuelType = normalizeFuelType(copy.config.fuelType || '');
    copy.factoryDate = normalizeMonthToDate(copy.factoryDate);
    copy.launchDate = normalizeMonthToDate(copy.launchDate);
    copy.imageIds = Array.isArray(copy.imageIds) ? copy.imageIds : [];
    if (!copy.imageIds.length && extra.imageId) copy.imageIds = [extra.imageId];

    const { provinceName, cityName } = splitProvinceCity(copy.regionId, regions.value);
    copy.regionProvince = copy.regionProvince || provinceName || null;
    copy.regionCity = copy.regionCity || cityName || null;
    return copy;
};

const validatePayload = (payload) => {
    if (!payload.plateNumber) return '请填写车牌号';
    if (!payload.modelId && !payload.modelName) return '请填写车型';
    if (!payload.companyId && !payload.companyName) return '请填写运营公司';
    if (!payload.regionId && !payload.regionCity) return '请填写地区信息';
    return '';
};

const stopSubmitProgressTimer = () => {
    if (submitProgressTimer) {
        clearInterval(submitProgressTimer);
        submitProgressTimer = null;
    }
};

const startSubmitProgress = (label) => {
    submitProgressLabel.value = label;
    submitProgressStatus.value = '';
    submitProgress.value = 10;
    stopSubmitProgressTimer();
    submitProgressTimer = setInterval(() => {
        if (submitProgress.value >= 90) return;
        submitProgress.value += Math.max(1, Math.ceil((92 - submitProgress.value) / 6));
    }, 150);
};

const finishSubmitProgress = (success) => {
    stopSubmitProgressTimer();
    submitProgress.value = 100;
    submitProgressStatus.value = success ? 'success' : 'exception';
    setTimeout(() => {
        submitProgress.value = 0;
        submitProgressStatus.value = '';
        submitProgressLabel.value = '';
    }, success ? 700 : 1400);
};

const handleApprove = async () => {
    if (!selected.value) return;
    const payload = buildPayloadFrom(form, {
        vehicleId: selected.value?.vehicleId || null,
        imageId: selected.value?.imageId || null
    });
    const message = validatePayload(payload);
    if (message) {
        ElMessage.warning(message);
        return;
    }
    submitting.value = true;
    startSubmitProgress('审核提交中');
    try {
        await approveSubmission(selected.value.id, payload);
        await Promise.all([
            store.dispatch('regions/loadRegions').catch(() => {}),
            store.dispatch('brands/loadBrands').catch(() => {}),
            store.dispatch('models/loadModels').catch(() => {}),
            store.dispatch('companies/loadCompanies').catch(() => {})
        ]);
        await loadPending();
        await loadManageList(true);
        finishSubmitProgress(true);
        ElMessage.success('审核通过，已入库');
    } catch (error) {
        finishSubmitProgress(false);
        ElMessage.error(error?.message || '审核通过失败');
    } finally {
        submitting.value = false;
    }
};

const handleReject = async () => {
    if (!selected.value) return;
    if (!rejectCode.value) {
        ElMessage.warning('请选择拒绝理由');
        return;
    }
    const reasonOption = rejectReasons.find((item) => item.code === rejectCode.value);
    const reason = rejectCode.value === 'OTHER' ? cleanText(rejectCustomReason.value) : reasonOption?.label;
    if (!reason) {
        ElMessage.warning('请填写拒绝原因');
        return;
    }

    submitting.value = true;
    startSubmitProgress('拒绝提交中');
    try {
        await rejectSubmission(selected.value.id, rejectCode.value, reason);
        await loadPending();
        finishSubmitProgress(true);
        ElMessage.success('已拒绝该上传申请');
    } catch (error) {
        finishSubmitProgress(false);
        ElMessage.error(error?.message || '拒绝失败');
    } finally {
        submitting.value = false;
    }
};

const resolveManageRegionId = () => {
    const profile = store.state.auth.profile || {};
    if (profile.role === 'STATION') {
        return manageFilters.regionId || null;
    }
    if (profile.role === 'REVIEWER') {
        return profile.reviewRegionId || null;
    }
    return null;
};

const manageRowKey = (row) => Number(row?.vehicle?.id || row?.id || 0);

const handleManageSelectionChange = (rows) => {
    selectedManageVehicleIds.value = (Array.isArray(rows) ? rows : [])
        .map((row) => manageRowKey(row))
        .filter((id) => id > 0);
};

const mergeManageRecords = (list, reset) => {
    const target = reset ? [] : [...manageRecords.value];
    const seen = new Set(target.map((item) => Number(item?.vehicle?.id || item?.id || 0)));
    (list || []).forEach((item) => {
        const id = Number(item?.vehicle?.id || item?.id || 0);
        if (!id || seen.has(id)) return;
        seen.add(id);
        target.push(item);
    });
    manageRecords.value = target;
};

const loadManageList = async (reset = true) => {
    if (manageLoading.value) return;
    manageLoading.value = true;
    try {
        const params = {
            size: 12,
            keyword: cleanText(manageFilters.keyword),
            regionId: resolveManageRegionId()
        };
        if (!reset) {
            params.lastLaunch = manageCursor.nextLaunch;
            params.lastId = manageCursor.nextId;
        }
        const data = await fetchManageVehiclePage(params);
        const list = Array.isArray(data?.records) ? data.records : [];
        mergeManageRecords(list, reset);
        if (reset) {
            selectedManageVehicleIds.value = [];
        }
        manageTotal.value = Number(data?.total || 0);
        manageCursor.nextLaunch = data?.nextLaunch || null;
        manageCursor.nextId = data?.nextId || null;
        manageHasMore.value = Boolean(data?.nextId);
    } catch (error) {
        if (reset) {
            manageRecords.value = [];
            manageTotal.value = 0;
            manageHasMore.value = false;
        }
        ElMessage.error(error?.message || '加载可管理车辆失败');
    } finally {
        manageLoading.value = false;
    }
};

const toEditableManagePayload = (detail) => {
    const vehicle = detail?.vehicle || {};
    const config = detail?.vehicleConfig || {};
    const images = Array.isArray(detail?.images) ? detail.images : [];
    const vehicleRoutes = Array.isArray(vehicle.routes) ? vehicle.routes : [];
    return {
        vehicleId: vehicle.id || null,
        plateNumber: vehicle.plateNumber || '',
        customNumber: vehicle.customNumber || '',
        brandId: vehicle.model?.brandId || config.brandId || null,
        brandName: vehicle.model?.brandName || config.brandName || '',
        modelId: vehicle.model?.id || config.modelId || null,
        modelName: vehicle.model?.name || config.modelName || '',
        companyId: vehicle.company?.id || null,
        companyName: vehicle.company?.name || '',
        regionId: vehicle.region?.id || null,
        regionProvince: '',
        regionCity: vehicle.region?.name || '',
        factoryDate: toMonthValue(vehicle.factoryDate),
        launchDate: toMonthValue(vehicle.launchDate),
        airConditioned: Boolean(vehicle.airConditioned),
        source: vehicle.source || '',
        routes: vehicleRoutes.map(vr => ({
            routeId: vr.routeId,
            routeNumber: vr.routeNumber || '',
            isCurrent: vr.isCurrent != null ? vr.isCurrent : true,
            remark: ''
        })),
        remark: vehicle.remark || '',
        imageIds: images.map((img) => img?.id).filter(Boolean),
        config: {
            brandId: config.brandId || vehicle.model?.brandId || null,
            modelId: config.modelId || vehicle.model?.id || null,
            motor: config.motor || '',
            engine: config.engine || '',
            fuelType: normalizeFuelType(config.fuelType || ''),
            stepType: config.stepType || '',
            transmissionSystem: config.transmissionSystem || '',
            suspension: config.suspension || '',
            axle: config.axle || '',
            otherConfigs: config.otherConfigs || ''
        }
    };
};

const openManageEdit = async (row) => {
    const id = row?.vehicle?.id;
    if (!id) return;
    manageEditingVehicleId.value = id;
    manageDialogVisible.value = true;
    manageEditLoading.value = true;
    try {
        store.dispatch('routes/loadRoutes', { size: 500 }).catch(() => {});
        const detail = await fetchVehicleGalleryDetail(id);
        fillManageForm(toEditableManagePayload(detail));
    } catch (error) {
        manageDialogVisible.value = false;
        ElMessage.error(error?.message || '加载车辆详情失败');
    } finally {
        manageEditLoading.value = false;
    }
};

const submitManageUpdate = async () => {
    if (!manageEditingVehicleId.value) return;
    const payload = buildPayloadFrom(manageForm, {
        vehicleId: manageEditingVehicleId.value
    });
    const message = validatePayload(payload);
    if (message) {
        ElMessage.warning(message);
        return;
    }
    manageSaving.value = true;
    try {
        const updatedDetail = await updateVehicle(manageEditingVehicleId.value, payload);
        store.commit('vehicles/SYNC_VEHICLE_DETAIL', updatedDetail);
        ElMessage.success('车辆信息已更新');
        manageDialogVisible.value = false;
        await loadManageList(true);
    } catch (error) {
        ElMessage.error(error?.message || '更新车辆失败');
    } finally {
        manageSaving.value = false;
    }
};

const confirmBatchDeleteVehicles = async () => {
    const ids = [...new Set(selectedManageVehicleIds.value)]
        .map((id) => Number(id || 0))
        .filter((id) => id > 0);
    if (!ids.length) {
        ElMessage.warning('请先勾选要删除的车辆');
        return;
    }
    try {
        await ElMessageBox.confirm(
            `将永久删除 ${ids.length} 辆车辆，是否继续？`,
            '二次确认',
            {
                type: 'warning',
                confirmButtonText: '确认删除',
                cancelButtonText: '取消',
                closeOnClickModal: true,
                closeOnPressEscape: true
            }
        );
    } catch (error) {
        return;
    }
    manageBatchDeleting.value = true;
    try {
        const result = await batchDeleteVehicles(ids);
        const deletedIds = Array.isArray(result?.deletedIds)
            ? result.deletedIds.map((id) => Number(id || 0))
            : [];
        const failedCount = Array.isArray(result?.failedIds) ? result.failedIds.length : 0;
        if (manageEditingVehicleId.value && deletedIds.includes(Number(manageEditingVehicleId.value))) {
            manageDialogVisible.value = false;
            manageEditingVehicleId.value = null;
        }
        await loadManageList(true);
        if (failedCount > 0) {
            ElMessage.warning(`批量删除完成：成功 ${deletedIds.length}，失败 ${failedCount}`);
            return;
        }
        ElMessage.success(`批量删除成功：共删除 ${deletedIds.length} 辆`);
    } catch (error) {
        ElMessage.error(error?.message || '批量删除失败');
    } finally {
        manageBatchDeleting.value = false;
    }
};

const confirmDeleteVehicle = async (row) => {
    const id = row?.vehicle?.id;
    if (!id) return;
    const plate = row?.vehicle?.plateNumber || '-';
    try {
        await ElMessageBox.confirm(
            `将删除车辆 #${id}（${plate}），删除后不可恢复。是否继续？`,
            '二次确认',
            {
                type: 'warning',
                confirmButtonText: '确认删除',
                cancelButtonText: '取消',
                closeOnClickModal: true,
                closeOnPressEscape: true
            }
        );
    } catch (error) {
        return;
    }
    try {
        await deleteVehicle(id);
        selectedManageVehicleIds.value = selectedManageVehicleIds.value.filter((item) => Number(item) !== Number(id));
        ElMessage.success('车辆已删除');
        if (manageEditingVehicleId.value === id) {
            manageDialogVisible.value = false;
            manageEditingVehicleId.value = null;
        }
        await loadManageList(true);
    } catch (error) {
        ElMessage.error(error?.message || '删除车辆失败');
    }
};

const findOptionIdByLabel = (options, label) => {
    const text = String(label || '').trim();
    if (!text) return null;
    const hit = (options || []).find((item) => String(item.label || '').trim() === text);
    return hit ? hit.value : null;
};

watch(() => form.brandId, (id) => {
    const hit = brandOptions.value.find((item) => item.value === id);
    if (hit) form.brandName = hit.label;
    form.config.brandId = id || null;
});
watch(
    () => form.brandName,
    (name) => {
        const matchedId = findOptionIdByLabel(brandOptions.value, name);
        if (form.brandId !== matchedId) {
            form.brandId = matchedId;
            form.config.brandId = matchedId;
        }
    }
);
watch(() => form.modelId, (id) => {
    const hit = modelOptions.value.find((item) => item.value === id);
    if (hit) form.modelName = hit.label;
    form.config.modelId = id || null;
});
watch(
    () => form.modelName,
    (name) => {
        const matchedId = findOptionIdByLabel(modelOptions.value, name);
        if (form.modelId !== matchedId) {
            form.modelId = matchedId;
            form.config.modelId = matchedId;
        }
    }
);
watch(() => form.companyId, (id) => {
    const hit = companyOptions.value.find((item) => item.value === id);
    if (hit) form.companyName = hit.label;
});
watch(
    () => form.companyName,
    (name) => {
        const matchedId = findOptionIdByLabel(companyOptions.value, name);
        if (form.companyId !== matchedId) {
            form.companyId = matchedId;
        }
    }
);
watch(() => manageForm.brandId, (id) => {
    const hit = brandOptions.value.find((item) => item.value === id);
    if (hit) manageForm.brandName = hit.label;
    manageForm.config.brandId = id || null;
});
watch(
    () => manageForm.brandName,
    (name) => {
        const matchedId = findOptionIdByLabel(brandOptions.value, name);
        if (manageForm.brandId !== matchedId) {
            manageForm.brandId = matchedId;
            manageForm.config.brandId = matchedId;
        }
    }
);
watch(() => manageForm.modelId, (id) => {
    const hit = modelOptions.value.find((item) => item.value === id);
    if (hit) manageForm.modelName = hit.label;
    manageForm.config.modelId = id || null;
});
watch(
    () => manageForm.modelName,
    (name) => {
        const matchedId = findOptionIdByLabel(modelOptions.value, name);
        if (manageForm.modelId !== matchedId) {
            manageForm.modelId = matchedId;
            manageForm.config.modelId = matchedId;
        }
    }
);
watch(() => manageForm.companyId, (id) => {
    const hit = companyOptions.value.find((item) => item.value === id);
    if (hit) manageForm.companyName = hit.label;
});
watch(
    () => manageForm.companyName,
    (name) => {
        const matchedId = findOptionIdByLabel(companyOptions.value, name);
        if (manageForm.companyId !== matchedId) {
            manageForm.companyId = matchedId;
        }
    }
);

onMounted(() => {
    if (store.getters['auth/hasToken']) store.dispatch('auth/fetchProfile').catch(() => {});
    store.dispatch('regions/loadRegions').catch(() => {});
    store.dispatch('brands/loadBrands').catch(() => {});
    store.dispatch('models/loadModels').catch(() => {});
    store.dispatch('companies/loadCompanies').catch(() => {});
    if (activePanel.value === 'manage') {
        loadManageList(true);
    } else {
        loadPending();
    }
});

watch(
    () => route.fullPath,
    () => {
        const panelQuery = route.query.panel;
        if (panelQuery === 'manage' || panelQuery === 'review') {
            activePanel.value = panelQuery;
        }
        if (activePanel.value === 'manage') {
            loadManageList(true);
        } else {
            loadPending();
        }
    }
);

onBeforeUnmount(() => {
    stopSubmitProgressTimer();
});
</script>

<style scoped lang="scss">
.review-page { min-height: 100vh; background: #f5f7fb; }
.review-main { width: min(1200px, 100%); margin: 0 auto; padding: 24px 16px 80px; }
.panel-switch { display: inline-flex; align-items: center; gap: 6px; padding: 4px; margin-bottom: 12px; border-radius: 12px; background: #e2e8f0; }
.switch-btn { border: none; background: transparent; color: #334155; padding: 8px 14px; border-radius: 9px; font-weight: 600; cursor: pointer; }
.switch-btn.active { background: #0f172a; color: #fff; box-shadow: 0 6px 18px rgba(15, 23, 42, .18); }
.panel { background: #fff; border-radius: 20px; padding: 20px; box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08); }
.panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 12px; }
.eyebrow { margin: 0 0 4px; letter-spacing: .2em; text-transform: uppercase; color: #94a3b8; font-size: .75rem; }
.muted { color: #64748b; margin: 6px 0 0; }
.layout { display: grid; grid-template-columns: 280px 1fr; gap: 14px; }
.list { border: 1px solid #e2e8f0; border-radius: 14px; padding: 10px; max-height: 80vh; overflow-y: auto; }
.row { width: 100%; margin-top: 8px; border: 1px solid #e2e8f0; border-radius: 10px; background: #f8fafc; padding: 8px; display: flex; flex-direction: column; gap: 2px; text-align: left; cursor: pointer; }
.row.active { border-color: #2563eb; background: #eff6ff; }
.detail { border: 1px solid #e2e8f0; border-radius: 14px; padding: 12px; }
.preview { width: 100%; border-radius: 12px; overflow: hidden; background: #0f172a; margin-bottom: 12px; padding: 10px 10px 8px; box-sizing: border-box; }
.preview :deep(.el-image) { width: 100%; display: block; cursor: zoom-in; }
.preview :deep(.el-image__inner) { width: 100%; height: auto; max-height: 70vh; object-fit: contain; display: block; }
.preview-tip { margin: 8px 0 0; font-size: .82rem; color: #cbd5e1; text-align: right; }
.actions { margin-top: 8px; display: flex; gap: 8px; }
.submit-progress { margin-top: 10px; padding: 10px 12px; border-radius: 10px; background: #f8fafc; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 8px; }
.submit-progress__label { color: #475569; font-size: .84rem; }
.reject-box { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.manage-panel-wrap { margin-top: 0; }
.manage-toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) minmax(220px, 320px) auto; gap: 10px; align-items: center; margin-bottom: 12px; }
.manage-region { width: 100%; }
.manage-actions { display: flex; align-items: center; gap: 8px; }
.manage-footer { margin-top: 12px; display: flex; justify-content: space-between; align-items: center; color: #64748b; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; }
.state { color: #94a3b8; padding: 20px 0; text-align: center; }
@media (max-width: 960px) {
    .layout { grid-template-columns: 1fr; }
    .preview :deep(.el-image__inner) { max-height: 52vh; }
    .manage-toolbar { grid-template-columns: 1fr; }
}
</style>
