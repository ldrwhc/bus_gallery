<template>
    <div class="upload-card">
        <h3>上传车辆图片并建档</h3>
        <el-form class="upload-form" label-width="90px" :model="form">
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
                        <el-input v-model="brandText" placeholder="直接填写品牌名称" clearable />
                        <p v-if="brandHint" class="field-hint">{{ brandHint }}</p>
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="车型" required>
                        <el-input v-model="modelText" placeholder="直接填写车型名称" clearable />
                        <p v-if="modelHint" class="field-hint">{{ modelHint }}</p>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="12" :sm="24">
                    <el-form-item label="公司">
                        <el-input v-model="companyText" placeholder="直接填写公司名称" clearable />
                        <p v-if="companyHint" class="field-hint">{{ companyHint }}</p>
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="地区" required>
                        <div class="region-picker" ref="regionPickerRef">
                            <button type="button" class="region-picker__trigger" @click="toggleRegionPanel">
                                <span v-if="regionLabel">{{ regionLabel }}</span>
                                <span v-else class="region-picker__placeholder">请选择省份 / 城市</span>
                                <svg class="icon-chevron" viewBox="0 0 24 24">
                                    <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" fill="none"
                                        stroke-linecap="round" stroke-linejoin="round" />
                                </svg>
                            </button>
                            <transition name="fade">
                                <div v-if="regionPanelVisible" class="region-panel">
                                    <div class="region-panel__provinces">
                                        <button v-for="(province, index) in PROVINCE_CITY_DATA" :key="province.province"
                                            type="button"
                                            :class="['region-panel__province', { active: index === selectedProvinceIndex }]"
                                            @click="selectProvince(index)">
                                            {{ province.province }}
                                        </button>
                                    </div>
                                    <div class="region-panel__cities">
                                        <button v-for="city in currentCities" :key="city" type="button" :class="[
                                            'region-panel__city',
                                            { active: city === selectedCity && provinceMatchesSelectedCity }
                                        ]" @click="selectCity(city)">
                                            {{ city }}
                                        </button>
                                        <p v-if="!currentCities.length" class="region-panel__empty">暂无城市数据</p>
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
                        <el-date-picker v-model="form.factoryDate" type="date" value-format="YYYY-MM-DD"
                            placeholder="选择出厂日期" style="width: 100%" />
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="上线日期">
                        <el-date-picker v-model="form.launchDate" type="date" value-format="YYYY-MM-DD"
                            placeholder="选择上线日期" style="width: 100%" />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16">
                <el-col :md="12" :sm="24">
                    <el-form-item label="空调">
                        <el-switch v-model="form.airConditioned" active-text="有空调" inactive-text="无空调" />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16" v-if="showConfigPanel">
                <el-col :md="12" :sm="24">
                    <el-form-item label="发动机型号">
                        <el-input v-model="form.config.engine" placeholder="填写发动机型号" clearable />
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="燃料类型">
                        <el-select v-model="form.config.fuelType" placeholder="选择燃料类型" clearable>
                            <el-option label="压缩天然气" value="cng" />
                            <el-option label="液化天然气" value="lng" />
                            <el-option label="柴油" value="diesel" />
                            <el-option label="电" value="electric" />
                            <el-option label="柴油,电" value="diesel_electric" />
                            <el-option label="压缩天然气,电" value="cng_electric" />
                            <el-option label="液化天然气,电" value="lng_electric" />
                        </el-select>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row v-if="showConfigPanel">
                <el-col :span="24">
                    <el-form-item label="悬挂类型">
                        <el-input v-model="form.config.suspension" placeholder="填写悬挂类型" clearable />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row v-if="showConfigPanel">
                <el-col :span="24">
                    <el-form-item label="踏步">
                        <el-input v-model="form.config.stepType" placeholder="例如：低入口 / 二级踏步" type="textarea"
                            :rows="2" />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="16" v-if="showConfigPanel">
                <el-col :md="12" :sm="24">
                    <el-form-item label="电机型号">
                        <el-input v-model="form.config.motor" placeholder="填写电机/驱动电机型号" clearable />
                    </el-form-item>
                </el-col>
                <el-col :md="12" :sm="24">
                    <el-form-item label="车桥型号">
                        <el-input v-model="form.config.axle" placeholder="填写车桥型号" clearable />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-button type="text" @click="toggleConfigPanel" v-if="!showConfigPanel">
                展开车辆配置填写
            </el-button>
            <el-button type="text" @click="toggleConfigPanel" v-else>
                收起车辆配置填写
            </el-button>

            <el-form-item label="备注">
                <el-input v-model="form.notes" placeholder="可填写车辆配置、上线线路等备注" type="textarea" :rows="2" />
            </el-form-item>

            <el-form-item label="图片" required>
                <el-upload class="upload-area" drag :auto-upload="false" :limit="1" :file-list="fileList"
                    :on-change="handleFileChange">
                    <i class="el-icon-upload"></i>
                    <div class="el-upload__text">
                        拖拽图片到此或 <em>点击选择图片</em>
                    </div>
                    <div class="el-upload__tip">仅支持单张上传，类型需符合后端限制</div>
                </el-upload>
            </el-form-item>

            <div class="actions">
                <el-button type="primary" :loading="loading" :disabled="loading" @click="submit">
                    上传并建档
                </el-button>
                <el-button :disabled="loading" @click="reset">重置</el-button>
            </div>
        </el-form>
    </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import { uploadVehicleWithImage } from '@/api/vehicles';

const PROVINCE_CITY_DATA = [
    { province: '北京市', cities: ['北京市'] },
    { province: '天津市', cities: ['天津市'] },
    { province: '上海市', cities: ['上海市'] },
    { province: '重庆市', cities: ['重庆市'] },
    {
        province: '河北省',
        cities: ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '邢台市', '保定市', '张家口市', '承德市', '沧州市', '廊坊市', '衡水市']
    },
    {
        province: '山西省',
        cities: ['太原市', '大同市', '阳泉市', '长治市', '晋城市', '朔州市', '晋中市', '运城市', '忻州市', '临汾市', '吕梁市']
    },
    {
        province: '内蒙古自治区',
        cities: ['呼和浩特市', '包头市', '乌海市', '赤峰市', '通辽市', '鄂尔多斯市', '呼伦贝尔市', '巴彦淖尔市', '乌兰察布市', '兴安盟', '锡林郭勒盟', '阿拉善盟']
    },
    {
        province: '辽宁省',
        cities: ['沈阳市', '大连市', '鞍山市', '抚顺市', '本溪市', '丹东市', '锦州市', '营口市', '阜新市', '辽阳市', '盘锦市', '铁岭市', '朝阳市', '葫芦岛市']
    },
    {
        province: '吉林省',
        cities: ['长春市', '吉林市', '四平市', '辽源市', '通化市', '白山市', '松原市', '白城市', '延边朝鲜族自治州']
    },
    {
        province: '黑龙江省',
        cities: ['哈尔滨市', '齐齐哈尔市', '牡丹江市', '佳木斯市', '大庆市', '伊春市', '鸡西市', '鹤岗市', '双鸭山市', '七台河市', '绥化市', '黑河市', '大兴安岭地区']
    },
    {
        province: '江苏省',
        cities: ['南京市', '无锡市', '徐州市', '常州市', '苏州市', '南通市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市']
    },
    {
        province: '浙江省',
        cities: ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市']
    },
    {
        province: '安徽省',
        cities: ['合肥市', '芜湖市', '蚌埠市', '淮南市', '马鞍山市', '淮北市', '铜陵市', '安庆市', '黄山市', '滁州市', '阜阳市', '宿州市', '巢湖市', '六安市', '亳州市', '池州市', '宣城市']
    },
    {
        province: '福建省',
        cities: ['福州市', '厦门市', '莆田市', '三明市', '泉州市', '漳州市', '南平市', '龙岩市', '宁德市']
    },
    {
        province: '江西省',
        cities: ['南昌市', '景德镇市', '萍乡市', '九江市', '新余市', '鹰潭市', '赣州市', '吉安市', '宜春市', '抚州市', '上饶市']
    },
    {
        province: '山东省',
        cities: ['济南市', '青岛市', '淄博市', '枣庄市', '东营市', '烟台市', '潍坊市', '济宁市', '泰安市', '威海市', '日照市', '莱芜市', '临沂市', '德州市', '聊城市', '滨州市', '菏泽市']
    },
    {
        province: '河南省',
        cities: ['郑州市', '开封市', '洛阳市', '平顶山市', '安阳市', '鹤壁市', '新乡市', '焦作市', '濮阳市', '许昌市', '漯河市', '三门峡市', '南阳市', '商丘市', '信阳市', '周口市', '驻马店市', '济源市']
    },
    {
        province: '湖北省',
        cities: ['武汉市', '黄石市', '十堰市', '宜昌市', '襄阳市', '鄂州市', '荆门市', '孝感市', '荆州市', '黄冈市', '咸宁市', '随州市', '恩施土家族苗族自治州', '仙桃市', '潜江市', '天门市', '神农架林区']
    },
    {
        province: '湖南省',
        cities: ['长沙市', '株洲市', '湘潭市', '衡阳市', '邵阳市', '岳阳市', '常德市', '张家界市', '益阳市', '郴州市', '永州市', '怀化市', '娄底市', '湘西土家族苗族自治州']
    },
    {
        province: '广东省',
        cities: ['广州市', '深圳市', '珠海市', '汕头市', '佛山市', '江门市', '湛江市', '茂名市', '肇庆市', '惠州市', '梅州市', '汕尾市', '河源市', '阳江市', '清远市', '东莞市', '中山市', '潮州市', '揭阳市', '云浮市']
    },
    {
        province: '广西壮族自治区',
        cities: ['南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市', '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市']
    },
    {
        province: '海南省',
        cities: ['海口市', '三亚市', '三沙市', '儋州市', '琼海市', '文昌市', '万宁市', '东方市']
    },
    {
        province: '四川省',
        cities: ['成都市', '自贡市', '攀枝花市', '泸州市', '德阳市', '绵阳市', '广元市', '遂宁市', '内江市', '乐山市', '南充市', '眉山市', '宜宾市', '广安市', '达州市', '雅安市', '巴中市', '资阳市', '阿坝藏族羌族自治州', '甘孜藏族自治州', '凉山彝族自治州']
    },
    {
        province: '贵州省',
        cities: ['贵阳市', '六盘水市', '遵义市', '安顺市', '毕节市', '铜仁市', '黔西南布依族苗族自治州', '黔东南苗族侗族自治州', '黔南布依族苗族自治州']
    },
    {
        province: '云南省',
        cities: ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄彝族自治州', '红河哈尼族彝族自治州', '文山壮族苗族自治州', '西双版纳傣族自治州', '大理白族自治州', '德宏傣族景颇族自治州', '怒江傈僳族自治州', '迪庆藏族自治州']
    },
    {
        province: '西藏自治区',
        cities: ['拉萨市', '日喀则市', '昌都市', '林芝市', '山南市', '那曲市', '阿里地区']
    },
    {
        province: '陕西省',
        cities: ['西安市', '铜川市', '宝鸡市', '咸阳市', '渭南市', '延安市', '汉中市', '榆林市', '安康市', '商洛市']
    },
    {
        province: '甘肃省',
        cities: ['兰州市', '嘉峪关市', '金昌市', '白银市', '天水市', '武威市', '张掖市', '平凉市', '酒泉市', '庆阳市', '定西市', '陇南市', '临夏回族自治州', '甘南藏族自治州']
    },
    {
        province: '青海省',
        cities: ['西宁市', '海东市', '海北藏族自治州', '黄南藏族自治州', '海南藏族自治州', '果洛藏族自治州', '玉树藏族自治州', '海西蒙古族藏族自治州']
    },
    {
        province: '宁夏回族自治区',
        cities: ['银川市', '石嘴山市', '吴忠市', '固原市', '中卫市']
    },
    {
        province: '新疆维吾尔自治区',
        cities: ['乌鲁木齐市', '克拉玛依市', '吐鲁番市', '哈密市', '昌吉回族自治州', '博尔塔拉蒙古自治州', '巴音郭楞蒙古自治州', '阿克苏地区', '克孜勒苏柯尔克孜自治州', '喀什地区', '和田地区', '伊犁哈萨克自治州', '塔城地区', '阿勒泰地区', '石河子市', '阿拉尔市', '图木舒克市', '五家渠市', '北屯市', '铁门关市', '双河市', '可克达拉市', '昆玉市', '胡杨河市']
    },
    {
        province: '香港特别行政区',
        cities: ['香港岛', '九龙', '新界']
    },
    {
        province: '澳门特别行政区',
        cities: ['澳门半岛', '氹仔', '路环']
    }
];

const emit = defineEmits(['uploaded']);
const store = useStore();

const regionOptions = computed(() => store.getters['regions/regionOptions'] || []);
const companyOptions = computed(() => store.getters['companies/companyOptions'] || []);
const brandOptions = computed(() => store.getters['brands/brandOptions'] || []);
const modelOptions = computed(() => store.getters['models/modelOptions'] || []);

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
    notes: '',
    config: {
        motor: null,
        engine: null,
        fuelType: null,
        stepType: null,
        suspension: null,
        axle: null,
        otherConfigs: null
    }
});

const brandText = ref('');
const modelText = ref('');
const companyText = ref('');

const showConfigPanel = ref(false);
const toggleConfigPanel = () => {
    showConfigPanel.value = !showConfigPanel.value;
};

const brandHint = computed(() => {
    if (!brandText.value) return '';
    return form.brandId
        ? `已匹配品牌：${brandOptions.value.find((i) => i.value === form.brandId)?.label}`
        : '系统暂未收录该品牌，将以备注形式保存。';
});

const modelHint = computed(() => {
    if (!modelText.value) return '';
    return form.modelId
        ? `已匹配车型：${modelOptions.value.find((i) => i.value === form.modelId)?.label}`
        : '系统暂未收录该车型，将在后台自动创建。';
});

const companyHint = computed(() => {
    if (!companyText.value) return '';
    return form.companyId
        ? `已匹配公司：${companyOptions.value.find((i) => i.value === form.companyId)?.label}`
        : '系统暂未收录该公司，将以备注形式保存。';
});

const regionPickerRef = ref(null);
const regionPanelVisible = ref(false);
const selectedProvinceIndex = ref(0);
const selectedCity = ref('');
const selectedProvinceForCity = ref('');
const regionLabel = ref('');

const currentCities = computed(() => PROVINCE_CITY_DATA[selectedProvinceIndex.value]?.cities || []);
const provinceMatchesSelectedCity = computed(() =>
    PROVINCE_CITY_DATA[selectedProvinceIndex.value]?.province === selectedProvinceForCity.value
);

const toggleRegionPanel = () => {
    regionPanelVisible.value = !regionPanelVisible.value;
};

const selectProvince = (index) => {
    selectedProvinceIndex.value = index;
};

const normalizeCityName = (name = '') =>
    name.replace(/市|地区|特别行政区|自治州|盟|林区|区|县/g, '').trim();

const resolveRegionId = (cityName) => {
    if (!cityName || !regionOptions.value.length) return null;
    const normalizedCity = normalizeCityName(cityName);
    const match = regionOptions.value.find(
        (item) => normalizeCityName(item.label || '') === normalizedCity
    );
    return match ? match.value : null;
};

const selectCity = (city) => {
    const province = PROVINCE_CITY_DATA[selectedProvinceIndex.value]?.province || '';
    selectedProvinceForCity.value = province;
    selectedCity.value = city;
    regionLabel.value = `${province} / ${city}`;
    form.regionId = resolveRegionId(city);
    regionPanelVisible.value = false;
};

watch(regionOptions, () => {
    if (selectedCity.value && !form.regionId) {
        form.regionId = resolveRegionId(selectedCity.value);
    }
});

const handleClickOutside = (event) => {
    if (
        regionPanelVisible.value &&
        regionPickerRef.value &&
        !regionPickerRef.value.contains(event.target)
    ) {
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

watch(() => brandText.value, (val) => {
    form.brandId = matchOptionId(val, brandOptions.value);
});

watch(() => modelText.value, (val) => {
    form.modelId = matchOptionId(val, modelOptions.value);
});

watch(() => companyText.value, (val) => {
    form.companyId = matchOptionId(val, companyOptions.value);
});

const fileList = ref([]);
const selectedFile = ref(null);
const loading = ref(false);

const handleFileChange = (file, files) => {
    fileList.value = files.slice(-1);
    selectedFile.value = file.raw;
};

const reset = () => {
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
    form.config = {
        motor: null,
        engine: null,
        fuelType: null,
        stepType: null,
        suspension: null,
        axle: null,
        otherConfigs: null
    };

    brandText.value = '';
    modelText.value = '';
    companyText.value = '';

    selectedProvinceIndex.value = 0;
    selectedCity.value = '';
    selectedProvinceForCity.value = '';
    regionLabel.value = '';
    regionPanelVisible.value = false;

    fileList.value = [];
    selectedFile.value = null;
    showConfigPanel.value = false;
};

const composeRemark = () => {
    const notes = [];
    if (form.notes?.trim()) notes.push(form.notes.trim());
    if (brandText.value?.trim()) notes.push(`品牌：${brandText.value.trim()}`);
    if (modelText.value?.trim()) notes.push(`车型：${modelText.value.trim()}`);
    if (companyText.value?.trim()) notes.push(`公司：${companyText.value.trim()}`);
    if (regionLabel.value) notes.push(`地区：${regionLabel.value}`);
    return notes.join('\n');
};

const buildConfigPayload = () => {
    const hasConfig =
        form.brandId ||
        form.modelId ||
        Object.values(form.config).some((v) => v && v.toString().trim().length);
    if (!hasConfig) return null;
    return {
        brandId: form.brandId || null,
        modelId: form.modelId || null,
        motor: form.config.motor?.trim() || null,
        engine: form.config.engine?.trim() || null,
        fuelType: form.config.fuelType || null,
        stepType: form.config.stepType?.trim() || null,
        suspension: form.config.suspension?.trim() || null,
        axle: form.config.axle?.trim() || null,
        otherConfigs: form.config.otherConfigs?.trim() || null
    };
};

const validate = () => {
    if (!selectedFile.value) {
        ElMessage.warning('请先选择要上传的图片');
        return false;
    }
    if (!form.plateNumber?.trim()) {
        ElMessage.warning('请填写车牌号');
        return false;
    }
    if (!companyText.value?.trim() && !form.companyId) {
        ElMessage.warning('请填写或选择公司');
        return false;
    }
    if (!selectedCity.value && !form.regionId) {
        ElMessage.warning('请选择城市');
        return false;
    }
    if (!form.modelId && !modelText.value?.trim()) {
        ElMessage.warning('请填写车型名称');
        return false;
    }
    return true;
};

const submit = async () => {
    if (!validate()) return;
    try {
        loading.value = true;
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
            regionProvince: selectedProvinceForCity.value || null,
            regionCity: selectedCity.value || null,
            factoryDate: form.factoryDate || null,
            launchDate: form.launchDate || null,
            airConditioned: form.airConditioned,
            remark: composeRemark() || null,
            source: '用户上传',
            config: buildConfigPayload(),
            uploadUser: form.plateNumber?.trim() || null
        };
        await uploadVehicleWithImage(payload, selectedFile.value);
        ElMessage.success('车辆档案已创建');
        emit('uploaded');
        reset();
    } catch (error) {
        ElMessage.error(error.message || '上传失败，请稍后重试');
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    document.addEventListener('click', handleClickOutside);
    store.dispatch('regions/loadRegions');
    store.dispatch('companies/loadCompanies');
    store.dispatch('brands/loadBrands');
    store.dispatch('models/loadModels');
});

onBeforeUnmount(() => {
    document.removeEventListener('click', handleClickOutside);
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

.upload-form {
    .upload-area {
        width: 100%;
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
    background: #0f172a;
    color: #e2e8f0;
    box-shadow: 0 24px 50px rgba(15, 23, 42, 0.45);
    overflow: hidden;
    z-index: 30;
}

.region-panel__provinces {
    width: 45%;
    border-right: 1px solid rgba(148, 163, 184, 0.2);
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
        background: rgba(37, 99, 235, 0.2);
        color: #fff;
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
    border-radius: 999px;
    background: transparent;
    color: #e2e8f0;
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