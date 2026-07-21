<template>
  <div class="stats-root">
    <div v-if="data" class="stats-body">
      <!-- Hero -->
      <header class="hero">
        <h1 class="hero__title">数据统计中心</h1>
        <p class="hero__sub">Bus Gallery 全站数据之最与记录汇总</p>
        <div class="ov-grid">
          <div
            class="ov-card"
            v-for="card in overviewCards"
            :key="card._key"
            @mouseenter="card._hover = true"
            @mouseleave="card._hover = false"
          >
            <span class="ov-card__icon">{{ card.icon }}</span>
            <span class="ov-card__value">{{ card._display }}</span>
            <span class="ov-card__label">{{ card.label }}</span>
            <Transition name="tip">
              <div class="ov-card__tip" v-if="card._hover">
                <template v-for="(line, li) in card._tipLines" :key="li">
                  {{ line }}<br v-if="li < card._tipLines.length - 1" />
                </template>
              </div>
            </Transition>
          </div>
        </div>
      </header>

      <!-- Sections -->
      <section class="sec" v-for="sec in sections" :key="sec.id">
        <div class="sec__head" :style="{ '--accent': sec.color }">
          <h2 class="sec__title">{{ sec.title }}</h2>
          <span class="sec__desc">{{ sec.desc }}</span>
        </div>
        <div class="sec__grid">
          <template v-for="card in sec.cards" :key="card.title">
            <!-- Donut card -->
            <div class="card" v-if="card._isDonut">
              <h3 class="card__title">{{ card.title }}</h3>
              <div class="donut-wrap" v-if="card._total > 0">
                <svg class="donut-svg" viewBox="0 0 120 120">
                  <circle cx="60" cy="60" r="42" fill="none" stroke="#f1f5f9" stroke-width="16"/>
                  <template v-for="(seg, si) in card._segments" :key="si">
                    <circle cx="60" cy="60" r="42" fill="none"
                      :stroke="seg.color" stroke-width="16"
                      :stroke-dasharray="seg.dashArray"
                      :stroke-dashoffset="seg.dashOffset"
                      stroke-linecap="butt"
                      transform="rotate(-90 60 60)"/>
                  </template>
                </svg>
                <div class="donut-center">
                  <span class="donut-center__val">{{ card._total.toLocaleString() }}</span>
                  <span class="donut-center__lbl">总计</span>
                </div>
              </div>
              <div class="donut-legend">
                <div
                  class="legend-row"
                  v-for="seg in card._segments"
                  :key="seg.name"
                  @mouseenter="seg._hover = true"
                  @mouseleave="seg._hover = false"
                >
                  <span class="legend-dot" :style="{ background: seg.color }"></span>
                  <span class="legend-name">{{ seg.name }}</span>
                  <span class="legend-val">{{ seg.cnt.toLocaleString() }}</span>
                  <span class="legend-pct">{{ seg.pct }}%</span>
                  <Transition name="tip">
                    <div class="legend-tip" v-if="seg._hover">
                      共 {{ seg.cnt.toLocaleString() }} 辆，占 {{ seg.pct }}%
                    </div>
                  </Transition>
                </div>
              </div>
              <div v-if="card._total === 0" class="empty">暂无数据</div>
            </div>

            <!-- Image card -->
            <div class="card" v-else-if="card._isImageCard">
              <h3 class="card__title">{{ card.title }}</h3>
              <div class="img-list">
                <div
                  class="img-row"
                  v-for="item in card._imageItems"
                  :key="item.id"
                  @mouseenter="item._hover = true"
                  @mouseleave="item._hover = false"
                >
                  <img v-if="item._thumb" :src="item._thumb" class="img-thumb" loading="lazy" />
                  <div v-else class="img-thumb img-thumb--empty"></div>
                  <div class="img-info">
                    <span class="img-info__plate">{{ item._label }}</span>
                    <span class="img-info__model" v-if="item._sublabel">{{ item._sublabel }}</span>
                    <span class="img-info__meta">{{ item._uploader }} · {{ item._time }}</span>
                  </div>
                  <Transition name="tip">
                    <div class="rank-tip" v-if="item._hover">
                      车牌：{{ item._label }}<br v-if="item._sublabel" />
                      车型：{{ item._sublabel || '未知' }}<br />
                      上传者：{{ item._uploader }}<br />
                      时间：{{ item._time }}
                    </div>
                  </Transition>
                </div>
              </div>
            </div>

            <!-- Rank card -->
            <div class="card" v-else>
              <h3 class="card__title">{{ card.title }}</h3>
              <div class="rank-list">
                <div
                  :class="['rank-row', { 'has-link': card._linkType }]"
                  v-for="(item, i) in card._items"
                  :key="i"
                  @mouseenter="item._hover = true"
                  @mouseleave="item._hover = false"
                  @click="handleItemClick(card._linkType, item)"
                >
                  <span class="rank-idx">
                    <span v-if="i === 0" class="rank-medal gold">●</span>
                    <span v-else-if="i === 1" class="rank-medal silver">●</span>
                    <span v-else-if="i === 2" class="rank-medal bronze">●</span>
                    <span v-else class="rank-num">{{ i + 1 }}</span>
                  </span>
                  <span class="rank-label">
                    <span class="rank-label__main">{{ item._label }}</span>
                    <span class="rank-label__sub" v-if="item._sublabel">{{ item._sublabel }}</span>
                  </span>
                  <span class="rank-bar" v-if="item._barPct !== null">
                    <span class="rank-bar__fill" :style="{ width: item._barPct, background: sec.color }"></span>
                  </span>
                  <span class="rank-val">{{ item._val }}</span>
                  <span v-if="card._linkType" class="rank-chevron">›</span>
                  <Transition name="tip">
                    <div class="rank-tip" v-if="item._hover">
                      <template v-for="(line, li) in item._tipLines" :key="li">
                        {{ line }}<br v-if="li < item._tipLines.length - 1" />
                      </template>
                    </div>
                  </Transition>
                </div>
              </div>
              <div v-if="!card._items.length" class="empty">暂无数据</div>
            </div>
          </template>
        </div>
      </section>
    </div>

    <!-- Loading -->
    <div class="state" v-if="loading">
      <div class="spinner"></div>
      <p>正在加载统计数据…</p>
    </div>

    <!-- Error -->
    <div class="state state--err" v-if="error">
      <p>{{ error }}</p>
      <el-button @click="loadStats">重新加载</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchStats } from '@/api/stats';

const data = ref(null);
const loading = ref(true);
const error = ref(null);

const DONUT_COLORS = ['#2563eb','#10b981','#f59e0b','#8b5cf6','#ef4444','#06b6d4','#ec4899','#84cc16','#f97316','#6366f1'];
const overviewCards = reactive([]);
const sections = reactive([]);

// ---- format ----
const fmt = n => n != null ? Number(n).toLocaleString() : '0';
const router = useRouter();

// ---- link resolver ----
const resolveLink = (linkType, item) => {
  const id = item?.id;
  if (!id) return null;
  const map = {
    model:   { name: 'ModelCatalog',   params: { modelId: id } },
    brand:   { name: 'BrandCatalog',   params: { brandId: id } },
    route:   { name: 'RouteDetail',    params: { routeId: id } },
    company: { name: 'CompanyCatalog', params: { companyId: id } },
    region:  { name: 'RegionCatalog',  params: { regionId: id } },
    user:    { name: 'UserProfile',    params: { userId: id } },
    vehicle: { name: 'SearchResults',  query: { keyword: item.plateNumber || item.name || '' } }
  };
  return map[linkType] || null;
};

const handleItemClick = (linkType, item) => {
  const link = resolveLink(linkType, item);
  if (link) router.push(link);
};

const formatTime = t => {
  if (!t) return '-';
  try { const d = new Date(t); if (isNaN(d.getTime())) return String(t).replace('T',' ').substring(0,16); return d.toLocaleString('zh-CN',{hour12:false}); } catch { return t; }
};

// ---- build tip lines that are actually interesting ----
const makeOverviewCards = (ov, d) => {
  const totVeh = ov.totalVehicles || 1;
  const totImg = ov.totalImages || 1;
  const brandCnt = ov.totalBrands;
  const regionCnt = ov.totalRegions;
  const topBrand = d.mostVehiclesByBrand?.[0];
  const topRegion = d.mostVehiclesByRegion?.[0];
  const topModel = d.mostVehiclesByModel?.[0];
  const topUploader = d.mostUploadingUsers?.[0];
  const topRouteRegion = d.mostRoutesByRegion?.[0];
  const topCoRegion = d.mostCompaniesByRegion?.[0];
  const avgImgPerVeh = (totImg / totVeh).toFixed(1);
  const avgImgSize = ov.totalImageSizeBytes > 0
    ? formatBytes(ov.totalImageSizeBytes / totImg) : '0 B';

  const defs = [
    { _key:'veh', icon:'🚌', label:'车辆', val:ov.totalVehicles,
      _tipLines: [
        `收录 ${brandCnt} 个客车品牌，覆盖 ${regionCnt} 个城市`,
        topBrand ? `车辆最多的品牌：${topBrand.name}（${topBrand.subName || ''}）${fmt(topBrand.cnt)} 辆` : '',
        topModel ? `车辆最多的车型：${topModel.name} ${fmt(topModel.cnt)} 辆` : ''
      ].filter(Boolean) },
    { _key:'img', icon:'🖼️', label:'图片', val:ov.totalImages,
      _tipLines: [
        `平均每辆车 ${avgImgPerVeh} 张照片`,
        `平均每张图片 ${avgImgSize}`,
        topUploader ? `上传最多：${topUploader.name}（${fmt(topUploader.cnt)} 张）` : ''
      ].filter(Boolean) },
    { _key:'mod', icon:'🚛', label:'车型', val:ov.totalModels,
      _tipLines: [
        `覆盖 ${brandCnt} 个品牌`,
        topModel ? `最常见的车型：${topModel.name}（${topModel.subName || ''}）` : '',
        `平均每个品牌 ${(ov.totalModels / Math.max(brandCnt,1)).toFixed(1)} 种车型`
      ] },
    { _key:'bnd', icon:'🏭', label:'品牌', val:ov.totalBrands,
      _tipLines: [
        topBrand ? `收录最多的是 ${topBrand.name}${topBrand.subName ? '（'+topBrand.subName+'）' : ''}` : '',
        topBrand ? `该品牌下有 ${fmt(topBrand.cnt)} 辆车` : ''
      ].filter(Boolean) },
    { _key:'com', icon:'🏢', label:'公司', val:ov.totalCompanies,
      _tipLines: [
        `分布在全国 ${regionCnt} 个城市`,
        topCoRegion ? `公司最多的城市：${topCoRegion.name}（${fmt(topCoRegion.cnt)} 家）` : ''
      ].filter(Boolean) },
    { _key:'reg', icon:'📍', label:'地区', val:ov.totalRegions,
      _tipLines: [
        topRegion ? `车辆最多的地区：${topRegion.name}｜${fmt(topRegion.cnt)} 辆` : '',
        topRouteRegion ? `线路最多的地区：${topRouteRegion.name}｜${fmt(topRouteRegion.cnt)} 条` : ''
      ].filter(Boolean) },
    { _key:'rt', icon:'🛣️', label:'线路', val:ov.totalRoutes,
      _tipLines: [
        topRouteRegion ? `线路最多的城市：${topRouteRegion.name}（${fmt(topRouteRegion.cnt)} 条）` : '',
        `覆盖 ${regionCnt} 个城市`
      ].filter(Boolean) },
    { _key:'usr', icon:'👤', label:'用户', val:ov.totalUsers,
      _tipLines: [
        topUploader ? `贡献最多的用户：${topUploader.name}` : '',
        topUploader ? `TA 上传了 ${fmt(topUploader.cnt)} 张图片` : ''
      ].filter(Boolean) },
    { _key:'siz', icon:'💾', label:'图片总大小', val:ov.totalImageSizeFormatted,
      _tipLines: [
        `共 ${fmt(ov.totalImageSizeBytes)} 字节`,
        `平均每张 ${avgImgSize}`
      ] }
  ];
  overviewCards.length = 0;
  defs.forEach((c, i) => {
    const item = reactive({ ...c, _display: '0', _hover: false });
    overviewCards.push(item);
    setTimeout(() => { item._display = typeof c.val === 'string' ? c.val : c.val.toLocaleString(); }, 60 + i * 50);
  });
};

function formatBytes(bytes) {
  if (!bytes || bytes < 1024) return (bytes||0) + ' B';
  const kb = bytes / 1024;
  if (kb < 1024) return kb.toFixed(0) + ' KB';
  const mb = kb / 1024;
  if (mb < 1024) return mb.toFixed(1) + ' MB';
  return (mb / 1024).toFixed(2) + ' GB';
}

// ---- rank items with smart tooltips ----
const makeRankItems = (raw, opts) => {
  const { labelKey, subKey, valFn, unit, tipBuilder } = opts;
  if (!raw?.length) return [];
  // filter out items with zero/empty counts to avoid ghost bars
  const filtered = raw.filter(r => {
    const v = valFn(r);
    return v != null && v > 0;
  });
  if (!filtered.length) return [];
  const maxVal = Math.max(...filtered.map(v => valFn(v)), 1);
  return filtered.map(r => {
    const val = valFn(r);
    const barPct = val > 0 ? (val / maxVal * 100).toFixed(1) + '%' : null;
    const tipLines = tipBuilder ? tipBuilder(r, val, maxVal) : [
      `${r._label || r.name || r.plateNumber}: ${fmt(val)}${unit || ''}`
    ];
    return reactive({
      ...r,
      _label: (r[labelKey] || r.name || r.plateNumber || '-'),
      _sublabel: subKey ? (r[subKey] || r.subName || r.modelName || '') : '',
      _val: fmt(val) + (unit ? ' ' + unit : ''),
      _barPct: barPct,
      _hover: false,
      _tipLines: tipLines
    });
  });
};

const buildSections = (d) => {
  const ov = d.overview;
  const totImages = ov.totalImages || 1;
  const totVehicles = ov.totalVehicles || 1;
  const totRoutes = ov.totalRoutes || 1;

  sections.length = 0;
  sections.push(
    {
      id:'img', title:'图片之最', desc:'Top 10 · 点击名称可跳转', color:'#2563eb',
      cards:[
        { title:'图片最多的车型', _linkType:'model', _items: makeRankItems(d.mostImagedModels, {
            labelKey:'name', subKey:'subName', valFn:r=>r.cnt, unit:'张',
            tipBuilder:(r,v)=>[
              `${r.name}｜品牌：${r.subName || '未知'}`,
              `共 ${fmt(v)} 张照片，占全站图片的 ${(v/totImages*100).toFixed(1)}%`
            ]}) },
        { title:'图片最多的车辆', _linkType:'vehicle', _items: makeRankItems(d.mostImagedVehicles, {
            labelKey:'name', subKey:'subName', valFn:r=>r.cnt, unit:'张',
            tipBuilder:(r,v)=>[
              `车牌：${r.name}｜车型：${r.subName || '未知'}`,
              `共 ${fmt(v)} 张照片，占全站 ${(v/totImages*100).toFixed(1)}%`
            ]}) },
        { title:'图片最多的线路', _linkType:'route', _items: makeRankItems(d.mostImagedRoutes, {
            labelKey:'name', subKey:null, valFn:r=>r.cnt, unit:'张',
            tipBuilder:(r,v)=>[
              `线路：${r.name}`,
              `共 ${fmt(v)} 张沿途实拍，占全站 ${(v/totImages*100).toFixed(1)}%`
            ]}) },
        { title:'上传最多的用户', _linkType:'user', _items: makeRankItems(d.mostUploadingUsers, {
            labelKey:'name', subKey:null, valFn:r=>r.cnt, unit:'张',
            tipBuilder:(r,v)=>[
              `用户：${r.name || '未知'}`,
              `贡献了 ${fmt(v)} 张照片，占全站 ${(v/totImages*100).toFixed(1)}%`
            ]}) }
      ]},
    {
      id:'model', title:'车型与品牌之最', desc:'Top 10 · 点击名称可跳转', color:'#10b981',
      cards:[
        { title:'车辆最多的车型', _linkType:'model', _items: makeRankItems(d.mostVehiclesByModel, {
            labelKey:'name', subKey:'subName', valFn:r=>r.cnt, unit:'辆',
            tipBuilder:(r,v)=>[
              `${r.name}｜品牌：${r.subName || '未知'}`,
              `该车型共 ${fmt(v)} 辆，占全站车辆的 ${(v/totVehicles*100).toFixed(1)}%`
            ]}) },
        { title:'车辆最多的品牌', _linkType:'brand', _items: makeRankItems(d.mostVehiclesByBrand, {
            labelKey:'name', subKey:'subName', valFn:r=>r.cnt, unit:'辆',
            tipBuilder:(r,v)=>[
              `${r.name}${r.subName ? '（'+r.subName+'）' : ''}`,
              `该品牌共 ${fmt(v)} 辆车，占全站 ${(v/totVehicles*100).toFixed(1)}%`
            ]}) },
        { title:'车型种类最多的公司', _linkType:'company', _items: makeRankItems(d.mostModelVarietyByCompany, {
            labelKey:'name', subKey:null, valFn:r=>r.cnt, unit:'种',
            tipBuilder:(r,v)=>[
              `${r.name}`,
              `旗下运营 ${fmt(v)} 种不同车型`
            ]}) },
        { title:'车型种类最多的线路', _linkType:'route', _items: makeRankItems(d.mostModelVarietyByRoute, {
            labelKey:'name', subKey:null, valFn:r=>r.cnt, unit:'种',
            tipBuilder:(r,v)=>[
              `线路：${r.name}`,
              `该线路共有 ${fmt(v)} 种不同车型运营`
            ]}) }
      ]},
    {
      id:'region', title:'地区之最', desc:'Top 10 · 点击名称可跳转', color:'#8b5cf6',
      cards:[
        { title:'车辆最多的地区', _linkType:'region', _items: makeRankItems(d.mostVehiclesByRegion, {
            labelKey:'name', subKey:null, valFn:r=>r.cnt, unit:'辆',
            tipBuilder:(r,v)=>[`${r.name}：${fmt(v)} 辆车，占全站 ${(v/totVehicles*100).toFixed(1)}%`]}) },
        { title:'线路最多的地区', _linkType:'region', _items: makeRankItems(d.mostRoutesByRegion, {
            labelKey:'name', subKey:null, valFn:r=>r.cnt, unit:'条',
            tipBuilder:(r,v)=>[`${r.name}：${fmt(v)} 条线路，占全站 ${(v/totRoutes*100).toFixed(1)}%`]}) },
        { title:'公司最多的地区', _linkType:'region', _items: makeRankItems(d.mostCompaniesByRegion, {
            labelKey:'name', subKey:null, valFn:r=>r.cnt, unit:'家',
            tipBuilder:(r,v)=>[`${r.name}：${fmt(v)} 家公交公司`]}) }
      ]},
    {
      id:'config', title:'配置统计', desc:'分布', color:'#f59e0b',
      cards:[
        buildDonut('燃料类型分布', d.fuelTypeDistribution, '辆'),
        buildDonut('空调车比例', d.acDistribution, '辆'),
        buildDonut('踏步类型分布', d.stepTypeDistribution, '辆')
      ]},
    {
      id:'time', title:'时间之最', desc:'记录 · 点击名称可跳转', color:'#06b6d4',
      cards:[
        { ...buildTimeCard('最老车辆 Top 10', d.oldestVehicles, '出厂'), _linkType:'vehicle' },
        { ...buildTimeCard('最新车辆 Top 10', d.newestVehicles, '上线'), _linkType:'vehicle' },
        buildYearCard('出厂年份分布', d.factoryYearDistribution)
      ]},
    {
      id:'latest', title:'最新上传', desc:'最近 10 张图片', color:'#ef4444',
      cards:[
        buildLatestImagesCard(d.latestImages)
      ]}
  );
};

const buildDonut = (title, items, unit) => {
  const total = (items||[]).reduce((s,i)=>s+(i.cnt||0), 0);
  if (total === 0) return { title, _isDonut:true, _total:0, _segments:[] };
  let cum = 0;
  const circ = 2*Math.PI*42;
  const segs = (items||[]).filter(it => it.cnt > 0).map((it, i) => {
    const pct = total>0 ? (it.cnt/total) : 0;
    const len = pct*circ;
    const seg = { name:it.name||'未知', cnt:it.cnt, pct:(pct*100).toFixed(1),
      color:DONUT_COLORS[i%DONUT_COLORS.length],
      dashArray:`${len} ${circ-len}`, dashOffset:-cum, _hover:false };
    cum += len; return seg;
  });
  return { title, _isDonut:true, _total:total, _segments:segs };
};

const buildTimeCard = (title, items, dateLabel) => ({
  title,
  _items: makeRankItems(items, {
    labelKey:'plateNumber', subKey:'modelName', valFn:()=>1, unit:'',
    tipBuilder:(r)=>{
      const lines = [`车牌：${r.plateNumber}｜车型：${r.modelName}`];
      if (r.factoryDate) lines.push(`出厂：${r.factoryDate}`);
      if (r.launchDate) lines.push(`上线：${r.launchDate}`);
      return lines;
    }}).map((it,i)=>({...it, _val:it.dateVal, _barPct:null})),
  _unitLabel: dateLabel+'日期'
});

const buildLatestImagesCard = (items) => ({
  title: '最新上传图片',
  _isImageCard: true,
  _imageItems: (items || []).map(r => ({
    ...r,
    _label: r.plateNumber || '无车牌',
    _sublabel: r.modelName || '',
    _time: formatTime(r.createdAt),
    _uploader: r.uploaderName || '未知',
    _thumb: r.thumbnailUrl || r.url || '',
    _hover: false
  }))
});

const buildYearCard = (title, items) => {
  const filtered = (items||[]).filter(y => y.cnt > 0);
  if (!filtered.length) return { title, _items: [] };
  const maxV = Math.max(...filtered.map(y=>y.cnt), 1);
  const its = filtered.map(y => reactive({
    ...y, _label:String(y.year), _val:fmt(y.cnt)+' 辆', _sublabel:'',
    _barPct:(y.cnt/maxV*100).toFixed(1)+'%',
    _hover:false,
    _tipLines:[`${y.year} 年出厂：${fmt(y.cnt)} 辆`, y.cnt>0?`占全站 ${(y.cnt/(data.value?.overview?.totalVehicles||1)*100).toFixed(1)}%`:'0%']
  }));
  return { title, _items:its };
};

// ---- load ----
const loadStats = async () => {
  loading.value = true; error.value = null;
  try {
    const resp = await fetchStats();
    data.value = resp;
    makeOverviewCards(resp.overview, resp);
    buildSections(resp);
  } catch(e) {
    error.value = e.message || '加载失败';
  } finally { loading.value = false; }
};
onMounted(loadStats);
</script>

<style scoped>
/* ===== root ===== */
.stats-root {
  min-height: calc(100vh - 74px);
  background: #f5f7fb;
}
.stats-body { max-width: 1360px; margin: 0 auto; padding: 0 28px 64px; }

/* ===== hero ===== */
.hero { text-align: center; padding: 44px 0 32px; }
.hero__title { font-size: 32px; font-weight: 800; color: #1e293b; margin: 0 0 6px; }
.hero__sub { font-size: 14px; color: #94a3b8; margin: 0 0 32px; }
.ov-grid { display: flex; flex-wrap: wrap; justify-content: center; gap: 14px; }
.ov-card {
  position: relative;
  background: #fff; border-radius: 14px; padding: 18px 22px;
  min-width: 100px; cursor: default;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  border: 1px solid #e8ecf1;
  transition: transform .18s, box-shadow .18s, border-color .18s;
  animation: fadeUp .45s ease both;
}
.ov-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(37,99,235,.10);
  border-color: #2563eb33;
}
.ov-card__icon { font-size: 22px; display: block; margin-bottom: 4px; }
.ov-card__value { font-size: 26px; font-weight: 800; color: #1e293b; display: block; font-variant-numeric: tabular-nums; }
.ov-card__label { font-size: 12px; color: #94a3b8; margin-top: 2px; display: block; }
.ov-card__tip {
  position: absolute; bottom: calc(100% + 10px); left: 50%; transform: translateX(-50%);
  background: #1e293b; color: #f1f5f9; font-size: 12px; line-height: 1.6;
  padding: 10px 16px; border-radius: 10px; white-space: nowrap; z-index: 30;
  box-shadow: 0 8px 30px rgba(0,0,0,.18);
  pointer-events: none;
}

/* ===== section ===== */
.sec { margin-top: 40px; }
.sec__head {
  display: flex; align-items: baseline; gap: 10px;
  margin-bottom: 18px; padding-bottom: 10px;
  border-bottom: 2px solid var(--accent);
}
.sec__title { font-size: 19px; font-weight: 700; color: #1e293b; margin: 0; }
.sec__desc { font-size: 12px; color: #94a3b8; margin-left: auto; }
.sec__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 18px; }

/* ===== card ===== */
.card {
  background: #fff; border-radius: 14px; padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  border: 1px solid #e8ecf1;
}
.card__title { font-size: 14px; font-weight: 600; color: #475569; margin: 0 0 14px; padding-bottom: 10px; border-bottom: 1px solid #f1f5f9; }

/* ===== rank rows ===== */
.rank-list { display: flex; flex-direction: column; }
.rank-row {
  position: relative;
  display: flex; align-items: center; gap: 10px;
  padding: 6px 8px; border-radius: 8px; cursor: default;
  transition: background .12s;
}
.rank-row:hover { background: #f8fafc; }
.rank-row.has-link { cursor: pointer; }
.rank-row.has-link:hover .rank-label__main { color: #2563eb; }
.rank-row.has-link:hover .rank-chevron { opacity: 1; }
.rank-idx { width: 26px; text-align: center; flex-shrink: 0; }
.rank-medal { font-size: 14px; }
.rank-medal.gold { color: #f59e0b; }
.rank-medal.silver { color: #94a3b8; }
.rank-medal.bronze { color: #d69e2e; }
.rank-num { font-size: 11px; color: #94a3b8; font-weight: 600; }
.rank-label { flex:1; min-width:0; display:flex; flex-direction:column; gap:1px; }
.rank-label__main { font-size:13px; color:#334155; font-weight:500; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.rank-label__sub { font-size:11px; color:#94a3b8; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.rank-bar { width:80px; height:7px; background:#f1f5f9; border-radius:4px; overflow:hidden; flex-shrink:0; }
.rank-bar__fill { height:100%; border-radius:4px; transition:width .5s ease; }
.rank-val { width:60px; text-align:right; font-size:12px; color:#64748b; font-weight:600; flex-shrink:0; white-space:nowrap; }
.rank-chevron { font-size: 16px; color: #cbd5e1; flex-shrink: 0; opacity: 0; transition: opacity 0.15s; margin-left: -4px; }
.rank-tip {
  position: absolute; bottom: calc(100% + 6px); left: 50%; transform: translateX(-50%);
  background: #1e293b; color: #f1f5f9; font-size: 11px; line-height: 1.7;
  padding: 8px 14px; border-radius: 8px; white-space: nowrap; z-index: 30;
  box-shadow: 0 6px 24px rgba(0,0,0,.18);
  pointer-events: none;
}

/* ===== image list ===== */
.img-list { display: flex; flex-direction: column; gap: 6px; }
.img-row {
  position: relative;
  display: flex; align-items: center; gap: 10px;
  padding: 6px 8px; border-radius: 8px; cursor: default;
  transition: background .12s;
}
.img-row:hover { background: #f8fafc; }
.img-thumb {
  width: 48px; height: 36px; border-radius: 6px; object-fit: cover;
  flex-shrink: 0; background: #f1f5f9;
}
.img-thumb--empty { background: #f1f5f9; }
.img-info {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column; gap: 1px;
}
.img-info__plate { font-size: 13px; color: #334155; font-weight: 600; }
.img-info__model { font-size: 11px; color: #64748b; }
.img-info__meta { font-size: 11px; color: #94a3b8; }

/* ===== donut ===== */
.donut-wrap { display:flex; justify-content:center; align-items:center; margin-bottom:16px; position:relative; }
.donut-svg { width:130px; height:130px; }
.donut-center { position:absolute; text-align:center; }
.donut-center__val { font-size:22px; font-weight:800; color:#1e293b; display:block; }
.donut-center__lbl { font-size:11px; color:#94a3b8; }
.donut-legend { display:flex; flex-wrap:wrap; gap:4px 14px; }
.legend-row {
  position:relative; display:flex; align-items:center; gap:5px;
  font-size:12px; color:#64748b; cursor:default; padding:1px 3px; border-radius:4px; transition:background .1s;
}
.legend-row:hover { background:#f8fafc; }
.legend-dot { width:8px; height:8px; border-radius:2px; flex-shrink:0; }
.legend-name { max-width:72px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.legend-val { font-weight:600; color:#475569; }
.legend-pct { font-size:10px; color:#94a3b8; }
.legend-tip {
  position:absolute; bottom:calc(100%+6px); left:50%; transform:translateX(-50%);
  background:#1e293b; color:#f1f5f9; font-size:11px; padding:5px 10px; border-radius:6px;
  white-space:nowrap; z-index:30; box-shadow:0 6px 20px rgba(0,0,0,.16); pointer-events:none;
}

/* ===== empty ===== */
.empty { text-align:center; color:#cbd5e1; font-size:13px; padding:20px 0; }

/* ===== state ===== */
.state { text-align:center; padding:120px 0; color:#94a3b8; }
.state--err { color:#ef4444; }
.spinner { width:32px; height:32px; margin:0 auto 14px; border:3px solid #e2e8f0; border-top-color:#2563eb; border-radius:50%; animation:spin .7s linear infinite; }

/* ===== transitions ===== */
.tip-enter-active { transition:opacity .15s,transform .15s; }
.tip-leave-active { transition:opacity .1s; }
.tip-enter-from { opacity:0; transform:translateX(-50%) translateY(4px); }
.tip-leave-to { opacity:0; }

@keyframes fadeUp { from{opacity:0;transform:translateY(12px)} to{opacity:1;transform:translateY(0)} }
@keyframes spin { to{transform:rotate(360deg)} }

@media (max-width:860px) {
  .stats-body { padding:0 14px 48px; }
  .hero__title { font-size:26px; }
  .sec__grid { grid-template-columns:1fr; }
  .ov-card { min-width:70px; padding:12px 14px; }
  .ov-card__value { font-size:20px; }
  .rank-bar { width:50px; }
}
</style>
