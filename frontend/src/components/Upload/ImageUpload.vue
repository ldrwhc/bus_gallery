<template>
    <div class="upload-card">
        <h3>上传车辆图片并建档</h3>
        <el-alert
            v-if="!isAuthenticated"
            class="auth-alert"
            type="warning"
            :closable="false"
            title="需要登录"
            description="请先登录后再上传，记录会同步到“我的图片”。"
        />

        <el-form class="upload-form" label-position="top" :model="form">
            <div class="media-field">
                <p class="field-label">车辆图片</p>
                <div class="media-field__body">
                    <el-upload
                        class="upload-area"
                        drag
                        :auto-upload="false"
                        :limit="1"
                        :file-list="fileList"
                        :show-file-list="false"
                        :on-change="handleFileChange"
                        :disabled="!isAuthenticated"
                    >
                        <i class="el-icon-upload"></i>
                        <div class="el-upload__text">
                            将图片拖拽到这里，或 <em>点击选择</em>
                        </div>
                        <div class="el-upload__tip">仅支持 JPG / PNG，文件不超过 15 MB</div>
                    </el-upload>
                    <div v-if="previewUrl" class="preview-card" @click="previewVisible = true">
                        <img :src="previewUrl" alt="preview" decoding="async" />
                        <p>点击预览大图</p>
                    </div>
                </div>
            </div>

            <el-row :gutter="16">
                <el-col :md="12" :sm="24">
                    <el-form-item label="车牌号" required>
                        <el-input v-model="form.plateNumber" placeholder="例如：京A·12345" clearable />
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="自编号">
                        <el-input v-model="form.customNumber" placeholder="车辆自编号" clearable />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="12" :sm="24">
                    <el-form-item label="品牌">
                        <el-autocomplete
                            v-model="brandText"
                            placeholder="直接填写或搜索品牌名称"
                            :fetch-suggestions="queryBrandSuggestions"
                            @select="handleBrandSelect"
                            clearable
                            trigger-on-focus
                        />
                        <p v-if="brandHint" class="field-hint">{{ brandHint }}</p>
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="车型" required>
                        <el-autocomplete
                            v-model="modelText"
                            placeholder="直接填写或搜索车型名称"
                            :fetch-suggestions="queryModelSuggestions"
                            @select="handleModelSelect"
                            clearable
                            trigger-on-focus
                        />
                        <p v-if="modelHint" class="field-hint">{{ modelHint }}</p>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="12" :sm="24">
                    <el-form-item label="运营公司">
                        <el-autocomplete
                            v-model="companyText"
                            placeholder="直接填写或搜索公司名称"
                            :fetch-suggestions="queryCompanySuggestions"
                            @select="handleCompanySelect"
                            clearable
                            trigger-on-focus
                        />
                        <p v-if="companyHint" class="field-hint">{{ companyHint }}</p>
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="地区" required>
                        <div class="region-picker" ref="regionPickerRef">
                            <button type="button" class="region-picker__trigger" @click="toggleRegionPanel">
                                <span v-if="regionLabel">{{ regionLabel }}</span>
                                <span v-else class="region-picker__placeholder">
                                    {{ provinceOptions.length ? '请选择省份 / 城市' : '正在加载地区...' }}
                                </span>
                                <svg class="icon-chevron" viewBox="0 0 24 24">
                                    <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" />
                                </svg>
                            </button>
                            <transition name="fade">
                                <div v-if="regionPanelVisible" class="region-panel">
                                    <div class="region-panel__provinces">
                                        <button
                                            v-for="(province, index) in provinceOptions"
                                            :key="province.province"
                                            type="button"
                                            :class="['region-panel__province', { active: index === selectedProvinceIndex }]"
                                            @click="selectProvince(index)"
                                        >
                                            {{ province.province }}
                                        </button>
                                    </div>
                                    <div class="region-panel__cities">
                                        <template v-if="currentCities.length">
                                            <button
                                                v-for="city in currentCities"
                                                :key="city"
                                                type="button"
                                                :class="['region-panel__city', { active: city === selectedCityName }]"
                                                @click="selectCity(city)"
                                            >
                                                {{ city }}
                                            </button>
                                        </template>
                                        <p v-else class="region-panel__empty">暂无下级城市，可直接选择当前省份</p>
                                    </div>
                                </div>
                            </transition>
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="12" :sm="24">
                    <el-form-item label="出厂日期">
                        <el-date-picker
                            v-model="form.factoryDate"
                            type="month"
                            value-format="YYYY-MM"
                            placeholder="选择出厂年月"
                            style="width: 100%"
                        />
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="上线日期">
                        <el-date-picker
                            v-model="form.launchDate"
                            type="month"
                            value-format="YYYY-MM"
                            placeholder="选择上线年月"
                            style="width: 100%"
                        />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="8" :sm="24">
                    <el-form-item label="空调">
                        <el-switch v-model="form.airConditioned" active-text="有空调" inactive-text="无空调" />
                    </el-form-item>
                </el-col>
                <el-col :md="16" :sm="24">
                    <el-form-item label="燃料类型">
                        <el-select v-model="form.config.fuelType" placeholder="请选择燃料" clearable>
                            <el-option v-for="option in fuelOptions" :key="option.value" :label="option.label" :value="option.value" />
                        </el-select>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col v-if="showEngineField" :md="12" :sm="24">
                    <el-form-item label="发动机型号">
                        <el-input v-model="form.config.engine" placeholder="填写发动机型号" clearable />
                    </el-form-item>
                </el-col>
                <el-col :md="showEngineField ? 12 : 24" :sm="24">
                    <el-form-item label="变速系统">
                        <el-input v-model="form.config.transmissionSystem" placeholder="填写变速箱 / 传动系统型号" clearable />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="24" :sm="24">
                    <el-form-item label="悬挂类型">
                        <el-input
                            v-model="form.config.suspension"
                            placeholder="填写悬挂形式，例如：空气悬挂"
                            clearable
                        />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="24" :sm="24">
                    <el-form-item label="踏步">
                        <el-input
                            v-model="form.config.stepType"
                            placeholder="例如：低入口 / 二级踏步"
                            clearable
                        />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col v-if="showMotorField" :md="12" :sm="24">
                    <el-form-item label="电机型号">
                        <el-input v-model="form.config.motor" placeholder="填写电机型号" clearable />
                    </el-form-item>
                </el-col>
                <el-col :md="showMotorField ? 12 : 24" :sm="24">
                    <el-form-item label="车桥型号">
                        <el-input v-model="form.config.axle" placeholder="填写前/后桥型号" clearable />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :span="24">
                    <el-form-item label="关联线路">
                        <div style="display:flex;flex-direction:column;gap:6px;width:100%">
                            <div v-for="(ra, idx) in form.routes" :key="idx" style="display:flex;gap:6px;align-items:center">
                                <el-autocomplete
                                    v-model="ra.routeNumber"
                                    placeholder="输入或搜索线路号"
                                    :fetch-suggestions="(kw, cb) => queryUploadRouteSuggestions(kw, cb)"
                                    @select="(item) => selectUploadRoute(idx, item)"
                                    style="flex:1"
                                    size="small"
                                    clearable
                                />
                                <el-switch v-model="ra.isCurrent" active-text="当前" size="small" />
                                <el-button type="danger" size="small" text @click="form.routes.splice(idx,1)">×</el-button>
                            </div>
                            <el-button size="small" @click="form.routes.push({ routeId: null, routeNumber: '', isCurrent: true, remark: '' })">+ 关联已有线路</el-button>
                            <el-button size="small" type="primary" @click="openUploadRouteCreate">+ 创建新线路</el-button>
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>

            <RouteCreateDialog v-model="uploadRouteCreateVisible" :prefillRouteNumber="uploadRoutePrefillNumber"
                :prefillRegionId="form.regionId" :prefillCompanyId="form.companyId"
                @created="onUploadRouteCreated" />

            <div v-if="submitStage !== 'idle'" class="submit-progress">
                <div class="submit-progress__meta">
                    <span>{{ progressLabel }}</span>
                    <span>{{ uploadPercent }}%</span>
                </div>
                <el-progress :percentage="uploadPercent" :status="progressStatus" :stroke-width="14" />
                <p v-if="progressHint" class="submit-progress__hint">{{ progressHint }}</p>
            </div>

            <div class="actions">
                <el-button type="primary" :loading="loading" :disabled="loading || !isAuthenticated" @click="submit">
                    提交审核
                </el-button>
                <el-button :disabled="loading" @click="reset">重置</el-button>
            </div>
        </el-form>

        <el-dialog v-model="previewVisible" title="图片预览" width="60%">
            <img v-if="previewUrl" :src="previewUrl" class="preview-image" alt="preview" decoding="async" />
        </el-dialog>
    </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import { fetchRoutes } from '@/api/routes';
import RouteCreateDialog from '@/components/Route/RouteCreateDialog.vue';
import {
    cancelVehicleChunkUpload,
    completeVehicleChunkUpload,
    fetchVehicleChunkProgress,
    initVehicleChunkUpload,
    uploadVehicleChunkPart
} from '@/api/vehicles';
import { PROVINCE_CITY_DATA } from '@/utils/regionData';
import { FUEL_OPTIONS, isCombustionFuel, isElectricFuel, normalizeFuelType } from '@/utils/fuel';

const emit = defineEmits(['uploaded']);
const store = useStore();

const isAuthenticated = computed(() => store.getters['auth/isAuthenticated']);

const fuelOptions = FUEL_OPTIONS;

const initialConfig = () => ({
    motor: '',
    engine: '',
    fuelType: '',
    stepType: '',
    suspension: '',
    axle: '',
    transmissionSystem: '',
    otherConfigs: ''
});

const form = reactive({
    plateNumber: '',
    customNumber: '',
    brandId: null,
    modelId: null,
    companyId: null,
    regionId: null,
    factoryDate: '',
    launchDate: '',
    airConditioned: true,
    routes: [],
    config: initialConfig()
});

const brandText = ref('');
const modelText = ref('');
const companyText = ref('');

const brandOptions = computed(() => store.getters['brands/brandOptions'] || []);
const modelOptions = computed(() => store.getters['models/modelOptions'] || []);
const companyOptions = computed(() => store.getters['companies/companyOptions'] || []);
const uploadRouteOptions = computed(() => store.getters['routes/routeOptions'] || []);
const regionOptions = computed(() => store.getters['regions/regionOptions'] || []);

const regionsList = computed(() => store.state.regions.list || []);
const regionMap = computed(() => {
    const map = {};
    regionsList.value.forEach((region) => {
        if (region?.id != null) {
            map[region.id] = region;
        }
    });
    return map;
});

const normalizeCityName = (name = '') =>
    String(name || '')
        .replace(/(特别行政区|自治区|自治州|自治县|地区|盟|市|县|区)$/g, '')
        .trim();

const resolveRegionId = (cityName) => {
    if (!cityName || !regionOptions.value.length) return null;
    const normalizedCity = normalizeCityName(cityName);
    const match = regionOptions.value.find(
        (item) => normalizeCityName(item.label) === normalizedCity
    );
    return match ? match.value : null;
};

const selectedProvinceIndex = ref(0);
const selectedCityName = ref('');
const regionPanelVisible = ref(false);
const regionPickerRef = ref(null);

const provinceOptions = PROVINCE_CITY_DATA;
const selectedProvinceName = computed(() => provinceOptions[selectedProvinceIndex.value]?.province || '');
const currentCities = computed(() => provinceOptions[selectedProvinceIndex.value]?.cities || []);

const regionLabel = computed(() => {
    if (selectedProvinceName.value && selectedCityName.value) {
        return `${selectedProvinceName.value} / ${selectedCityName.value}`;
    }
    if (selectedProvinceName.value) {
        return selectedProvinceName.value;
    }
    return '';
});

const toggleRegionPanel = () => {
    if (!provinceOptions.length) return;
    regionPanelVisible.value = !regionPanelVisible.value;
};

const selectProvince = (index) => {
    selectedProvinceIndex.value = index;
    selectedCityName.value = '';
    form.regionId = null;
};

const selectCity = (city) => {
    if (!city) return;
    selectedCityName.value = city;
    form.regionId = resolveRegionId(city);
    regionPanelVisible.value = false;
};

const handleClickOutside = (event) => {
    if (!regionPanelVisible.value) return;
    if (regionPickerRef.value && !regionPickerRef.value.contains(event.target)) {
        regionPanelVisible.value = false;
    }
};

const matchOptionId = (text, options) => {
    if (!text?.trim() || !Array.isArray(options)) return null;
    const normalized = text.trim().toLowerCase();
    const exact = options.find((item) => (item.label || '').toLowerCase() === normalized);
    if (exact) return exact.value;
    const fuzzy = options.find((item) => (item.label || '').toLowerCase().includes(normalized));
    return fuzzy ? fuzzy.value : null;
};

const toSuggestion = (option) => ({
    value: option.label,
    id: option.value
});

const makeSuggestionFetcher = (optionsRef, regionFilter = false) => (queryString, cb) => {
    const keyword = (queryString || '').trim().toLowerCase();
    let list = optionsRef.value;
    if (regionFilter && form.regionId) {
        list = list.filter(o => o.regionId === form.regionId || o.regionId == null);
    }
    const results = list
        .filter((option) => !keyword || option.label.toLowerCase().includes(keyword))
        .slice(0, 8)
        .map(toSuggestion);
    cb(results);
};

const queryBrandSuggestions = makeSuggestionFetcher(brandOptions);
const queryModelSuggestions = makeSuggestionFetcher(modelOptions);
const queryCompanySuggestions = makeSuggestionFetcher(companyOptions, true);

const handleBrandSelect = (item = {}) => {
    form.brandId = item.id || null;
    brandText.value = item.value || '';
};

const handleModelSelect = (item = {}) => {
    form.modelId = item.id || null;
    modelText.value = item.value || '';
};

const handleCompanySelect = (item = {}) => {
    form.companyId = item.id || null;
    companyText.value = item.value || '';
};

watch(
    () => brandText.value,
    (val) => {
        form.brandId = matchOptionId(val, brandOptions.value);
    }
);

watch(
    () => modelText.value,
    (val) => {
        form.modelId = matchOptionId(val, modelOptions.value);
    }
);

watch(
    () => companyText.value,
    (val) => {
        form.companyId = matchOptionId(val, companyOptions.value);
    }
);

const brandHint = computed(() => {
    if (!brandText.value) return '';
    return form.brandId
        ? `已匹配品牌：${brandOptions.value.find((item) => item.value === form.brandId)?.label}`
        : '系统暂无该品牌，提交后将自动创建';
});

const modelHint = computed(() => {
    if (!modelText.value) return '';
    return form.modelId
        ? `已匹配车型：${modelOptions.value.find((item) => item.value === form.modelId)?.label}`
        : '系统暂无该车型，提交后将自动创建';
});

const uploadRouteCreateVisible = ref(false);
const uploadRoutePrefillNumber = ref('');
let uploadRouteAbort = null;

const queryUploadRouteSuggestions = (keyword, cb) => {
    if (uploadRouteAbort) uploadRouteAbort.abort();
    const controller = new AbortController();
    uploadRouteAbort = controller;
    const kw = (keyword || '').trim();
    const params = { keyword: kw, size: 8, isActive: true };
    if (form.regionId) params.regionId = form.regionId;
    if (form.companyId) params.companyId = form.companyId;
    fetchRoutes(params, controller.signal)
        .then(resp => {
            const items = (Array.isArray(resp?.records) ? resp.records : (Array.isArray(resp) ? resp : [])).map(r => {
                const regionName = r.region?.name || '';
                const display = regionName ? `${r.routeNumber} | ${regionName}` : r.routeNumber;
                return {
                    value: display, id: r.id, routeNumber: r.routeNumber,
                    startStop: r.startStop, endStop: r.endStop, companyName: r.companyName
                };
            });
            if (!items.length && kw) {
                items.push({ value: kw, id: '__new__', routeNumber: kw, __hint: true });
            }
            cb(items);
        })
        .catch(() => { if (controller.signal.aborted) return; cb([]); });
};

const selectUploadRoute = (idx, item) => {
    if (!item) return;
    if (item.id === '__new__') {
        uploadRoutePrefillNumber.value = item.routeNumber || '';
        uploadRouteCreateVisible.value = true;
        form.routes.splice(idx, 1);
        return;
    }
    form.routes[idx].routeId = item.id;
    form.routes[idx].routeNumber = item.routeNumber || item.value;
};

const onUploadRouteCreated = (saved) => {
    if (saved?.id) {
        form.routes.push({ routeId: saved.id, routeNumber: saved.routeNumber, isCurrent: true, remark: '' });
    }
};

const openUploadRouteCreate = () => {
    uploadRoutePrefillNumber.value = '';
    uploadRouteCreateVisible.value = true;
};

const companyHint = computed(() => {
    if (!companyText.value) return '';
    return form.companyId
        ? `已匹配公司：${companyOptions.value.find((item) => item.value === form.companyId)?.label}`
        : '系统暂无该公司，提交后将自动创建';
});

const showMotorField = computed(() => isElectricFuel(form.config.fuelType));
const showEngineField = computed(() => isCombustionFuel(form.config.fuelType));

const MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB
const DEFAULT_CHUNK_SIZE = 2 * 1024 * 1024; // 2MB
const fileList = ref([]);
const selectedFile = ref(null);
const previewUrl = ref('');
const previewVisible = ref(false);
let previewObjectUrl = '';
const loading = ref(false);
const submitStage = ref('idle');
const uploadPercent = ref(0);
const progressHint = ref('');
const activeUploadId = ref('');

const progressLabel = computed(() => {
    if (submitStage.value === 'uploading') return '图片上传中';
    if (submitStage.value === 'submitting') return '提交审核中';
    if (submitStage.value === 'success') return '提交完成';
    if (submitStage.value === 'error') return '提交失败';
    return '准备中';
});

const progressStatus = computed(() => {
    if (submitStage.value === 'success') return 'success';
    if (submitStage.value === 'error') return 'exception';
    return undefined;
});

const revokePreview = () => {
    if (previewObjectUrl) {
        URL.revokeObjectURL(previewObjectUrl);
        previewObjectUrl = '';
    }
};

const updatePreview = (file) => {
    revokePreview();
    if (!file) {
        previewUrl.value = '';
        return;
    }
    previewObjectUrl = URL.createObjectURL(file);
    previewUrl.value = previewObjectUrl;
};

const handleFileChange = (file, files) => {
    const latestFile = file.raw;
    if (latestFile?.size > MAX_FILE_SIZE) {
        ElMessage.warning('文件大小超过 15MB，请压缩后再上传');
        fileList.value = [];
        selectedFile.value = null;
        updatePreview(null);
        return;
    }

    fileList.value = files.slice(-1);
    selectedFile.value = latestFile;
    updatePreview(latestFile);
};

const normalizeMonth = (value) => (value ? `${value}-01` : null);

const reset = () => {
    form.plateNumber = '';
    form.customNumber = '';
    form.brandId = null;
    form.modelId = null;
    form.companyId = null;
    form.regionId = null;
    form.factoryDate = '';
    form.launchDate = '';
    form.airConditioned = true;
    Object.assign(form.config, initialConfig());

    brandText.value = '';
    modelText.value = '';
    companyText.value = '';

    selectedProvinceIndex.value = 0;
    selectedCityName.value = '';
    regionPanelVisible.value = false;

    fileList.value = [];
    selectedFile.value = null;
    updatePreview(null);
    previewVisible.value = false;
    submitStage.value = 'idle';
    uploadPercent.value = 0;
    progressHint.value = '';
    activeUploadId.value = '';
};

const buildConfigPayload = () => {
    const values = form.config || {};
    const hasValue = Object.values(values).some((item) => item && item.toString().trim().length);
    if (!hasValue && !form.brandId && !form.modelId) {
        return null;
    }
    return {
        brandId: form.brandId || null,
        modelId: form.modelId || null,
        motor: values.motor?.trim() || null,
        engine: values.engine?.trim() || null,
        fuelType: normalizeFuelType(values.fuelType) || null,
        stepType: values.stepType?.trim() || null,
        transmissionSystem: values.transmissionSystem?.trim() || null,
        suspension: values.suspension?.trim() || null,
        axle: values.axle?.trim() || null,
        otherConfigs: values.otherConfigs?.trim() || null
    };
};

const resolveRegionNames = () => {
    if (form.regionId) {
        const city = regionMap.value[form.regionId];
        if (city) {
            if (!city.parentId) {
                return { provinceName: city.name, cityName: null };
            }
            const province = regionMap.value[city.parentId];
            return {
                provinceName: province?.name || city.name,
                cityName: city.name
            };
        }
    }

    if (selectedProvinceName.value || selectedCityName.value) {
        return {
            provinceName: selectedProvinceName.value || selectedCityName.value || null,
            cityName: selectedCityName.value || selectedProvinceName.value || null
        };
    }

    return { provinceName: null, cityName: null };
};

const validate = () => {
    if (!isAuthenticated.value) {
        ElMessage.warning('请先登录再上传车辆信息');
        return false;
    }
    if (!selectedFile.value) {
        ElMessage.warning('请先选择需要上传的图片');
        return false;
    }
    if (!form.plateNumber?.trim()) {
        ElMessage.warning('请填写车牌号');
        return false;
    }
    if (!form.modelId && !modelText.value?.trim()) {
        ElMessage.warning('请填写或选择车型');
        return false;
    }
    if (!form.companyId && !companyText.value?.trim()) {
        ElMessage.warning('请填写或选择运营公司');
        return false;
    }
    if (!form.regionId && !selectedCityName.value) {
        ElMessage.warning('请选择车辆所属城市');
        return false;
    }
    return true;
};

const buildIdempotencyKey = () => {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const detectContentType = (file) => {
    const type = String(file?.type || '').toLowerCase();
    if (type === 'image/png') return 'image/png';
    if (type === 'image/jpeg' || type === 'image/jpg') return 'image/jpeg';
    const fileName = String(file?.name || '').toLowerCase();
    if (fileName.endsWith('.png')) return 'image/png';
    return 'image/jpeg';
};

const uploadFileChunks = async (file, uploadId, chunkSize, totalChunks) => {
    let uploadedBytes = 0;
    for (let index = 0; index < totalChunks; index += 1) {
        const start = index * chunkSize;
        const end = Math.min(file.size, start + chunkSize);
        const chunk = file.slice(start, end);
        progressHint.value = `正在上传分片 ${index + 1}/${totalChunks}`;
        await uploadVehicleChunkPart(uploadId, index, chunk, (event) => {
            const loaded = Math.min(event?.loaded || 0, chunk.size);
            const totalUploaded = uploadedBytes + loaded;
            uploadPercent.value = Math.min(90, Math.round((totalUploaded / file.size) * 90));
        });
        uploadedBytes += chunk.size;
        uploadPercent.value = Math.min(90, Math.round((uploadedBytes / file.size) * 90));
    }
};

const submit = async () => {
    if (!validate()) return;
    const { provinceName, cityName } = resolveRegionNames();
    const file = selectedFile.value;
    if (!file) return;
    try {
        loading.value = true;
        submitStage.value = 'uploading';
        uploadPercent.value = 0;
        progressHint.value = '初始化上传会话...';

        const payload = {
            plateNumber: form.plateNumber.trim(),
            customNumber: form.customNumber?.trim() || null,
            brandId: form.brandId || null,
            brandName: brandText.value?.trim() || null,
            modelId: form.modelId || null,
            modelName: modelText.value?.trim() || null,
            companyId: form.companyId || null,
            companyName: companyText.value?.trim() || null,
            regionId: form.regionId || null,
            regionProvince: provinceName,
            regionCity: cityName,
            factoryDate: normalizeMonth(form.factoryDate),
            launchDate: normalizeMonth(form.launchDate),
            airConditioned: form.airConditioned,
            source: '用户上传',
            remark: null,
            routes: (form.routes || []).filter(r => r.routeId || r.routeNumber).map(r => ({
                routeId: r.routeId || null,
                routeNumber: r.routeNumber || null,
                isCurrent: r.isCurrent != null ? r.isCurrent : true,
                remark: r.remark || ''
            })),
            config: buildConfigPayload()
        };

        const initResp = await initVehicleChunkUpload({
            fileName: file.name,
            contentType: detectContentType(file),
            totalSize: file.size,
            chunkSize: DEFAULT_CHUNK_SIZE,
            totalChunks: Math.ceil(file.size / DEFAULT_CHUNK_SIZE)
        });
        const uploadId = initResp?.uploadId;
        const chunkSize = Number(initResp?.chunkSize || DEFAULT_CHUNK_SIZE);
        const totalChunks = Number(initResp?.totalChunks || Math.ceil(file.size / chunkSize));
        if (!uploadId) {
            throw new Error('上传会话创建失败');
        }
        activeUploadId.value = uploadId;

        await uploadFileChunks(file, uploadId, chunkSize, totalChunks);

        const serverProgress = await fetchVehicleChunkProgress(uploadId);
        if (!serverProgress?.completed) {
            throw new Error('服务端尚未完成分片接收，请稍后重试');
        }

        submitStage.value = 'submitting';
        progressHint.value = '正在提交审核资料...';
        uploadPercent.value = 95;
        const result = await completeVehicleChunkUpload(uploadId, payload, buildIdempotencyKey());
        uploadPercent.value = 100;
        submitStage.value = 'success';
        progressHint.value = '资料已提交审核';
        if (result?.status === 'PENDING') {
            ElMessage.success('上传已提交，等待审核');
        } else {
            ElMessage.success('上传成功，车辆已建档');
        }
        emit('uploaded');
        reset();
    } catch (error) {
        submitStage.value = 'error';
        progressHint.value = error?.message || '上传失败，请稍后重试';
        if (activeUploadId.value) {
            await cancelVehicleChunkUpload(activeUploadId.value).catch(() => {});
        }
        ElMessage.error(error?.message || '上传失败，请稍后重试');
    } finally {
        loading.value = false;
        activeUploadId.value = '';
    }
};

onMounted(() => {
    document.addEventListener('click', handleClickOutside);
    store.dispatch('regions/loadRegions');
    store.dispatch('companies/loadCompanies');
    store.dispatch('routes/loadRoutes', { size: 500 }).catch(() => {});
    store.dispatch('brands/loadBrands');
    store.dispatch('models/loadModels');
});

onBeforeUnmount(() => {
    document.removeEventListener('click', handleClickOutside);
    revokePreview();
});
</script>

<style scoped lang="scss">
.upload-card {
    background: #fff;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);

    h3 {
        margin: 0 0 16px;
    }
}

.auth-alert {
    margin-bottom: 16px;
}

.upload-form {
    display: flex;
    flex-direction: column;
    gap: 8px;

    :deep(.el-form-item) {
        margin-bottom: 0;
    }

    :deep(.el-input),
    :deep(.el-select),
    :deep(.el-autocomplete),
    :deep(.el-date-editor),
    :deep(.el-textarea) {
        width: 100%;
    }
}

.media-field {
    margin-bottom: 24px;
}

.field-label {
    font-weight: 600;
    margin-bottom: 8px;
    color: #0f172a;
}

.media-field__body {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    align-items: flex-start;
}

.preview-card {
    width: 200px;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    background: #f8fafc;
    cursor: pointer;
    text-align: center;
    box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.2);

    img {
        width: 100%;
        height: 140px;
        object-fit: cover;
        border-radius: 12px;
        margin-bottom: 6px;
    }

    p {
        margin: 0;
        font-size: 0.85rem;
        color: #475569;
    }
}

.field-hint {
    margin: 4px 0 0;
    font-size: 12px;
    color: #6b7280;
}

.region-picker {
    position: relative;
    width: 100%;
}

.region-picker__trigger {
    width: 100%;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 10px 40px 10px 12px;
    font-size: 14px;
    text-align: left;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fff;
    cursor: pointer;
    transition: border-color 0.2s, box-shadow 0.2s;

    &:hover {
        border-color: #cbd5f5;
    }

    &:focus-visible {
        outline: none;
        border-color: #2563eb;
        box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
    }
}

.region-picker__placeholder {
    color: #94a3b8;
}

.icon-chevron {
    width: 18px;
    height: 18px;
    color: #94a3b8;
}

.region-panel {
    position: absolute;
    top: calc(100% + 8px);
    left: 0;
    width: 100%;
    max-height: 320px;
    display: flex;
    border-radius: 18px;
    background: #fff;
    color: #0f172a;
    border: 1px solid rgba(148, 163, 184, 0.4);
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.15);
    overflow: hidden;
    z-index: 30;
}

.region-panel__provinces {
    width: 45%;
    border-right: 1px solid rgba(148, 163, 184, 0.3);
    padding: 12px 0;
    overflow-y: auto;
}

.region-panel__province {
    width: 100%;
    text-align: left;
    padding: 10px 18px;
    background: none;
    border: none;
    color: inherit;
    font-size: 14px;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;

    &.active,
    &:hover {
        background: rgba(37, 99, 235, 0.08);
        color: #1d4ed8;
    }
}

.region-panel__cities {
    width: 55%;
    padding: 12px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    overflow-y: auto;
}

.region-panel__city {
    flex: 0 1 calc(50% - 8px);
    border: 1px solid rgba(148, 163, 184, 0.3);
    border-radius: 12px;
    background: #f8fafc;
    color: #0f172a;
    padding: 6px 12px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &.active,
    &:hover {
        background: #2563eb;
        border-color: #2563eb;
        color: #fff;
    }
}

.region-panel__empty {
    width: 100%;
    text-align: center;
    color: #94a3b8;
}

.preview-image {
    width: 100%;
    border-radius: 12px;
}

.submit-progress {
    margin-top: 12px;
    padding: 12px;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    background: #f8fafc;
}

.submit-progress__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    color: #334155;
    margin-bottom: 8px;
}

.submit-progress__hint {
    margin: 8px 0 0;
    font-size: 12px;
    color: #64748b;
}

.actions {
    margin-top: 12px;
    display: flex;
    gap: 12px;
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
