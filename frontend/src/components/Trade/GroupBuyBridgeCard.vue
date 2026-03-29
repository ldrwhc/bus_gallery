<template>
    <section class="group-buy-bridge">
        <div class="group-buy-bridge__head">
            <div>
                <p class="group-buy-bridge__eyebrow">交易模块</p>
                <h3>拼团购买</h3>
            </div>
        </div>

        <div class="group-buy-bridge__price">
            <p>当前图片价格：<strong>{{ priceText }}</strong></p>
            <p class="muted">拼团价：<strong>{{ groupPriceText }}</strong></p>
        </div>

        <div class="group-buy-bridge__teams">
            <p class="teams-title">正在拼团</p>
            <p v-if="loading" class="teams-empty">拼团信息加载中...</p>
            <p v-else-if="!activeTeams.length" class="teams-empty">暂无进行中的团。</p>
            <div v-else class="team-list">
                <div v-for="team in activeTeams" :key="team.teamId" class="team-item">
                    <div class="team-item__meta">
                        <span>团号 {{ team.teamId }}</span>
                        <span>{{ team.completeCount }}/{{ team.targetCount }} 人</span>
                    </div>
                    <button type="button" class="join-btn" @click="emit('join-team', team.teamId)">参与该团</button>
                </div>
            </div>
        </div>

        <div class="group-buy-bridge__meta">
            <span>图片ID: {{ context.imageId || '-' }}</span>
            <span>goodsId: {{ context.goodsId || '-' }}</span>
            <span>activityId: {{ context.activityId || '-' }}</span>
        </div>
    </section>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
    context: {
        type: Object,
        default: () => ({})
    },
    marketConfig: {
        type: Object,
        default: () => null
    },
    activeTeams: {
        type: Array,
        default: () => []
    },
    loading: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['buy', 'group-buy', 'join-team']);

const centsText = (value) => {
    if (value == null || value === '') return '-';
    return `${(Number(value || 0) / 100).toFixed(2)} 元`;
};

const priceText = computed(() => centsText(props.marketConfig?.originalPriceCents));
const groupPriceText = computed(() => centsText(props.marketConfig?.groupPriceCents));
</script>

<style scoped lang="scss">
.group-buy-bridge {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.group-buy-bridge__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.group-buy-bridge__eyebrow {
    margin: 0;
    font-size: 12px;
    letter-spacing: 0.08em;
    color: #64748b;
}

h3 {
    margin: 2px 0 0;
    color: #0f172a;
}

.group-buy-bridge__price {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 10px;
}

.group-buy-bridge__price p {
    margin: 0;
    font-size: 13px;
    color: #1f2937;
}

.group-buy-bridge__price .muted {
    margin-top: 4px;
    color: #64748b;
}

.group-buy-bridge__teams {
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 10px;
}

.teams-title {
    margin: 0 0 8px;
    font-size: 13px;
    color: #111827;
    font-weight: 600;
}

.teams-empty {
    margin: 0;
    color: #6b7280;
    font-size: 12px;
}

.team-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.team-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    padding: 8px;
}

.team-item__meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 12px;
    color: #334155;
}

.join-btn {
    border: 1px solid #93c5fd;
    background: #eff6ff;
    color: #1d4ed8;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
    padding: 6px 10px;
    cursor: pointer;
}

.group-buy-bridge__meta {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px 10px;
    font-size: 12px;
    color: #334155;
}

@media (max-width: 640px) {
    .group-buy-bridge__meta {
        grid-template-columns: 1fr;
    }
    .team-item {
        flex-direction: column;
        align-items: flex-start;
    }
    .join-btn {
        width: 100%;
    }
}
</style>
