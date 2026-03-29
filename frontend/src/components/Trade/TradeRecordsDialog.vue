<template>
    <el-dialog
        v-model="visibleProxy"
        class="trade-record-dialog"
        title="购买记录"
        width="900px"
        :lock-scroll="false"
        append-to-body
        destroy-on-close
    >
        <div class="trade-record-panel">
            <div class="trade-record-toolbar">
                <span class="summary">共 {{ records.length }} 条记录</span>
                <el-button text :loading="loading" @click="emit('refresh')">刷新</el-button>
            </div>

            <div v-if="loading" class="state">购买记录加载中...</div>
            <div v-else-if="!records.length" class="state">暂无拼团或交易记录</div>
            <div v-else class="trade-record-list">
                <article
                    v-for="record in pagedRecords"
                    :key="record.recordId || record.orderId || record.outTradeNo"
                    class="trade-record-row"
                >
                    <div class="trade-record-main">
                        <p class="trade-record-title">{{ record.title || '图片交易' }}</p>
                        <p class="muted">
                            {{ resolveTradeMode(record) }} · {{ resolveTradeStatus(record) }} · {{ formatTradePrice(record.payPriceCents) }}
                        </p>
                        <p class="muted">{{ formatTradeTime(record.createdAt) }}</p>
                    </div>
                    <div class="trade-record-actions">
                        <el-button
                            v-if="canContinueGroup(record)"
                            :loading="cancelingIds.has(String(record.recordId || record.outTradeNo || ''))"
                            size="small"
                            type="danger"
                            plain
                            @click="emit('cancel-group', record)"
                        >
                            取消拼团
                        </el-button>
                        <el-button
                            v-if="record.canDownload"
                            type="primary"
                            size="small"
                            @click="emit('download', record)"
                        >
                            下载原图
                        </el-button>
                    </div>
                </article>
            </div>

            <div v-if="!loading && records.length" class="pager-wrap">
                <el-pagination
                    background
                    layout="prev, pager, next"
                    :total="records.length"
                    :page-size="safePageSize"
                    :current-page="currentPage"
                    @current-change="onPageChange"
                />
            </div>
        </div>
    </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue';

const props = defineProps({
    modelValue: {
        type: Boolean,
        default: false
    },
    records: {
        type: Array,
        default: () => []
    },
    loading: {
        type: Boolean,
        default: false
    },
    cancelingIds: {
        type: Object,
        default: () => new Set()
    },
    pageSize: {
        type: Number,
        default: 30
    }
});

const emit = defineEmits(['update:modelValue', 'refresh', 'cancel-group', 'download']);

const cancelingIds = computed(() => {
    if (props.cancelingIds instanceof Set) {
        return props.cancelingIds;
    }
    return new Set();
});

const currentPage = ref(1);

const safePageSize = computed(() => {
    const value = Number(props.pageSize || 30);
    return value > 0 ? value : 30;
});

const visibleProxy = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
});

const pagedRecords = computed(() => {
    const start = (currentPage.value - 1) * safePageSize.value;
    return props.records.slice(start, start + safePageSize.value);
});

const onPageChange = (page) => {
    currentPage.value = Number(page || 1);
};

const canContinueGroup = (record) =>
    Number(record?.orderMode || 0) !== 2 &&
    Number(record?.tradeStatus || 0) === 0 &&
    !record?.canDownload &&
    Boolean(record?.outTradeNo);

const resolveTradeMode = (record) => (Number(record?.orderMode || 0) === 2 ? '直接下单' : '拼团购买');

const resolveTradeStatus = (record) => {
    const status = Number(record?.tradeStatus || 0);
    if (status === 1) return '交易成功';
    if (status === 2) return '拼团失败（已退款）';
    return Number(record?.orderMode || 0) === 2 ? '处理中' : '拼团中';
};

const formatTradePrice = (cents) => `¥${(Number(cents || 0) / 100).toFixed(2)}`;

const formatTradeTime = (value) => {
    const ts = value ? new Date(value) : null;
    if (!ts || Number.isNaN(ts.getTime())) return '-';
    return ts.toLocaleString('zh-CN', { hour12: false });
};

watch(
    () => props.modelValue,
    (visible) => {
        if (visible) {
            currentPage.value = 1;
        }
    }
);

watch(
    () => props.records.length,
    (size) => {
        const maxPage = Math.max(1, Math.ceil(size / safePageSize.value));
        if (currentPage.value > maxPage) {
            currentPage.value = maxPage;
        }
    }
);
</script>

<style scoped lang="scss">
.trade-record-panel {
    display: grid;
    gap: 12px;
}

.trade-record-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.summary {
    color: #475569;
    font-size: 13px;
}

.state {
    text-align: center;
    color: #94a3b8;
    padding: 24px 0;
}

.trade-record-list {
    display: grid;
    gap: 10px;
}

.trade-record-row {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.trade-record-main {
    min-width: 0;
}

.trade-record-title {
    margin: 0;
    color: #0f172a;
    font-weight: 600;
}

.muted {
    margin: 4px 0 0;
    color: #64748b;
    font-size: 13px;
    word-break: break-word;
}

.trade-record-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
}

.pager-wrap {
    display: flex;
    justify-content: center;
}

.trade-record-dialog:deep(.el-dialog__body) {
    max-height: min(74vh, 820px);
    overflow-y: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
}

.trade-record-dialog:deep(.el-dialog__body::-webkit-scrollbar) {
    width: 0;
    height: 0;
}

@media (max-width: 768px) {
    .trade-record-row {
        flex-direction: column;
        align-items: flex-start;
    }

    .trade-record-actions {
        width: 100%;
        justify-content: flex-end;
    }
}
</style>
