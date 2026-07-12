<template>
    <el-dialog v-model="visible" :title="editingId ? '编辑线路' : '新增线路'" width="560px" destroy-on-close
               @closed="reset">
        <el-form label-position="top" :model="form">
            <el-form-item label="线路号" required>
                <el-input v-model="form.routeNumber" placeholder="如 1路 / BRT1" />
            </el-form-item>
            <el-row :gutter="12">
                <el-col :span="12">
                    <el-form-item label="起点站"><el-input v-model="form.startStop" placeholder="上行起点" /></el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="终点站"><el-input v-model="form.endStop" placeholder="上行终点" /></el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="12">
                <el-col :span="12">
                    <el-form-item label="城市">
                        <el-select v-model="form.regionId" clearable filterable placeholder="选择城市">
                            <el-option v-for="r in regionOpts" :key="r.value" :label="r.label" :value="r.value" />
                        </el-select>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="运营公司">
                        <el-select v-model="form.companyId" clearable filterable placeholder="选择公司">
                            <el-option v-for="c in companyOpts" :key="c.value" :label="c.label" :value="c.value" />
                        </el-select>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="12">
                <el-col :span="8">
                    <el-form-item label="线路形式">
                        <el-select v-model="form.subType" clearable placeholder="主线">
                            <el-option v-for="(v,k) in SUB_TYPES" :key="k" :label="v" :value="k" />
                        </el-select>
                    </el-form-item>
                </el-col>
                <el-col :span="8">
                    <el-form-item label="公交类型">
                        <el-select v-model="form.routeType">
                            <el-option v-for="(v,k) in ROUTE_TYPES" :key="k" :label="v" :value="k" />
                        </el-select>
                    </el-form-item>
                </el-col>
                <el-col :span="8">
                    <el-form-item label="环线"><el-switch v-model="form.isLoop" /></el-form-item>
                </el-col>
            </el-row>
        </el-form>
        <template #footer>
            <el-button @click="visible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import { createRoute, fetchRoutes } from '@/api/routes';

const props = defineProps({
    modelValue: { type: Boolean, default: false },
    prefillRouteNumber: { type: String, default: '' },
    prefillRegionId: { type: Number, default: null },
    prefillCompanyId: { type: Number, default: null }
});

const emit = defineEmits(['update:modelValue', 'created']);

const store = useStore();
const visible = computed({
    get: () => props.modelValue,
    set: (v) => emit('update:modelValue', v)
});

const SUB_TYPES = { INTERVAL: '区间', BRANCH: '支线', EXPRESS: '快线', NIGHT: '夜班', DIRECT: '直达' };
const ROUTE_TYPES = { REGULAR: '常规', BRT: '快速公交', AIRPORT: '机场', TOURIST: '旅游', COMMUNITY: '微循环', SUBWAY: '地铁接驳' };

const regionOpts = computed(() => (store.state.regions?.list || []).map(r => ({ value: r.id, label: r.name })));
const companyOpts = computed(() => (store.state.companies?.list || []).map(c => ({ value: c.id, label: c.name })));
const editingId = ref(null);
const saving = ref(false);

const defaultForm = () => ({
    routeNumber: '', subType: null, parentRouteId: null,
    startStop: '', endStop: '', downStartStop: '', downEndStop: '',
    isLoop: false, regionId: null, companyId: null, routeType: 'REGULAR',
    lineLengthKm: null, ticketType: null, ticketPrice: '', operatingHours: '',
    isActive: true, firstOperated: null, lastOperated: null, remark: ''
});

const form = reactive(defaultForm());

watch(() => props.modelValue, (val) => {
    if (val) {
        Object.assign(form, defaultForm());
        form.routeNumber = props.prefillRouteNumber || '';
        form.regionId = props.prefillRegionId || null;
        form.companyId = props.prefillCompanyId || null;
        editingId.value = null;
        store.dispatch('regions/loadRegions').catch(() => {});
        store.dispatch('companies/loadCompanies').catch(() => {});
    }
});

const reset = () => Object.assign(form, defaultForm());

const save = async () => {
    if (!form.routeNumber.trim()) { ElMessage.warning('请输入线路号'); return; }
    saving.value = true;
    try {
        const payload = { ...form, routeNumber: form.routeNumber.trim() };
        const saved = await createRoute(payload);
        ElMessage.success('线路已创建');
        visible.value = false;
        // Reload routes in store so autocomplete picks it up
        store.dispatch('routes/loadRoutes', { size: 500 }).catch(() => {});
        emit('created', saved);
    } catch (e) { ElMessage.error(e?.message || '创建失败'); }
    finally { saving.value = false; }
};
</script>
