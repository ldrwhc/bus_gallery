<template>
    <div ref="mapRoot" class="footprint-map"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { fetchUserFootprint } from '@/api/footprint';

const AMAP_KEY = 'aaf4d758c8a4864e319ba239c71e8e0c';
const AMAP_JSCODE = '95b3b7300974a8f43938eb662203eea1';

const props = defineProps({
    userId: { type: Number, required: true }
});

const mapRoot = ref(null);
let map = null;
let loaded = false;
const polyMetaMap = new Map(); // polygon → { city, count, thumbnail, color }
let infoWindow = null;
let hoveredPolygon = null;

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

const showInfoWindow = (polygon, meta) => {
    if (infoWindow) {
        map.remove(infoWindow);
    }
    const thumbHtml = meta.thumbnail
        ? `<img src="${meta.thumbnail}" style="width:200px;height:auto;max-height:130px;object-fit:cover;border-radius:8px;margin-bottom:6px;display:block;" />`
        : '';
    const content = `<div style="min-width:180px;padding:4px;">
        <strong style="font-size:14px;">${meta.city}</strong>
        <span style="color:#94a3b8;font-size:12px;margin-left:6px;">${meta.count} 张</span>
        ${thumbHtml}
    </div>`;
    infoWindow = new window.AMap.InfoWindow({
        content,
        offset: new window.AMap.Pixel(0, -10),
        closeWhenClickMap: true
    });
    infoWindow.open(map, polygon.getBounds().getCenter());
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
        const items = Array.isArray(data) ? data : [];
        const maxCount = Math.max(...items.map(d => d.count || 0), 1);

        const cityMap = new Map();
        items.forEach(d => {
            if (d.city) cityMap.set(d.city, { count: d.count || 0, thumbnail: d.thumbnail || '' });
        });

        const allCities = new Set(cityMap.keys());
        const paintedCities = new Set();

        for (const city of allCities) {
            const result = await new Promise((resolve) => {
                district.search(city, (status, res) => {
                    if (status !== 'complete' || !res.districtList?.length) {
                        resolve(null);
                        return;
                    }
                    const d = res.districtList[0];
                    if (!d.boundaries) { resolve(null); return; }
                    resolve({ name: d.name || city, boundaries: d.boundaries });
                });
            });
            if (result) {
                paintedCities.add(result.name);
                const meta = cityMap.get(city);
                const color = resolveColor(meta.count, maxCount);
                const polygons = [];
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
                    });
                    poly.on('mouseout', () => {
                        if (hoveredPolygon === poly) hoveredPolygon = null;
                        poly.setOptions({
                            fillColor: color.fill,
                            strokeColor: color.stroke,
                            strokeWeight: 0.8
                        });
                    });
                    poly.on('click', (e) => {
                        showInfoWindow(poly, { city: result.name, ...meta });
                    });
                    polygons.push(poly);
                });
                map.add(polygons);
            }
        }

        if (paintedCities.size > 0) {
            map.setFitView(null, false, [60, 60, 60, 60]);
        }

        map.on('click', () => {
            if (infoWindow) {
                map.remove(infoWindow);
                infoWindow = null;
            }
        });
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
    infoWindow = null;
    hoveredPolygon = null;
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
.footprint-map {
    width: 100%;
    height: 360px;
    border-radius: 14px;
    overflow: hidden;
    background: #f1f5f9;
}
</style>
