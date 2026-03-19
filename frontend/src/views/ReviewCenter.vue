<template>
    <div class="review-page">
        <main class="review-main">
            <section class="panel">
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
                                        <el-form-item label="品牌（中文）">
                                            <el-select v-model="form.brandId" clearable filterable placeholder="可搜索品牌">
                                                <el-option v-for="option in brandOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="品牌名称"><el-input v-model="form.brandName" /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="车型（中文）">
                                            <el-select v-model="form.modelId" clearable filterable placeholder="可搜索车型">
                                                <el-option v-for="option in modelOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="车型名称（必填）" required><el-input v-model="form.modelName" /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="运营公司（中文）">
                                            <el-select v-model="form.companyId" clearable filterable placeholder="可搜索公司">
                                                <el-option v-for="option in companyOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="运营公司名称（必填）" required><el-input v-model="form.companyName" /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="地区（中文）">
                                            <el-select v-model="form.regionId" clearable filterable placeholder="可搜索地区">
                                                <el-option v-for="option in regionOptions" :key="option.value" :label="option.label" :value="option.value" />
                                            </el-select>
                                        </el-form-item>
                                    </el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="地区显示"><el-input :model-value="regionDisplay" disabled /></el-form-item></el-col>
                                </el-row>

                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24"><el-form-item label="出厂日期"><el-date-picker v-model="form.factoryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="上线日期"><el-date-picker v-model="form.launchDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
                                </el-row>
                                <el-form-item label="空调"><el-switch v-model="form.airConditioned" /></el-form-item>
                                <el-form-item label="来源"><el-input v-model="form.source" /></el-form-item>
                                <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>

                                <h3>配置信息</h3>
                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24"><el-form-item label="电机"><el-input v-model="form.config.motor" /></el-form-item></el-col>
                                    <el-col :sm="12" :xs="24"><el-form-item label="发动机"><el-input v-model="form.config.engine" /></el-form-item></el-col>
                                </el-row>
                                <el-row :gutter="12">
                                    <el-col :sm="12" :xs="24">
                                        <el-form-item label="燃料类型（中文）">
                                            <el-select v-model="form.config.fuelType" clearable filterable allow-create default-first-option placeholder="请选择或输入">
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
        </main>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import { approveSubmission, fetchPendingSubmissions, rejectSubmission } from '@/api/reviews';
import { FUEL_OPTIONS, normalizeFuelType } from '@/utils/fuel';

const store = useStore();
const route = useRoute();

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

const fuelOptions = FUEL_OPTIONS;
const brandOptions = computed(() => store.getters['brands/brandOptions'] || []);
const modelOptions = computed(() => store.getters['models/modelOptions'] || []);
const companyOptions = computed(() => store.getters['companies/companyOptions'] || []);
const regionOptions = computed(() => (store.state.regions.list || []).map((item) => ({ value: item.id, label: item.name, parentId: item.parentId })));
const regionMap = computed(() => regionOptions.value.reduce((acc, item) => ({ ...acc, [item.value]: item }), {}));
const previewImageList = computed(() => {
    const imageUrl = selected.value?.imageUrl;
    const thumbnailUrl = selected.value?.imageThumbnailUrl;
    if (imageUrl) return [imageUrl];
    if (thumbnailUrl) return [thumbnailUrl];
    return [];
});

const regionDisplay = computed(() => {
    const city = regionMap.value[form.regionId];
    if (!city) return '-';
    const parent = city.parentId ? regionMap.value[city.parentId] : null;
    return parent ? `${parent.label} / ${city.label}` : city.label;
});
const currentReviewRegionLabel = computed(() => {
    const profile = store.state.auth.profile || {};
    if (profile.role === 'STATION') return '全部地区（站长）';
    if (profile.role !== 'REVIEWER') return '未登录审核身份';
    const reviewRegionId = profile.reviewRegionId;
    if (!reviewRegionId) return '未分配';
    const region = regionMap.value[reviewRegionId];
    if (!region) return `ID ${reviewRegionId}`;
    const parent = region.parentId ? regionMap.value[region.parentId] : null;
    return parent ? `${parent.label} / ${region.label}` : region.label;
});

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
    }
});

const form = reactive(blankPayload());

const fillForm = (payload = {}) => {
    const base = blankPayload();
    Object.assign(base, payload || {});
    base.config = { ...blankPayload().config, ...(payload?.config || {}) };
    base.config.fuelType = normalizeFuelType(base.config.fuelType || '');
    Object.assign(form, base);
    form.config = base.config;
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

const buildPayload = () => {
    const copy = JSON.parse(JSON.stringify(form));
    copy.vehicleId = selected.value?.vehicleId || copy.vehicleId || null;
    copy.config.fuelType = normalizeFuelType(copy.config.fuelType || '');
    copy.imageIds = Array.isArray(copy.imageIds) ? copy.imageIds : [];
    if (!copy.imageIds.length && selected.value?.imageId) copy.imageIds = [selected.value.imageId];

    const city = regionMap.value[copy.regionId];
    const province = city?.parentId ? regionMap.value[city.parentId] : null;
    copy.regionProvince = copy.regionProvince || province?.label || city?.label || null;
    copy.regionCity = copy.regionCity || city?.label || null;
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
    const payload = buildPayload();
    const message = validatePayload(payload);
    if (message) {
        ElMessage.warning(message);
        return;
    }
    submitting.value = true;
    startSubmitProgress('审核提交中');
    try {
        await approveSubmission(selected.value.id, payload);
        await loadPending();
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

watch(() => form.brandId, (id) => {
    const hit = brandOptions.value.find((item) => item.value === id);
    if (hit) form.brandName = hit.label;
    form.config.brandId = id || null;
});
watch(() => form.modelId, (id) => {
    const hit = modelOptions.value.find((item) => item.value === id);
    if (hit) form.modelName = hit.label;
    form.config.modelId = id || null;
});
watch(() => form.companyId, (id) => {
    const hit = companyOptions.value.find((item) => item.value === id);
    if (hit) form.companyName = hit.label;
});

onMounted(() => {
    if (store.getters['auth/hasToken']) store.dispatch('auth/fetchProfile').catch(() => {});
    store.dispatch('regions/loadRegions').catch(() => {});
    store.dispatch('brands/loadBrands').catch(() => {});
    store.dispatch('models/loadModels').catch(() => {});
    store.dispatch('companies/loadCompanies').catch(() => {});
    loadPending();
});

watch(
    () => route.fullPath,
    () => {
        loadPending();
    }
);

onBeforeUnmount(() => {
    stopSubmitProgressTimer();
});
</script>

<style scoped lang="scss">
.review-page { min-height: 100vh; background: #f5f7fb; }
.review-main { width: min(1200px, 100%); margin: 0 auto; padding: 24px 16px 80px; }
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
.state { color: #94a3b8; padding: 20px 0; text-align: center; }
@media (max-width: 960px) {
    .layout { grid-template-columns: 1fr; }
    .preview :deep(.el-image__inner) { max-height: 52vh; }
}
</style>
