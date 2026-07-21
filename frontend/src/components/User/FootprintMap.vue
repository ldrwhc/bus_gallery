<template>
    <div class="footprint-wrapper">
        <div ref="mapRoot" class="footprint-map"></div>
        <div v-if="hoveredCity" class="map-tooltip" :style="tooltipStyle">{{ hoveredCity }}</div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue';
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
        const items = Array.isArray(data) ? data : [];
        const maxCount = Math.max(...items.map(d => d.count || 0), 1);

        const cityMap = new Map();
        items.forEach(d => {
            if (d.city) cityMap.set(d.city, { count: d.count || 0, thumbnail: d.thumbnail || '' });
        });

        const allCities = new Set(cityMap.keys());

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

                    poly.on('mouseover', (e) => {
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
                        const px = e.pixel;
                        tooltipStyle.left = (px.x + 12) + 'px';
                        tooltipStyle.top = (px.y - 10) + 'px';
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
                        map.panBy(0, -(px.y - map.getSize().getHeight() / 2 + 70));
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
    overflow: hidden; background: #f1f5f9;
}
.map-tooltip {
    position: absolute; z-index: 999; pointer-events: none;
    background: rgba(0,0,0,0.78); color: #fff; font-size: 12px;
    padding: 4px 10px; border-radius: 6px; white-space: nowrap;
    transform: translateY(-100%);
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
