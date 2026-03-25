<template>
    <div class="dashboard-page">
        <main class="constrained">
            <section class="card hero">
                <div>
                    <p class="eyebrow">Station Dashboard</p>
                    <h1>站长后台</h1>
                    <p class="muted">管理用户角色、审核分区与基础数据表（增删改查）。</p>
                </div>
                <el-button type="primary" :loading="refreshing" @click="reloadAll">刷新数据</el-button>
            </section>

            <section class="stats-grid">
                <article class="stat"><p>总用户</p><strong>{{ overview.totalUsers }}</strong></article>
                <article class="stat"><p>站长</p><strong>{{ overview.stationUsers }}</strong></article>
                <article class="stat"><p>审核员</p><strong>{{ overview.reviewerUsers }}</strong></article>
                <article class="stat"><p>普通用户</p><strong>{{ overview.normalUsers }}</strong></article>
                <article class="stat"><p>待审核</p><strong>{{ overview.pendingSubmissions }}</strong></article>
                <article class="stat"><p>已通过</p><strong>{{ overview.approvedSubmissions }}</strong></article>
                <article class="stat"><p>已拒绝</p><strong>{{ overview.rejectedSubmissions }}</strong></article>
            </section>

            <section class="card">
                <header class="row-between">
                    <div>
                        <h2>用户角色管理</h2>
                        <p class="muted">审核员目标地区按省级划分，必须选择省级行政区。</p>
                    </div>
                </header>
                <div v-if="usersLoading" class="state">加载中...</div>
                <el-table v-else :data="userRows" stripe>
                    <el-table-column prop="id" label="ID" width="80" />
                    <el-table-column prop="username" label="用户名" min-width="120" />
                    <el-table-column prop="displayName" label="昵称" min-width="120" />
                    <el-table-column label="当前角色" width="120">
                        <template #default="{ row }">{{ roleLabel(row.role) }}</template>
                    </el-table-column>
                    <el-table-column label="审核区域(省)" min-width="180">
                        <template #default="{ row }">{{ resolveRegionName(row.reviewRegionId) || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="目标角色" min-width="150">
                        <template #default="{ row }">
                            <el-select v-model="row.nextRole" :disabled="isCurrentUser(row)" @change="handleRoleChange(row)">
                                <el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
                            </el-select>
                        </template>
                    </el-table-column>
                    <el-table-column label="目标审核区域(省)" min-width="210">
                        <template #default="{ row }">
                            <el-select
                                v-model="row.nextReviewRegionId"
                                clearable
                                filterable
                                :filter-method="(keyword) => handleProvinceFilter(row, keyword)"
                                @visible-change="(visible) => handleProvinceVisible(row, visible)"
                                :disabled="row.nextRole !== 'REVIEWER' || isCurrentUser(row)"
                                popper-class="province-select-popper"
                                placeholder="审核员必选省级区域"
                            >
                                <el-option
                                    v-for="option in filteredProvinceOptions(row)"
                                    :key="option.value"
                                    :label="option.label"
                                    :value="option.value"
                                />
                            </el-select>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="120" fixed="right">
                        <template #default="{ row }">
                            <el-button
                                type="primary"
                                :loading="savingUserId === row.id"
                                :disabled="isCurrentUser(row) || !isUserDirty(row)"
                                @click="saveUserRole(row)"
                            >
                                保存
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </section>

            <section class="card">
                <el-tabs v-model="activeTab">
                    <el-tab-pane label="地区表" name="regions">
                        <div class="row-between">
                            <p class="muted">地区基础表（建议省级 level=1，地级市 level=2）。</p>
                            <el-button type="primary" @click="openRegionEditor()">新增地区</el-button>
                        </div>
                        <el-table :data="regionRows" stripe>
                            <el-table-column prop="id" label="ID" width="80" />
                            <el-table-column prop="name" label="名称" min-width="160" />
                            <el-table-column prop="parentName" label="父级" min-width="160" />
                            <el-table-column prop="level" label="层级" width="100" />
                            <el-table-column label="操作" width="180" fixed="right">
                                <template #default="{ row }">
                                    <el-button size="small" @click="openRegionEditor(row)">编辑</el-button>
                                    <el-button size="small" type="danger" @click="removeRegion(row)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>

                    <el-tab-pane label="公司表" name="companies">
                        <div class="row-between">
                            <p class="muted">运营公司基础表。</p>
                            <el-button type="primary" @click="openCompanyEditor()">新增公司</el-button>
                        </div>
                        <el-table :data="companyRows" stripe>
                            <el-table-column prop="id" label="ID" width="80" />
                            <el-table-column prop="name" label="名称" min-width="180" />
                            <el-table-column prop="regionName" label="地区" min-width="140" />
                            <el-table-column label="操作" width="180" fixed="right">
                                <template #default="{ row }">
                                    <el-button size="small" @click="openCompanyEditor(row)">编辑</el-button>
                                    <el-button size="small" type="danger" @click="removeCompany(row)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>

                    <el-tab-pane label="品牌表" name="brands">
                        <div class="row-between">
                            <p class="muted">品牌基础表。</p>
                            <el-button type="primary" @click="openBrandEditor()">新增品牌</el-button>
                        </div>
                        <el-table :data="brandRows" stripe>
                            <el-table-column prop="id" label="ID" width="80" />
                            <el-table-column prop="name" label="英文名" min-width="140" />
                            <el-table-column prop="chnName" label="中文名" min-width="140" />
                            <el-table-column label="操作" width="180" fixed="right">
                                <template #default="{ row }">
                                    <el-button size="small" @click="openBrandEditor(row)">编辑</el-button>
                                    <el-button size="small" type="danger" @click="removeBrand(row)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>

                    <el-tab-pane label="车型表" name="models">
                        <div class="row-between">
                            <p class="muted">车型基础表。</p>
                            <el-button type="primary" @click="openModelEditor()">新增车型</el-button>
                        </div>
                        <el-table :data="modelRows" stripe>
                            <el-table-column prop="id" label="ID" width="80" />
                            <el-table-column prop="name" label="车型名" min-width="180" />
                            <el-table-column prop="brandName" label="品牌" min-width="140" />
                            <el-table-column label="操作" width="180" fixed="right">
                                <template #default="{ row }">
                                    <el-button size="small" @click="openModelEditor(row)">编辑</el-button>
                                    <el-button size="small" type="danger" @click="removeModel(row)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>

                    <el-tab-pane label="异常图片" name="images">
                        <div class="row-between">
                            <p class="muted">检查未关联车辆的图片，以及 MinIO 原图或缩略图已丢失的异常记录。</p>
                            <div class="toolbar-actions">
                                <el-button :loading="suspectImagesLoading" @click="loadSuspectImages">重新检查</el-button>
                                <el-button
                                    type="danger"
                                    plain
                                    :loading="cleaningAllSuspects"
                                    :disabled="!suspectImageRows.length"
                                    @click="cleanupAllSuspects"
                                >
                                    清理全部异常图片
                                </el-button>
                            </div>
                        </div>
                        <div v-if="suspectImagesLoading" class="state">正在检查异常图片...</div>
                        <div v-else-if="!suspectImageRows.length" class="state">未发现异常图片记录</div>
                        <el-table v-else :data="suspectImageRows" stripe>
                            <el-table-column prop="id" label="ID" width="90" />
                            <el-table-column prop="issueSummary" label="异常原因" min-width="220" />
                            <el-table-column label="上传者" min-width="180">
                                <template #default="{ row }">
                                    {{ row.uploaderDisplayName || row.uploadUser || row.uploaderUsername || '-' }}
                                </template>
                            </el-table-column>
                            <el-table-column prop="relationCount" label="关联车辆数" width="110" />
                            <el-table-column prop="primaryVehicleId" label="主要车辆ID" width="120" />
                            <el-table-column label="对象路径" min-width="280">
                                <template #default="{ row }">
                                    <div class="path-cell">{{ row.objectName || '-' }}</div>
                                </template>
                            </el-table-column>
                            <el-table-column label="创建时间" min-width="180">
                                <template #default="{ row }">{{ row.createTime || '-' }}</template>
                            </el-table-column>
                            <el-table-column label="操作" width="140" fixed="right">
                                <template #default="{ row }">
                                    <el-button
                                        size="small"
                                        type="danger"
                                        :loading="cleaningSingleImageId === row.id"
                                        @click="cleanupSingleSuspect(row)"
                                    >
                                        清理
                                    </el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>
                </el-tabs>
            </section>

            <section class="card">
                <header class="row-between">
                    <div>
                        <h2>评论管理</h2>
                        <p class="muted">站长可删除任意评论；评论作者也可在前台详情页删除自己的评论。</p>
                    </div>
                    <el-button :loading="commentAdmin.loading" @click="loadAdminComments(commentAdmin.page)">刷新评论</el-button>
                </header>
                <div v-if="commentAdmin.loading && !commentAdmin.records.length" class="state">正在加载评论...</div>
                <div v-else-if="!commentAdmin.records.length" class="state">暂无评论数据</div>
                <el-table v-else :data="commentAdmin.records" stripe>
                    <el-table-column prop="id" label="ID" width="90" />
                    <el-table-column prop="vehicleId" label="车辆ID" width="110" />
                    <el-table-column label="用户" min-width="160">
                        <template #default="{ row }">
                            {{ row.displayName || row.username || ('用户#' + (row.userId || '-')) }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="content" label="评论内容" min-width="260" />
                    <el-table-column prop="createdAt" label="创建时间" min-width="180" />
                    <el-table-column label="操作" width="120" fixed="right">
                        <template #default="{ row }">
                            <el-button
                                type="danger"
                                size="small"
                                :loading="deletingCommentId === row.id"
                                @click="removeCommentByStation(row)"
                            >
                                删除
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="row-between" v-if="commentAdmin.total > commentAdmin.size">
                    <p class="muted">共 {{ commentAdmin.total }} 条评论</p>
                    <el-pagination
                        background
                        layout="prev, pager, next"
                        :current-page="commentAdmin.page"
                        :page-size="commentAdmin.size"
                        :total="commentAdmin.total"
                        @current-change="loadAdminComments"
                    />
                </div>
            </section>
        </main>

        <el-dialog v-model="regionEditor.visible" :title="regionEditor.id ? '编辑地区' : '新增地区'" width="520px">
            <el-form label-position="top">
                <el-form-item label="名称"><el-input v-model="regionEditor.name" /></el-form-item>
                <el-form-item label="父级地区">
                    <el-select v-model="regionEditor.parentId" clearable filterable>
                        <el-option v-for="option in regionOptions" :key="option.value" :label="option.label" :value="option.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="层级"><el-input-number v-model="regionEditor.level" :min="1" :max="4" /></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="regionEditor.visible = false">取消</el-button>
                <el-button type="primary" :loading="regionEditor.saving" @click="saveRegion">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="companyEditor.visible" :title="companyEditor.id ? '编辑公司' : '新增公司'" width="620px">
            <el-form label-position="top">
                <el-form-item label="名称"><el-input v-model="companyEditor.name" /></el-form-item>
                <el-form-item label="地区">
                    <el-select v-model="companyEditor.regionId" clearable filterable>
                        <el-option v-for="option in regionOptions" :key="option.value" :label="option.label" :value="option.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="描述"><el-input v-model="companyEditor.description" type="textarea" /></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="companyEditor.visible = false">取消</el-button>
                <el-button type="primary" :loading="companyEditor.saving" @click="saveCompany">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="brandEditor.visible" :title="brandEditor.id ? '编辑品牌' : '新增品牌'" width="620px">
            <el-form label-position="top">
                <el-form-item label="英文名"><el-input v-model="brandEditor.name" /></el-form-item>
                <el-form-item label="中文名"><el-input v-model="brandEditor.chnName" /></el-form-item>
                <el-form-item label="描述"><el-input v-model="brandEditor.description" type="textarea" /></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="brandEditor.visible = false">取消</el-button>
                <el-button type="primary" :loading="brandEditor.saving" @click="saveBrand">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="modelEditor.visible" :title="modelEditor.id ? '编辑车型' : '新增车型'" width="640px">
            <el-form label-position="top">
                <el-form-item label="车型名"><el-input v-model="modelEditor.name" /></el-form-item>
                <el-form-item label="品牌">
                    <el-select v-model="modelEditor.brandId" filterable>
                        <el-option v-for="option in brandOptions" :key="option.value" :label="option.label" :value="option.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="描述"><el-input v-model="modelEditor.description" type="textarea" /></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="modelEditor.visible = false">取消</el-button>
                <el-button type="primary" :loading="modelEditor.saving" @click="saveModel">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
    createAdminBrand,
    createAdminCompany,
    createAdminModel,
    createAdminRegion,
    deleteAdminBrand,
    deleteAdminComment,
    deleteAdminCompany,
    deleteAdminModel,
    deleteAdminRegion,
    fetchAdminBrands,
    fetchAdminComments,
    fetchAdminCompanies,
    fetchAdminModels,
    fetchAdminOverview,
    fetchAdminRegions,
    fetchAdminSuspectImages,
    fetchAdminUsers,
    cleanupAdminSuspectImages,
    updateAdminBrand,
    updateAdminCompany,
    updateAdminModel,
    updateAdminRegion,
    updateAdminUserRole
} from '@/api/admin';

const store = useStore();

const refreshing = ref(false);
const usersLoading = ref(false);
const savingUserId = ref(null);
const activeTab = ref('regions');
const suspectImagesLoading = ref(false);
const suspectImageRows = ref([]);
const cleaningAllSuspects = ref(false);
const cleaningSingleImageId = ref(null);
const deletingCommentId = ref(null);
const commentAdmin = reactive({
    loading: false,
    page: 1,
    size: 20,
    total: 0,
    records: []
});

const overview = reactive({
    totalUsers: 0,
    stationUsers: 0,
    reviewerUsers: 0,
    normalUsers: 0,
    pendingSubmissions: 0,
    approvedSubmissions: 0,
    rejectedSubmissions: 0
});

const roleOptions = [
    { value: 'USER', label: '普通用户' },
    { value: 'REVIEWER', label: '审核员' },
    { value: 'STATION', label: '站长' }
];

const userRows = ref([]);
const regionRows = ref([]);
const companyRows = ref([]);
const brandRows = ref([]);
const modelRows = ref([]);
const PROVINCE_NAMES = [
    '北京市', '天津市', '上海市', '重庆市',
    '河北省', '山西省', '辽宁省', '吉林省', '黑龙江省',
    '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省',
    '河南省', '湖北省', '湖南省', '广东省', '海南省',
    '四川省', '贵州省', '云南省', '陕西省', '甘肃省', '青海省',
    '内蒙古自治区', '广西壮族自治区', '西藏自治区', '宁夏回族自治区', '新疆维吾尔自治区',
    '香港特别行政区', '澳门特别行政区', '台湾省'
];

const currentUserId = computed(() => Number(store.state.auth.profile?.id || 0));
const regionOptions = computed(() =>
    regionRows.value
        .map((item) => ({ value: item.id, label: item.name, parentId: item.parentId, level: item.level }))
        .sort((a, b) => String(a.label).localeCompare(String(b.label), 'zh-CN'))
);
const normalizeKeyword = (value) => String(value || '').replace(/\s+/g, '').toLowerCase();
const isProvinceLevelRegion = (item) => {
    if (!item) return false;
    if (Number(item.level) === 1 || item.parentId == null) return true;
    const label = String(item.label || '').trim();
    return PROVINCE_NAMES.some((name) => label === name || label.startsWith(name));
};
const provinceRegionOptions = computed(() =>
    regionOptions.value.filter(isProvinceLevelRegion)
);
const regionNameMap = computed(() =>
    regionRows.value.reduce((acc, item) => ({ ...acc, [item.id]: item.name }), {})
);
const brandOptions = computed(() =>
    brandRows.value.map((item) => ({ value: item.id, label: item.chnName || item.name }))
);

const roleLabel = (role) => {
    if (role === 'STATION') return '站长';
    if (role === 'REVIEWER') return '审核员';
    return '普通用户';
};

const resolveRegionName = (id) => (id ? regionNameMap.value[id] : '');
const isCurrentUser = (row) => Number(row.id) === currentUserId.value;

const normalizeUserRow = (item) => ({
    ...item,
    nextRole: item.role || 'USER',
    nextReviewRegionId: item.reviewRegionId || null,
    _provinceKeyword: ''
});

const isUserDirty = (row) =>
    row.nextRole !== row.role ||
    Number(row.nextReviewRegionId || 0) !== Number(row.reviewRegionId || 0);

const handleRoleChange = (row) => {
    if (row.nextRole !== 'REVIEWER') {
        row.nextReviewRegionId = null;
        row._provinceKeyword = '';
    }
};

const handleProvinceFilter = (row, keyword) => {
    if (!row) return;
    row._provinceKeyword = keyword || '';
};

const handleProvinceVisible = (row, visible) => {
    if (!visible && row) {
        row._provinceKeyword = '';
    }
};

const filteredProvinceOptions = (row) => {
    const keyword = normalizeKeyword(row?._provinceKeyword);
    if (!keyword) return provinceRegionOptions.value;
    return provinceRegionOptions.value.filter((item) => {
        const label = normalizeKeyword(item.label);
        return label.includes(keyword) || String(item.value).includes(keyword);
    });
};

const saveUserRole = async (row) => {
    if (row.nextRole === 'REVIEWER' && !row.nextReviewRegionId) {
        ElMessage.warning('审核员必须配置省级审核区域');
        return;
    }
    savingUserId.value = row.id;
    try {
        const updated = await updateAdminUserRole(
            row.id,
            row.nextRole,
            row.nextRole === 'REVIEWER' ? row.nextReviewRegionId : null
        );
        row.role = updated.role;
        row.reviewRegionId = updated.reviewRegionId || null;
        row.nextRole = row.role;
        row.nextReviewRegionId = row.reviewRegionId;
        ElMessage.success('角色更新成功');
        await loadOverview();
    } catch (error) {
        ElMessage.error(error?.message || '角色更新失败');
    } finally {
        savingUserId.value = null;
    }
};

const loadOverview = async () => {
    const data = await fetchAdminOverview();
    Object.assign(overview, data || {});
};

const loadUsers = async () => {
    usersLoading.value = true;
    try {
        const list = await fetchAdminUsers();
        userRows.value = (Array.isArray(list) ? list : []).map(normalizeUserRow);
    } finally {
        usersLoading.value = false;
    }
};

const loadRegionTable = async () => {
    const list = await fetchAdminRegions();
    regionRows.value = Array.isArray(list) ? list : [];
};
const loadCompanyTable = async () => {
    const list = await fetchAdminCompanies();
    companyRows.value = Array.isArray(list) ? list : [];
};
const loadBrandTable = async () => {
    const list = await fetchAdminBrands();
    brandRows.value = Array.isArray(list) ? list : [];
};
const loadModelTable = async () => {
    const list = await fetchAdminModels();
    modelRows.value = Array.isArray(list) ? list : [];
};
const loadAdminComments = async (page = commentAdmin.page) => {
    commentAdmin.loading = true;
    try {
        const normalizedPage = Number(page) > 0 ? Number(page) : 1;
        const resp = await fetchAdminComments({
            page: normalizedPage,
            size: commentAdmin.size
        });
        commentAdmin.records = Array.isArray(resp?.records) ? resp.records : [];
        commentAdmin.page = resp?.page ?? normalizedPage;
        commentAdmin.size = resp?.size ?? commentAdmin.size;
        commentAdmin.total = resp?.total ?? commentAdmin.records.length;
    } finally {
        commentAdmin.loading = false;
    }
};
const loadSuspectImages = async () => {
    suspectImagesLoading.value = true;
    try {
        const list = await fetchAdminSuspectImages();
        suspectImageRows.value = Array.isArray(list) ? list : [];
    } finally {
        suspectImagesLoading.value = false;
    }
};

const reloadAll = async () => {
    refreshing.value = true;
    try {
        await Promise.all([
            loadOverview(),
            store.dispatch('regions/loadRegions').catch(() => {}),
            loadUsers(),
            loadRegionTable(),
            loadCompanyTable(),
            loadBrandTable(),
            loadModelTable(),
            loadAdminComments(commentAdmin.page),
            activeTab.value === 'images' ? loadSuspectImages() : Promise.resolve()
        ]);
    } catch (error) {
        ElMessage.error(error?.message || '后台数据加载失败');
    } finally {
        refreshing.value = false;
    }
};

const regionEditor = reactive({ visible: false, saving: false, id: null, name: '', parentId: null, level: 1 });
const companyEditor = reactive({ visible: false, saving: false, id: null, name: '', regionId: null, description: '' });
const brandEditor = reactive({ visible: false, saving: false, id: null, name: '', chnName: '', description: '' });
const modelEditor = reactive({ visible: false, saving: false, id: null, name: '', brandId: null, description: '' });

const openRegionEditor = (row = null) => {
    regionEditor.visible = true;
    regionEditor.id = row?.id || null;
    regionEditor.name = row?.name || '';
    regionEditor.parentId = row?.parentId || null;
    regionEditor.level = row?.level || 1;
};
const openCompanyEditor = (row = null) => {
    companyEditor.visible = true;
    companyEditor.id = row?.id || null;
    companyEditor.name = row?.name || '';
    companyEditor.regionId = row?.regionId || null;
    companyEditor.description = row?.description || '';
};
const openBrandEditor = (row = null) => {
    brandEditor.visible = true;
    brandEditor.id = row?.id || null;
    brandEditor.name = row?.name || '';
    brandEditor.chnName = row?.chnName || '';
    brandEditor.description = row?.description || '';
};
const openModelEditor = (row = null) => {
    modelEditor.visible = true;
    modelEditor.id = row?.id || null;
    modelEditor.name = row?.name || '';
    modelEditor.brandId = row?.brandId || null;
    modelEditor.description = row?.description || '';
};

const saveRegion = async () => {
    if (!regionEditor.name.trim()) {
        ElMessage.warning('地区名称不能为空');
        return;
    }
    regionEditor.saving = true;
    try {
        const payload = { name: regionEditor.name, parentId: regionEditor.parentId, level: regionEditor.level };
        if (regionEditor.id) await updateAdminRegion(regionEditor.id, payload);
        else await createAdminRegion(payload);
        regionEditor.visible = false;
        await loadRegionTable();
        ElMessage.success('地区保存成功');
    } catch (error) {
        ElMessage.error(error?.message || '地区保存失败');
    } finally {
        regionEditor.saving = false;
    }
};

const saveCompany = async () => {
    if (!companyEditor.name.trim()) {
        ElMessage.warning('公司名称不能为空');
        return;
    }
    companyEditor.saving = true;
    try {
        const payload = {
            name: companyEditor.name,
            regionId: companyEditor.regionId,
            description: companyEditor.description || null
        };
        if (companyEditor.id) await updateAdminCompany(companyEditor.id, payload);
        else await createAdminCompany(payload);
        companyEditor.visible = false;
        await loadCompanyTable();
        ElMessage.success('公司保存成功');
    } catch (error) {
        ElMessage.error(error?.message || '公司保存失败');
    } finally {
        companyEditor.saving = false;
    }
};

const saveBrand = async () => {
    if (!brandEditor.name.trim()) {
        ElMessage.warning('品牌名称不能为空');
        return;
    }
    brandEditor.saving = true;
    try {
        const payload = {
            name: brandEditor.name,
            chnName: brandEditor.chnName || null,
            description: brandEditor.description || null
        };
        if (brandEditor.id) await updateAdminBrand(brandEditor.id, payload);
        else await createAdminBrand(payload);
        brandEditor.visible = false;
        await loadBrandTable();
        ElMessage.success('品牌保存成功');
    } catch (error) {
        ElMessage.error(error?.message || '品牌保存失败');
    } finally {
        brandEditor.saving = false;
    }
};

const saveModel = async () => {
    if (!modelEditor.name.trim() || !modelEditor.brandId) {
        ElMessage.warning('车型名称和品牌不能为空');
        return;
    }
    modelEditor.saving = true;
    try {
        const payload = {
            name: modelEditor.name,
            brandId: modelEditor.brandId,
            description: modelEditor.description || null
        };
        if (modelEditor.id) await updateAdminModel(modelEditor.id, payload);
        else await createAdminModel(payload);
        modelEditor.visible = false;
        await loadModelTable();
        ElMessage.success('车型保存成功');
    } catch (error) {
        ElMessage.error(error?.message || '车型保存失败');
    } finally {
        modelEditor.saving = false;
    }
};

const confirmDelete = async (message, action) => {
    await ElMessageBox.confirm(message, '确认删除', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        closeOnClickModal: true,
        closeOnPressEscape: true
    });
    await action();
};

const removeRegion = (row) =>
    confirmDelete(`确认删除地区「${row.name}」吗？`, async () => {
        await deleteAdminRegion(row.id);
        await loadRegionTable();
        ElMessage.success('地区已删除');
    }).catch(() => {});

const removeCompany = (row) =>
    confirmDelete(`确认删除公司「${row.name}」吗？`, async () => {
        await deleteAdminCompany(row.id);
        await loadCompanyTable();
        ElMessage.success('公司已删除');
    }).catch(() => {});

const removeBrand = (row) =>
    confirmDelete(`确认删除品牌「${row.name}」吗？`, async () => {
        await deleteAdminBrand(row.id);
        await loadBrandTable();
        ElMessage.success('品牌已删除');
    }).catch(() => {});

const removeModel = (row) =>
    confirmDelete(`确认删除车型「${row.name}」吗？`, async () => {
        await deleteAdminModel(row.id);
        await loadModelTable();
        ElMessage.success('车型已删除');
    }).catch(() => {});

const removeCommentByStation = (row) =>
    confirmDelete(`确认删除评论 #${row.id} 吗？`, async () => {
        deletingCommentId.value = row.id;
        await deleteAdminComment(row.id);
        const maxPage = Math.max(1, Math.ceil(Math.max(commentAdmin.total - 1, 0) / commentAdmin.size));
        await loadAdminComments(Math.min(commentAdmin.page, maxPage));
        ElMessage.success('评论已删除');
    }).catch((error) => {
        if (!isDialogCancel(error) && error?.message) {
            ElMessage.error(error.message);
        }
    }).finally(() => {
        deletingCommentId.value = null;
    });

const isDialogCancel = (error) => ['cancel', 'close'].includes(String(error?.message || error || '').toLowerCase());

const cleanupSingleSuspect = (row) =>
    confirmDelete(`确认清理异常图片 #${row.id} 吗？`, async () => {
        cleaningSingleImageId.value = row.id;
        const result = await cleanupAdminSuspectImages([row.id]);
        if (Array.isArray(result?.failedIds) && result.failedIds.length) {
            throw new Error('异常图片清理失败');
        }
        await loadSuspectImages();
        ElMessage.success('异常图片已清理');
    }).catch((error) => {
        if (!isDialogCancel(error) && error?.message) {
            ElMessage.error(error.message);
        }
    }).finally(() => {
        cleaningSingleImageId.value = null;
    });

const cleanupAllSuspects = () =>
    confirmDelete('确认清理当前检测到的全部异常图片吗？该操作不可恢复。', async () => {
        cleaningAllSuspects.value = true;
        const result = await cleanupAdminSuspectImages();
        await loadSuspectImages();
        const deletedCount = Array.isArray(result?.deletedIds) ? result.deletedIds.length : 0;
        const failedCount = Array.isArray(result?.failedIds) ? result.failedIds.length : 0;
        if (failedCount > 0) {
            ElMessage.warning(`已清理 ${deletedCount} 张，但仍有 ${failedCount} 张失败`);
            return;
        }
        ElMessage.success(`已清理 ${deletedCount} 张异常图片`);
    }).catch((error) => {
        if (!isDialogCancel(error) && error?.message) {
            ElMessage.error(error.message);
        }
    }).finally(() => {
        cleaningAllSuspects.value = false;
    });

const refreshTabData = async (tab) => {
    if (tab === 'regions') {
        await loadRegionTable();
        return;
    }
    if (tab === 'companies') {
        await Promise.all([loadRegionTable(), loadCompanyTable()]);
        return;
    }
    if (tab === 'brands') {
        await loadBrandTable();
        return;
    }
    if (tab === 'models') {
        await Promise.all([loadBrandTable(), loadModelTable()]);
        return;
    }
    if (tab === 'images') {
        await loadSuspectImages();
    }
};

watch(activeTab, async (tab) => {
    try {
        await refreshTabData(tab);
    } catch (error) {
        ElMessage.error(error?.message || '表格刷新失败');
    }
});

onMounted(() => {
    reloadAll();
});
</script>

<style scoped lang="scss">
.dashboard-page { min-height: 100vh; background: #f5f7fb; }
.constrained { width: min(1240px, 100%); margin: 0 auto; padding: 24px 16px 72px; display: flex; flex-direction: column; gap: 16px; }
.card { background: #fff; border-radius: 18px; padding: 18px; box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08); }
.hero { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.eyebrow { margin: 0 0 4px; letter-spacing: 0.2em; text-transform: uppercase; color: #94a3b8; font-size: .75rem; }
.muted { color: #64748b; margin: 6px 0 0; }
.row-between { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 10px; }
.toolbar-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; }
.stat { background: #fff; border-radius: 14px; padding: 12px 14px; border: 1px solid #e2e8f0; }
.stat p { margin: 0; color: #64748b; font-size: .84rem; }
.stat strong { display: block; margin-top: 6px; font-size: 1.25rem; color: #0f172a; }
.state { text-align: center; padding: 26px 0; color: #94a3b8; }
.path-cell { word-break: break-all; color: #334155; }
:deep(.province-select-popper .el-select-dropdown__wrap) { max-height: 280px; }
@media (max-width: 900px) { .hero { flex-direction: column; align-items: flex-start; } }
</style>
