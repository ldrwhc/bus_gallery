<template>
    <div class="footprint-wrapper">
        <div v-if="stats" class="footprint-stats">
            <div class="stat-item">
                <span class="stat-num">{{ stats.reachedCities }}</span>
                <span class="stat-text">到达城市</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item stat-progress-item">
                <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
                </div>
                <span class="stat-text">{{ stats.reachedCities }} / {{ stats.totalCities }} 城</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
                <span class="stat-num">#{{ stats.rank }}</span>
                <span class="stat-text">全站排名 / {{ stats.totalUsers }} 人</span>
            </div>
        </div>
        <div ref="mapRoot" class="footprint-map">
            <div v-if="hoveredCity" class="map-tooltip" :style="tooltipStyle">{{ hoveredCity }}</div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { fetchUserFootprint } from '@/api/footprint';

const AMAP_KEY = 'aaf4d758c8a4864e319ba239c71e8e0c';
const AMAP_JSCODE = '95b3b7300974a8f43938eb662203eea1';

const props = defineProps({
    userId: { type: Number, required: true }
});

const mapRoot = ref(null);
let map = null;
let loaded = false;
const polyMetaMap = new Map();
let hoveredPolygon = null;
let infoWindow = null;

const hoveredCity = ref('');
const tooltipStyle = reactive({ left: '0px', top: '0px' });
const stats = ref(null);
const progressPct = computed(() => {
    if (!stats.value || !stats.value.totalCities) return 0;
    return Math.round((stats.value.reachedCities / stats.value.totalCities) * 100);
});

const loadAMap = () => {
    return new Promise((resolve, reject) => {
        if (window.AMap) return resolve(window.AMap);
        const existing = document.querySelector('script[src*="webapi.amap.com"]');
        if (existing) {
            existing.addEventListener('load', () => resolve(window.AMap));
            return;
        }
        window._AMapSecurityConfig = { securityJsCode: AMAP_JSCODE };
        const script = document.createElement('script');
        script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.DistrictSearch`;
        script.onload = () => resolve(window.AMap);
        script.onerror = reject;
        document.head.appendChild(script);
    });
};

const resolveColor = (count, maxCount) => {
    if (!count || maxCount === 0) return { fill: 'rgba(200,200,200,0.18)', stroke: '#ccc' };
    const ratio = maxCount > 0 ? count / maxCount : 0;
    const r = 220, g = Math.round(50 + 50 * (1 - ratio)), b = Math.round(80 + 80 * (1 - ratio));
    return {
        fill: `rgba(${r},${g},${b},${0.15 + ratio * 0.55})`,
        stroke: `rgba(${r - 40},${Math.max(0, g - 40)},${Math.max(0, b - 40)},0.85)`
    };
};

const resolveThumbUrl = (path) => {
    if (!path) return '';
    if (/^https?:\/\//i.test(path)) return path;
    return window.location.origin + '/' + path.replace(/^\//, '');
};

const buildMap = async () => {
    if (!mapRoot.value) return;
    await loadAMap();

    map = new window.AMap.Map(mapRoot.value, {
        zoom: 4,
        center: [108, 36],
        mapStyle: 'amap://styles/light',
        resizeEnable: true,
        dragEnable: true,
        zoomEnable: true,
        doubleClickZoom: false,
        keyboardEnable: false,
        scrollWheel: true
    });

    const district = new window.AMap.DistrictSearch({
        level: 'city',
        extensions: 'all',
        subdistrict: 0
    });

    try {
        const data = await fetchUserFootprint(props.userId);
        stats.value = { reachedCities: data.reachedCities || 0, totalCities: data.totalCities || 0, rank: data.rank || 0, totalUsers: data.totalUsers || 0 };
        const items = Array.isArray(data.cities) ? data.cities : [];
        const maxCount = Math.max(...items.map(d => d.count || 0), 1);

        const cityMap = new Map();
        items.forEach(d => {
            if (d.city) cityMap.set(d.city, { count: d.count || 0, thumbnail: d.thumbnail || '' });
        });

        const allCities = new Set(cityMap.keys());

        const searchCity = (name) => new Promise((resolve) => {
            district.search(name, (status, res) => {
                if (status === 'complete' && res.districtList?.length) {
                    const d = res.districtList[0];
                    if (d.boundaries && d.boundaries.length) {
                        resolve({ name: d.name || name, boundaries: d.boundaries });
                        return;
                    }
                }
                resolve(null);
            });
        });

        for (const city of allCities) {
            let result = await searchCity(city);
            if (!result) {
                const short = city.replace(/[市州地区盟]$/, '');
                if (short !== city) result = await searchCity(short);
            }
            if (result) {
                const meta = cityMap.get(city);
                const color = resolveColor(meta.count, maxCount);
                result.boundaries.forEach(boundary => {
                    const poly = new window.AMap.Polygon({
                        path: boundary,
                        fillColor: color.fill,
                        fillOpacity: 1,
                        strokeColor: color.stroke,
                        strokeWeight: 0.8,
                        strokeStyle: 'solid',
                        cursor: 'pointer'
                    });
                    polyMetaMap.set(poly, { city: result.name, ...meta, color });

                    poly.on('mouseover', () => {
                        if (hoveredPolygon && hoveredPolygon !== poly) {
                            const prev = polyMetaMap.get(hoveredPolygon);
                            hoveredPolygon.setOptions({
                                fillColor: prev.color.fill,
                                strokeColor: prev.color.stroke,
                                strokeWeight: 0.8
                            });
                        }
                        hoveredPolygon = poly;
                        poly.setOptions({
                            fillColor: 'rgba(255,82,119,0.75)',
                            strokeColor: '#ff3860',
                            strokeWeight: 2
                        });
                        hoveredCity.value = result.name;
                    });

                    poly.on('mousemove', (e) => {
                        tooltipStyle.left = (e.pixel.x + 8) + 'px';
                        tooltipStyle.top = (e.pixel.y - 20) + 'px';
                    });

                    poly.on('mouseout', () => {
                        if (hoveredPolygon === poly) hoveredPolygon = null;
                        poly.setOptions({
                            fillColor: color.fill,
                            strokeColor: color.stroke,
                            strokeWeight: 0.8
                        });
                        hoveredCity.value = '';
                    });

                    poly.on('click', () => {
                        if (infoWindow) { map.remove(infoWindow); }
                        const center = poly.getBounds().getCenter();
                        const thumbUrl = resolveThumbUrl(meta.thumbnail);
                        const imgTag = thumbUrl
                            ? `<div class="info-thumb-wrap"><img src="${thumbUrl}" /></div>`
                            : '';
                        infoWindow = new window.AMap.InfoWindow({
                            content: `<div class="info-popup">
                                <div class="info-title">${result.name}<span class="info-count">${meta.count} 张</span></div>
                                ${imgTag}
                            </div>`,
                            offset: new window.AMap.Pixel(0, -6),
                            closeWhenClickMap: true
                        });
                        infoWindow.open(map, center);
                        const px = map.lngLatToContainer(center);
                        const targetY = map.getSize().getHeight() * 0.55;
                        map.panBy(0, targetY - px.y);
                    });

                    map.add(poly);
                });
            }
        }

        if (allCities.size > 0) {
            map.setFitView(null, false, [60, 60, 60, 60]);
        }
    } catch (err) {
        console.error('Footprint map load failed:', err);
    }
};

const destroyMap = () => {
    if (map) {
        map.destroy();
        map = null;
    }
    polyMetaMap.clear();
    hoveredPolygon = null;
    hoveredCity.value = '';
    if (infoWindow) { infoWindow = null; }
};

onMounted(() => {
    if (!loaded) {
        loaded = true;
        buildMap();
    }
});

watch(() => props.userId, () => {
    destroyMap();
    buildMap();
});

onBeforeUnmount(() => {
    destroyMap();
});
</script>

<style scoped>
.footprint-wrapper { position: relative; }
.footprint-map {
    width: 100%; height: 360px; border-radius: 14px;
    background: #f1f5f9;
}
.map-tooltip {
    position: absolute; z-index: 999; pointer-events: none;
    background: rgba(0,0,0,0.78); color: #fff; font-size: 12px;
    padding: 4px 10px; border-radius: 6px; white-space: nowrap;
    transform: translate(-50%, -100%);
}
.footprint-stats {
    display: flex; align-items: center; gap: 0;
    background: #fff; border-radius: 14px 14px 0 0;
    padding: 12px 18px; border-bottom: 1px solid #e2e8f0;
}
.stat-item { display: flex; flex-direction: column; align-items: center; gap: 2px; flex: 1; }
.stat-num { font-size: 20px; font-weight: 800; color: #0f172a; line-height: 1.1; }
.stat-text { font-size: 11px; color: #94a3b8; white-space: nowrap; }
.stat-divider { width: 1px; height: 36px; background: #e2e8f0; flex-shrink: 0; }
.stat-progress-item { flex: 2; padding: 0 8px; }
.progress-bar { width: 100%; height: 6px; background: #e2e8f0; border-radius: 3px; overflow: hidden; margin-bottom: 4px; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #f97316, #ef4444); border-radius: 3px; transition: width .4s ease; }

@media (max-width: 560px) {
    .footprint-stats { padding: 10px 12px; }
    .stat-num { font-size: 17px; }
    .stat-text { font-size: 10px; }
    .footprint-map { height: 260px; }
}

@media (max-width: 400px) {
    .footprint-stats { flex-wrap: wrap; gap: 8px; }
    .stat-divider:first-of-type { display: none; }
    .stat-progress-item { flex: 1 1 100%; order: 1; }
}
</style>

<style>
.info-popup { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; padding: 2px 4px 6px; }
.info-title { font-size: 14px; font-weight: 700; color: #0f172a; margin-bottom: 6px; }
.info-count { font-weight: 400; font-size: 12px; color: #94a3b8; margin-left: 6px; }
.info-thumb-wrap { border-radius: 10px; overflow: hidden; }
.info-thumb-wrap img { width: 200px; height: 130px; object-fit: cover; display: block; }
.amap-info-content { padding: 8px 10px !important; background: #fff !important; border-radius: 14px !important; box-shadow: 0 8px 30px rgba(0,0,0,0.18) !important; }
.amap-info-sharp { border-top-color: #fff !important; }
</style>
