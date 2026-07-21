<template>
    <div class="page route-detail" v-loading="loading">
        <main class="constrained">
            <router-link :to="{ name: 'RouteCatalog' }" class="back-link">← 线路列表</router-link>

            <template v-if="routeInfo">
                <!-- Hero header: route number + stops -->
                <section class="hero">
                    <div class="hero__head">
                        <h1>{{ routeInfo.routeNumber }}</h1>
                        <div class="hero__tags">
                            <el-tag v-if="routeInfo.subType" size="small" type="warning">{{ subTypeLabel(routeInfo.subType) }}</el-tag>
                            <el-tag size="small" :type="routeInfo.isActive ? 'success' : 'info'">{{ routeInfo.isActive ? '运营中' : '已停运' }}</el-tag>
                        </div>
                    </div>
                    <div class="hero__stops">
                        <template v-if="routeInfo.isLoop">{{ routeInfo.startStop || '?' }} → {{ routeInfo.endStop || '?' }}</template>
                        <template v-else-if="routeInfo.downStartStop || routeInfo.downEndStop">
                            <div class="stop-line">上行：{{ routeInfo.startStop || '?' }} → {{ routeInfo.endStop || '?' }}</div>
                            <div class="stop-line">下行：{{ routeInfo.downStartStop || routeInfo.endStop || '?' }} → {{ routeInfo.downEndStop || routeInfo.startStop || '?' }}</div>
                        </template>
                        <template v-else>
                            {{ routeInfo.startStop || '?' }} ↔ {{ routeInfo.endStop || '?' }}
                        </template>
                    </div>
                    <!-- Variant routes -->
                    <div v-if="variantRoutes.length" class="hero__variants">
                        <div v-for="vr in variantRoutes" :key="vr.id" class="variant-line"
                             :class="{ 'variant-line--active': vr.id === routeInfo.id }">
                            <el-tag size="small" type="warning">{{ subTypeLabel(vr.subType) }}</el-tag>
                            <el-tag v-if="vr.isLoop" size="small" type="info">环线</el-tag>
                            <span>{{ vr.startStop || '?' }} ↔ {{ vr.endStop || '?' }}</span>
                        </div>
                    </div>
                </section>

                <!-- Body: left sidebar + right gallery -->
                <div class="body-layout">
                    <!-- Left: Info sidebar (single block) -->
                    <aside class="sidebar">
                        <div class="sidebar-block">
                            <h3>服务地区</h3>
                            <p>
                                <router-link v-if="routeInfo.region?.id" class="info-link"
                                    :to="{ name: 'RegionCatalog', params: { regionId: routeInfo.region.id } }">
                                    {{ routeInfo.region.name }}
                                </router-link>
                                <span v-else>-</span>
                            </p>
                        </div>

                        <div class="sidebar-block">
                            <h3>运营公司</h3>
                            <p>
                                <router-link v-if="routeInfo.company?.id" class="info-link"
                                    :to="{ name: 'CompanyCatalog', params: { companyId: routeInfo.company.id } }">
                                    {{ routeInfo.company.name }}
                                </router-link>
                                <span v-else>-</span>
                            </p>
                        </div>

                        <div class="sidebar-block" v-if="hasMetaInfo">
                            <h3>线路信息</h3>
                            <dl class="meta-dl">
                                <div v-if="routeInfo.lineLengthKm"><dt>里程</dt><dd>{{ routeInfo.lineLengthKm }} km</dd></div>
                                <div v-if="routeInfo.ticketType"><dt>票制</dt><dd>{{ ticketLabel(routeInfo.ticketType) }}</dd></div>
                                <div v-if="routeInfo.ticketPrice"><dt>票价</dt><dd>{{ routeInfo.ticketPrice }}</dd></div>
                                <div v-if="routeInfo.operatingHours"><dt>运营时间</dt><dd>{{ routeInfo.operatingHours }}</dd></div>
                                <div v-if="routeInfo.firstOperated"><dt>开通日期</dt><dd>{{ routeInfo.firstOperated }}</dd></div>
                            </dl>
                        </div>

                        <div class="sidebar-block" v-if="historicalModels.length">
                            <h3>曾用车型</h3>
                            <div class="model-chips">
                                <router-link v-for="m in historicalModels" :key="m.modelId" class="model-chip model-chip--link"
                                    :to="{ name: 'ModelCatalog', params: { modelId: m.modelId } }">
                                    {{ m.brandName }} {{ m.modelName }}
                                </router-link>
                            </div>
                        </div>

                        <div class="sidebar-block" v-if="routeInfo.remark">
                            <h3>备注</h3>
                            <p class="remark-text">{{ routeInfo.remark }}</p>
                        </div>
                    </aside>

                    <!-- Right: Photos by year -->
                    <div class="gallery">
                        <div v-if="!yearGroups.length" class="state">暂无车辆图片</div>
                        <div v-for="group in yearGroups" :key="group.year" class="year-group">
                            <h2 class="year-heading">{{ group.year }} <span class="count">({{ group.photos.length }})</span></h2>
                            <div class="photo-grid">
                                <div v-for="photo in group.photos" :key="photo.imageId" class="photo-card"
                                     @click="openPhotoDetail(photo.vehicleId, photo.imageId)">
                                    <img :src="photo.thumb || fallbackImg" :alt="photo.plate" loading="lazy" />
                                    <div class="photo-card__info">
                                        <strong>
                                            <span v-if="photo.routeNumber" class="photo-route-tag">{{ photo.routeNumber }}</span>
                                            {{ photo.plate }}
                                        </strong>
                                        <span>{{ photo.modelName }}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </template>

            <div v-else-if="!loading" class="state">线路不存在</div>
        </main>

        <VehicleDetailModal v-if="detailVisible" :visible="detailVisible" :detail="activeDetail"
            :loading="detailLoadingComputed" :initialImageId="activeImageId" @close="detailVisible = false; activeVehicleId = null" />
    </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { fetchRouteDetail } from '@/api/routes';
import { fetchRoutes } from '@/api/routes';
import http from '@/api/axiosInstance';
import placeholderBus from '@/assets/images/placeholder-bus.png';

const VehicleDetailModal = defineAsyncComponent(() => import('@/components/Gallery/VehicleDetailModal.vue'));

const $route = useRoute();
const store = useStore();
const loading = ref(true);
const routeInfo = ref(null);
const vehicles = ref([]);
const variantRoutes = ref([]);
const detailVisible = ref(false);
const activeVehicleId = ref(null);
const activeImageId = ref(null);
const fallbackImg = placeholderBus;

const activeDetail = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailMap[activeVehicleId.value] || null : null
);
const detailLoadingComputed = computed(() =>
    activeVehicleId.value ? store.state.vehicles.detailLoadingMap[activeVehicleId.value] || false : false
);

const SUB_TYPES = { INTERVAL: '区间', BRANCH: '支线', EXPRESS: '快线', NIGHT: '夜班', DIRECT: '直达' };
const ROUTE_TYPES = { REGULAR: '常规', BRT: '快速公交', AIRPORT: '机场', TOURIST: '旅游', COMMUNITY: '微循环', SUBWAY: '地铁接驳' };
const TICKETS = { FLAT: '一票制', SECTIONAL: '分段计价', FREE: '免费' };
const subTypeLabel = (v) => SUB_TYPES[v] || v;
const routeTypeLabel = (v) => ROUTE_TYPES[v] || v;
const ticketLabel = (v) => TICKETS[v] || v;

const hasMetaInfo = computed(() =>
    routeInfo.value?.lineLengthKm || routeInfo.value?.ticketType ||
    routeInfo.value?.ticketPrice || routeInfo.value?.operatingHours || routeInfo.value?.firstOperated
);

const historicalModels = computed(() => {
    const seen = new Set();
    return vehicles.value
        .map(v => ({ modelId: v.vehicle?.model?.id, modelName: v.vehicle?.model?.name, brandName: v.vehicle?.model?.brandChnName || v.vehicle?.model?.brandName }))
        .filter(m => m.modelId && !seen.has(m.modelId) && seen.add(m.modelId));
});

const extractYear = (img) => {
    const exif = img?.exif || {};
    const exifDate = exif['DateTimeOriginal'] || exif['DateTime'] || exif['DateCreated'];
    if (exifDate) {
        const m = exifDate.match(/^(\d{4})/);
        if (m) return m[1];
    }
    if (img?.createTime) return img.createTime.substring(0, 4);
    return '未知年份';
};

// Flatten all images across all vehicles (main + variants), tagged with routeNumber
const yearGroups = computed(() => {
    const map = {};
    vehicles.value.forEach(v => {
        const rn = v._routeNumber || '';
        (v.images || []).forEach(img => {
            const year = extractYear(img);
            if (!map[year]) map[year] = [];
            map[year].push({
                imageId: img.id,
                vehicleId: v.vehicle?.id,
                plate: v.vehicle?.plateNumber || '-',
                modelName: v.vehicle?.model?.name || '-',
                routeNumber: rn,
                thumb: img.thumbnailUrl || img.url,
                year
            });
        });
    });
    return Object.entries(map)
        .map(([year, photos]) => ({ year, photos }))
        .sort((a, b) => b.year.localeCompare(a.year));
});

const openPhotoDetail = async (vehicleId, imageId) => {
    if (!vehicleId) return;
    activeVehicleId.value = vehicleId;
    activeImageId.value = imageId || null;
    detailVisible.value = true;
    try {
        await store.dispatch('vehicles/loadVehicleDetail', { vehicleId, plateNumber: '', force: true });
    } catch (e) {
        console.error(e);
    }
};

onMounted(async () => {
    try {
        let detail = await fetchRouteDetail($route.params.routeId);

        // If this route is a variant (has parentRouteId), redirect to the parent
        if (detail?.parentRouteId) {
            $route.params.routeId = detail.parentRouteId;
            detail = await fetchRouteDetail(detail.parentRouteId);
        }
        routeInfo.value = detail;

        // Strip suffix (路/区间/支线 etc.) to get base number for keyword search,
        // so "880路" can find "880区间" via LIKE '%880%'
        const baseRouteKey = (detail.routeNumber || '').replace(/[路区间支线快线夜班直达线]$/g, '');

        const [vehicleList, allRoutesResp] = await Promise.all([
            http.get(`/routes/${detail.id}/vehicles`),
            fetchRoutes({ keyword: baseRouteKey, size: 50, isActive: undefined })
        ]);

        const mainVehicles = (Array.isArray(vehicleList) ? vehicleList : (vehicleList?.records || []))
            .map(v => { v._routeNumber = detail.routeNumber; return v; });
        vehicles.value = mainVehicles;

        // Find child routes (variants: interval, branch, etc.)
        // Match by: parentRouteId pointing to us, or same numeric prefix
        // IMPORTANT: only match within the same region AND company to avoid
        // cross-city collisions (e.g. Guangzhou 104路 ≠ Beijing 104路)
        const allRoutes = Array.isArray(allRoutesResp?.records) ? allRoutesResp.records : (Array.isArray(allRoutesResp) ? allRoutesResp : []);
        const detailRegionId = detail.region?.id;
        const detailCompanyId = detail.company?.id;
        const siblings = allRoutes.filter(r =>
            r.id !== detail.id && (
                r.parentRouteId === detail.id ||
                ((r.routeNumber || '').startsWith(baseRouteKey) &&
                 detailRegionId != null && r.region?.id === detailRegionId &&
                 detailCompanyId != null && r.company?.id === detailCompanyId)
            )
        );
        variantRoutes.value = siblings;

        // Fetch vehicles for each sibling route
        if (siblings.length) {
            const siblingVehicles = await Promise.all(
                siblings.map(async (sr) => {
                    try {
                        const vlist = await http.get(`/routes/${sr.id}/vehicles`);
                        const arr = Array.isArray(vlist) ? vlist : (vlist?.records || []);
                        const subLabel = sr.subType ? subTypeLabel(sr.subType) : '';
                        return arr.map(v => { v._routeNumber = sr.routeNumber + (subLabel && !sr.routeNumber.endsWith(subLabel) ? subLabel : ''); return v; });
                    } catch { return []; }
                })
            );
            vehicles.value = [...mainVehicles, ...siblingVehicles.flat()];
        }
    } catch (e) {
        console.error('Failed to load route', e);
    } finally {
        loading.value = false;
    }
});
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f5f7fb; }
.constrained { width: min(1100px, 100%); margin: 0 auto; padding: 24px 16px 72px; }
.back-link { color: #2563eb; text-decoration: none; font-size: 14px; display: inline-block; margin-bottom: 20px; }

// Hero
.hero {
    background: linear-gradient(135deg, #1e3a5f 0%, #2563eb 100%);
    border-radius: 20px; padding: 32px 36px; color: #fff; margin-bottom: 24px;
}
.hero__head { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 14px;
    h1 { margin: 0; font-size: 32px; font-weight: 800; }
}
.hero__tags { display: flex; gap: 6px; }
.hero__stops { font-size: 16px; opacity: 0.9;
    .stop-line + .stop-line { margin-top: 4px; }
}
.hero__variants { margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.2); }
.variant-line {
    display: flex; align-items: center; gap: 8px; font-size: 14px; opacity: 0.75; padding: 2px 0;
    &--active { opacity: 1; font-weight: 600; }
}

// Body layout
.body-layout { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 20px; align-items: start; }

// Sidebar
.sidebar {
    background: #fff; border-radius: 16px; padding: 22px;
    box-shadow: 0 4px 14px rgba(15,23,42,.05);
    display: flex; flex-direction: column; gap: 18px;
}
.sidebar-block {
    h3 { margin: 0 0 6px; font-size: 12px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.06em; }
    p { margin: 0; color: #0f172a; font-size: 15px; font-weight: 600; }
}
.meta-dl { margin: 0;
    div { display: flex; padding: 4px 0; font-size: 14px; }
    dt { color: #94a3b8; width: 72px; flex-shrink: 0; }
    dd { color: #0f172a; font-weight: 600; margin: 0; }
}
.model-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.model-chip { background: #eef2ff; color: #1d4ed8; padding: 4px 12px; border-radius: 999px; font-size: 13px; font-weight: 600; text-decoration: none;
    &--link:hover { background: #dbeafe; }
}
.remark-text { color: #475569; font-size: 14px; line-height: 1.6; font-weight: 400; }

// Gallery
.gallery { min-width: 0; }
.year-group { margin-bottom: 28px; }
.year-heading {
    color: #0f172a; font-size: 18px; margin: 0 0 12px; padding-bottom: 8px;
    border-bottom: 2px solid #e2e8f0;
    .count { color: #94a3b8; font-weight: 400; font-size: 14px; }
}
.photo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; }
.photo-card {
    background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer;
    box-shadow: 0 3px 10px rgba(15,23,42,.05); transition: transform .12s;
    &:hover { transform: translateY(-2px); }
}
.photo-card img { width: 100%; aspect-ratio: 16/10; object-fit: cover; display: block; background: #f3f4f6; }
.photo-card__info { padding: 8px 10px; display: flex; flex-direction: column; gap: 1px;
    strong { color: #111827; font-size: 13px; display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
    span { color: #6b7280; font-size: 12px; }
}
.photo-route-tag {
    color: #94a3b8; font-size: 10px; font-weight: 500; white-space: nowrap;
}

.info-link { color: #2563eb; text-decoration: none; font-weight: 600; &:hover { text-decoration: underline; } }
.state { text-align: center; padding: 60px 0; color: #94a3b8; }

@media (max-width: 768px) {
    .body-layout { grid-template-columns: 1fr; }
    .hero { padding: 20px; h1 { font-size: 24px; } }
}
</style>
