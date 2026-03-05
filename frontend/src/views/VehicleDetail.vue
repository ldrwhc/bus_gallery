<template>
    <div class="detail-page" v-if="vehicleData">
        <vehicle-gallery-card :vehicle="vehicleData.vehicle" :config="vehicleData.config"
            :images="vehicleData.images" />

        <el-card class="related-card" v-if="sameCompany.length">
            <template #header>同公司其他车辆</template>
            <el-row :gutter="12">
                <el-col v-for="item in sameCompany" :key="item.vehicle.id" :xs="24" :sm="12" :md="8" :lg="6">
                    <vehicle-card :vehicle="item.vehicle" :images="item.images" />
                </el-col>
            </el-row>
        </el-card>
    </div>
    <el-empty v-else description="数据加载中..." />
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import VehicleGalleryCard from '@/components/gallery/VehicleGalleryCard.vue';
import VehicleCard from '@/components/VehicleCard.vue';
import { useVehicleStore } from '@/store/modules/vehicle';

const route = useRoute();
const vehicleStore = useVehicleStore();

const vehicleData = ref(null);
const sameCompany = ref([]);

const loadSameCompany = async (companyId, currentVehicleId) => {
    if (!companyId) {
        sameCompany.value = [];
        return;
    }
    await vehicleStore.loadByCompany(companyId);
    const list =
        vehicleStore.listByCompany[companyId]?.list ||
        vehicleStore.listByCompany[companyId] ||
        [];
    sameCompany.value = list.filter(
        (item) => item.vehicle.id !== currentVehicleId
    );
};

const loadDetail = async (id) => {
    if (!id) {
        vehicleData.value = null;
        sameCompany.value = [];
        return;
    }
    const detail = await vehicleStore.loadDetail(id);
    vehicleData.value = detail;
    await loadSameCompany(
        detail?.vehicle?.companyId,
        detail?.vehicle?.id
    );
};

watch(
    () => route.params.id,
    (id) => {
        const resolved = Number(id);
        if (Number.isNaN(resolved)) {
            vehicleData.value = null;
            sameCompany.value = [];
            return;
        }
        loadDetail(resolved);
    },
    { immediate: true }
);
</script>

<style scoped lang="scss">
.detail-page {
    padding: 16px;
}

.related-card {
    margin-top: 24px;
}

/* 覆写 VehicleGalleryCard 中轮播图样式，确保原图比例展示 */
:deep(.vehicle-gallery-card .carousel__main) {
    min-height: 360px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #0b1220;
}

:deep(.vehicle-gallery-card .carousel__main img) {
    width: 100%;
    height: auto;
    max-height: 520px;
    object-fit: contain;
    background: #0b1220;
}

:deep(.vehicle-gallery-card .carousel__thumbs img) {
    width: 100%;
    height: auto;
    aspect-ratio: 4 / 3;
    object-fit: contain;
    background: #0b1220;
}
</style>