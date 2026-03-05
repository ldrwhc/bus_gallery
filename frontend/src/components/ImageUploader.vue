<template>
    <div class="upload-card">
        <h3>上传车辆图片并建档</h3>
        <form class="upload-form" @submit.prevent="submit">
            <!-- 车牌 / 自编号 -->
            <div class="form-row">
                <div class="form-item">
                    <label class="form-label" for="plateNumber">车牌号 <span class="required">*</span></label>
                    <input id="plateNumber" v-model="form.plateNumber" class="form-input" placeholder="例如：京A·12345" />
                </div>
                <div class="form-item">
                    <label class="form-label" for="customNumber">自编号</label>
                    <input id="customNumber" v-model="form.customNumber" class="form-input" placeholder="车辆自编号" />
                </div>
            </div>

            <!-- 品牌 / 车型（自定义联想组件） -->
            <div class="form-row">
                <!-- 品牌输入+联想 -->
                <div class="form-item">
                    <label class="form-label" for="brand">品牌</label>
                    <div class="autocomplete-wrapper" ref="brandWrapper">
                        <input id="brand" v-model="inputVal.brand" class="form-input" placeholder="输入品牌名称，支持模糊查找/自由填写"
                            @input="handleInput('brand')" @focus="handleFocus('brand')"
                            @blur="() => closeDropdown('brand')" />
                        <!-- 自定义联想下拉框 -->
                        <div v-if="dropdownShow.brand && dropdownList.brand.length" class="dropdown-list" @click.stop>
                            <div class="dropdown-item" v-for="item in dropdownList.brand" :key="item.id"
                                @click="selectItem('brand', item)">
                                {{ item.name }}
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 车型输入+联想 -->
                <div class="form-item">
                    <label class="form-label" for="model">车型</label>
                    <div class="autocomplete-wrapper" ref="modelWrapper">
                        <input id="model" v-model="inputVal.model" class="form-input" placeholder="输入车型名称，支持模糊查找/自由填写"
                            @input="handleInput('model')" @focus="handleFocus('model')"
                            @blur="() => closeDropdown('model')" />
                        <div v-if="dropdownShow.model && dropdownList.model.length" class="dropdown-list" @click.stop>
                            <div class="dropdown-item" v-for="item in dropdownList.model" :key="item.id"
                                @click="selectItem('model', item)">
                                {{ item.name }}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 地区 / 公司（自定义联想组件） -->
            <div class="form-row">
                <!-- 地区输入+联想 -->
                <div class="form-item">
                    <label class="form-label" for="region">地区 <span class="required">*</span></label>
                    <div class="autocomplete-wrapper" ref="regionWrapper">
                        <input id="region" v-model="inputVal.region" class="form-input" placeholder="输入城市名称，支持模糊查找/自由填写"
                            @input="handleInput('region')" @focus="handleFocus('region')"
                            @blur="() => closeDropdown('region')" />
                        <div v-if="dropdownShow.region && dropdownList.region.length" class="dropdown-list" @click.stop>
                            <div class="dropdown-item" v-for="item in dropdownList.region" :key="item.id"
                                @click="selectItem('region', item)">
                                {{ item.name }}
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 公司输入+联想 -->
                <div class="form-item">
                    <label class="form-label" for="company">公司 <span class="required">*</span></label>
                    <div class="autocomplete-wrapper" ref="companyWrapper">
                        <input id="company" v-model="inputVal.company" class="form-input"
                            placeholder="输入公司名称，支持模糊查找/自由填写" @input="handleInput('company')"
                            @focus="handleFocus('company')" @blur="() => closeDropdown('company')" />
                        <div v-if="dropdownShow.company && dropdownList.company.length" class="dropdown-list"
                            @click.stop>
                            <div class="dropdown-item" v-for="item in dropdownList.company" :key="item.id"
                                @click="selectItem('company', item)">
                                {{ item.name }}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 日期 / 空调 -->
            <div class="form-row">
                <div class="form-item">
                    <label class="form-label" for="factoryDate">出厂日期</label>
                    <input id="factoryDate" v-model="form.factoryDate" class="form-input" type="date" />
                </div>
                <div class="form-item">
                    <label class="form-label" for="launchDate">上线日期</label>
                    <input id="launchDate" v-model="form.launchDate" class="form-input" type="date" />
                </div>
            </div>

            <div class="form-row">
                <div class="form-item">
                    <label class="form-label">空调</label>
                    <div class="switch-wrapper">
                        <label class="switch">
                            <input type="checkbox" v-model="form.airConditioned" />
                            <span class="slider round"></span>
                        </label>
                        <span class="switch-text">{{ form.airConditioned ? '有空调' : '无空调' }}</span>
                    </div>
                </div>
            </div>

            <!-- 备注 -->
            <div class="form-item full-width">
                <label class="form-label" for="notes">备注</label>
                <textarea id="notes" v-model="form.notes" class="form-textarea" placeholder="可填写车辆配置、上线线路等备注"
                    rows="2"></textarea>
            </div>

            <!-- 图片上传 -->
            <div class="form-item full-width">
                <label class="form-label">图片 <span class="required">*</span></label>
                <div class="upload-area">
                    <input type="file" id="fileUpload" accept="image/*" @change="handleFileChange" class="file-input" />
                    <label for="fileUpload" class="upload-label">
                        <i class="upload-icon">📤</i>
                        <div class="upload-text">拖拽图片到此或 <em>点击选择图片</em></div>
                        <div class="upload-tip">仅支持单张上传，格式：jpg/png/webp</div>
                    </label>
                    <div v-if="selectedFileName" class="file-name">{{ selectedFileName }}</div>
                </div>
            </div>

            <!-- 操作按钮 -->
            <div class="form-actions">
                <button type="submit" class="btn primary-btn" :disabled="loading">
                    <span v-if="loading">处理中...</span>
                    <span v-else>上传并建档</span>
                </button>
                <button type="button" class="btn default-btn" :disabled="loading" @click="reset">
                    重置
                </button>
            </div>
        </form>
    </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus'; // 仅保留消息提示，可替换为原生alert
import { uploadImage } from '@/api/images';
import { createVehicle } from '@/api/vehicles';

// 事件派发
const emit = defineEmits(['uploaded']);
const store = useStore();

// 基础数据（从Vuex获取）
const regionList = computed(() => store.getters['regions/regionOptions'] || []);
const companyList = computed(() => store.getters['companies/companyOptions'] || []);
const brandList = computed(() => store.getters['brands/brandOptions'] || []);
const modelList = computed(() => store.getters['models/modelOptions'] || []);

// 表单核心数据
const form = reactive({
    plateNumber: '',
    customNumber: '',
    brandId: null,
    modelId: null,
    regionId: null,
    companyId: null,
    factoryDate: '',
    launchDate: '',
    airConditioned: true,
    notes: ''
});

// 输入框实时值（核心：纯文本，不会丢失）
const inputVal = reactive({
    brand: '',
    model: '',
    region: '',
    company: ''
});

// 自定义联想下拉状态
const dropdownShow = reactive({
    brand: false,
    model: false,
    region: false,
    company: false
});
const dropdownList = reactive({
    brand: [],
    model: [],
    region: [],
    company: []
});

// 图片上传相关
const selectedFile = ref(null);
const selectedFileName = ref('');
const loading = ref(false);

// 页面加载时获取基础数据
onMounted(() => {
    store.dispatch('regions/loadRegions');
    store.dispatch('companies/loadCompanies');
    store.dispatch('brands/loadBrands');
    store.dispatch('models/loadModels');
});

/**
 * 处理输入事件：模糊匹配联想列表
 * @param {String} type 字段类型（brand/model/region/company）
 */
const handleInput = (type) => {
    const keyword = inputVal[type].trim().toLowerCase();
    if (!keyword) {
        dropdownList[type] = [];
        dropdownShow[type] = false;
        return;
    }

    // 数据源映射
    const sourceMap = {
        brand: brandList.value,
        model: modelList.value,
        region: regionList.value,
        company: companyList.value
    };

    // 格式化数据并模糊匹配
    const source = sourceMap[type] || [];
    dropdownList[type] = source
        .map(item => ({
            id: item.id || item.value,
            name: item.name || item.label
        }))
        .filter(item => item.name?.toLowerCase().includes(keyword))
        .slice(0, 10); // 最多展示10条联想项

    dropdownShow[type] = dropdownList[type].length > 0;
};

/**
 * 输入框聚焦：触发联想（可选）
 * @param {String} type 字段类型
 */
const handleFocus = (type) => {
    if (inputVal[type].trim()) {
        handleInput(type); // 已有输入时触发联想
    }
};

/**
 * 关闭下拉框（延迟关闭，避免点击选项时立即隐藏）
 * @param {String} type 字段类型
 */
const closeDropdown = (type) => {
    setTimeout(() => {
        dropdownShow[type] = false;
    }, 200);
};

/**
 * 选择联想项：赋值ID并保持输入框文本
 * @param {String} type 字段类型
 * @param {Object} item 选中项
 */
const selectItem = (type, item) => {
    // 赋值对应ID
    form[`${type}Id`] = item.id;
    // 保持输入框文本（核心：解决值丢失）
    inputVal[type] = item.name;
    // 关闭下拉框
    dropdownShow[type] = false;
};

/**
 * 图片上传处理
 */
const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // 仅保留单张图片
    selectedFile.value = file;
    selectedFileName.value = file.name;

    // 清空input值，避免重复选择同一文件不触发change
    e.target.value = '';
};

/**
 * 表单重置
 */
const reset = () => {
    // 重置表单数据
    form.plateNumber = '';
    form.customNumber = '';
    form.brandId = null;
    form.modelId = null;
    form.regionId = null;
    form.companyId = null;
    form.factoryDate = '';
    form.launchDate = '';
    form.airConditioned = true;
    form.notes = '';

    // 重置输入框（核心：保留输入值不丢失）
    inputVal.brand = '';
    inputVal.model = '';
    inputVal.region = '';
    inputVal.company = '';

    // 重置下拉框
    Object.keys(dropdownShow).forEach(key => {
        dropdownShow[key] = false;
        dropdownList[key] = [];
    });

    // 重置图片
    selectedFile.value = null;
    selectedFileName.value = '';
};

/**
 * 表单校验
 */
const validate = () => {
    if (!selectedFile.value) {
        ElMessage?.warning('请先选择要上传的图片') || alert('请先选择要上传的图片');
        return false;
    }
    if (!form.plateNumber.trim()) {
        ElMessage?.warning('请填写车牌号') || alert('请填写车牌号');
        return false;
    }
    if (!inputVal.region.trim()) {
        ElMessage?.warning('请填写地区') || alert('请填写地区');
        return false;
    }
    if (!inputVal.company.trim()) {
        ElMessage?.warning('请填写公司') || alert('请填写公司');
        return false;
    }
    return true;
};

/**
 * 提交表单
 */
const submit = async () => {
    if (!validate()) return;

    try {
        loading.value = true;

        // 1. 上传图片
        const formData = new FormData();
        formData.append('file', selectedFile.value);
        formData.append('title', form.plateNumber.trim());
        formData.append('description', form.notes.trim());
        const uploadedImage = await uploadImage(formData);
        const imageId = uploadedImage?.id || uploadedImage?.data?.id;

        if (!imageId) {
            throw new Error('上传成功但未返回图片ID');
        }

        // 2. 构造提交参数
        const payload = {
            plateNumber: form.plateNumber.trim(),
            customNumber: form.customNumber.trim() || null,
            brandId: form.brandId,
            brandName: inputVal.brand.trim() || null,
            modelId: form.modelId,
            modelName: inputVal.model.trim() || null,
            regionId: form.regionId,
            regionName: inputVal.region.trim() || null,
            companyId: form.companyId,
            companyName: inputVal.company.trim() || null,
            factoryDate: form.factoryDate || null,
            launchDate: form.launchDate || null,
            airConditioned: form.airConditioned,
            remark: form.notes.trim() || null,
            source: '用户上传',
            imageIds: [imageId]
        };

        // 3. 创建车辆档案
        await createVehicle(payload);

        ElMessage?.success('车辆档案创建成功') || alert('车辆档案创建成功');
        emit('uploaded');
        reset();
    } catch (error) {
        ElMessage?.error(error.message || '上传失败，请稍后重试') || alert(error.message || '上传失败');
    } finally {
        loading.value = false;
    }
};
</script>

<style scoped lang="scss">
/* 全局样式重置 */
.upload-card {
    background: #fff;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    max-width: 800px;
    margin: 0 auto;

    h3 {
        margin: 0 0 24px;
        font-size: 18px;
        font-weight: 600;
        color: #333;
    }
}

.upload-form {
    width: 100%;
}

/* 表单行 */
.form-row {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 16px;

    @media (max-width: 768px) {
        flex-direction: column;
        gap: 12px;
    }
}

/* 表单项 */
.form-item {
    flex: 1;
    min-width: 280px;

    &.full-width {
        flex: 1 1 100%;
        min-width: 100%;
    }

    .form-label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        color: #666;

        .required {
            color: #f56c6c;
            margin-left: 4px;
        }
    }

    .form-input {
        width: 100%;
        padding: 10px 12px;
        border: 1px solid #e6e6e6;
        border-radius: 6px;
        font-size: 14px;
        transition: border-color 0.2s;

        &:focus {
            outline: none;
            border-color: #409eff;
            box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
        }
    }

    .form-textarea {
        width: 100%;
        padding: 10px 12px;
        border: 1px solid #e6e6e6;
        border-radius: 6px;
        font-size: 14px;
        resize: vertical;
        transition: border-color 0.2s;

        &:focus {
            outline: none;
            border-color: #409eff;
            box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
        }
    }
}

/* 自定义联想组件样式 */
.autocomplete-wrapper {
    position: relative;
    width: 100%;

    .dropdown-list {
        position: absolute;
        top: calc(100% + 4px);
        left: 0;
        right: 0;
        background: #fff;
        border: 1px solid #e6e6e6;
        border-radius: 6px;
        max-height: 200px;
        overflow-y: auto;
        z-index: 999;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

        .dropdown-item {
            padding: 10px 12px;
            font-size: 14px;
            color: #333;
            cursor: pointer;

            &:hover {
                background-color: #f5f7fa;
            }

            &:active {
                background-color: #e8f4ff;
            }
        }
    }
}

/* 开关样式 */
.switch-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;

    .switch {
        position: relative;
        display: inline-block;
        width: 40px;
        height: 20px;

        input {
            opacity: 0;
            width: 0;
            height: 0;
        }

        .slider {
            position: absolute;
            cursor: pointer;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: #ccc;
            transition: .4s;
            border-radius: 20px;

            &:before {
                position: absolute;
                content: "";
                height: 16px;
                width: 16px;
                left: 2px;
                bottom: 2px;
                background-color: white;
                transition: .4s;
                border-radius: 50%;
            }
        }

        input:checked+.slider {
            background-color: #409eff;
        }

        input:checked+.slider:before {
            transform: translateX(20px);
        }
    }

    .switch-text {
        font-size: 14px;
        color: #666;
    }
}

/* 上传区域样式 */
.upload-area {
    position: relative;
    width: 100%;
    height: 120px;
    border: 1px dashed #e6e6e6;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    cursor: pointer;
    transition: border-color 0.2s;

    &:hover {
        border-color: #409eff;
    }

    .file-input {
        display: none;
    }

    .upload-label {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        cursor: pointer;
        color: #999;

        .upload-icon {
            font-size: 24px;
            margin-bottom: 8px;
        }

        .upload-text {
            font-size: 14px;
            margin-bottom: 4px;

            em {
                color: #409eff;
                font-style: normal;
            }
        }

        .upload-tip {
            font-size: 12px;
            color: #ccc;
        }
    }

    .file-name {
        position: absolute;
        bottom: 8px;
        left: 12px;
        font-size: 12px;
        color: #666;
        max-width: calc(100% - 24px);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
}

/* 按钮样式 */
.form-actions {
    margin-top: 24px;
    display: flex;
    gap: 12px;
}

.btn {
    padding: 10px 20px;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    border: none;

    &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
    }

    &.primary-btn {
        background-color: #409eff;
        color: #fff;

        &:not(:disabled):hover {
            background-color: #66b1ff;
        }

        &:not(:disabled):active {
            background-color: #3399ff;
        }
    }

    &.default-btn {
        background-color: #f5f7fa;
        color: #666;
        border: 1px solid #e6e6e6;

        &:not(:disabled):hover {
            background-color: #e8f4ff;
            border-color: #d9e9fc;
        }
    }
}
</style>