import placeholderBus from '@/assets/images/placeholder-bus.png';

export const NAV_LINKS = [
    { label: '首页', name: 'Home', path: '/' },
    { label: '图库', name: 'Gallery', path: '/gallery' },
    { label: '地区', name: 'RegionCatalog', path: '/regions' },
    { label: '公司', name: 'CompanyCatalog', path: '/companies' },
    { label: '品牌', name: 'BrandCatalog', path: '/brands' },
    { label: '车型', name: 'ModelCatalog', path: '/models' },
    { label: '线路', name: 'RouteCatalog', path: '/routes' },
    { label: '对比', name: 'Compare', path: '/compare' },
    { label: '上传', name: 'Upload', path: '/upload', requiresAuth: true }
];

export const FALLBACK_IMAGE = placeholderBus;

export const VEHICLE_INFO_FIELDS = [
    { key: 'regionName', label: '地区', link: 'region' },
    { key: 'companyName', label: '公司', link: 'company' },
    { key: 'customNumber', label: '自编号' },
    { key: 'factoryDate', label: '出厂日期', type: 'date' },
    { key: 'launchDate', label: '上线日期', type: 'date' }
];

export const CONFIG_INFO_FIELDS = [
    { key: 'brandName', label: '品牌', link: 'brand' },
    { key: 'modelName', label: '车型', link: 'model' },
    { key: 'motor', label: '电机' },
    { key: 'engine', label: '发动机' },
    { key: 'transmissionSystem', label: '变速系统' },
    { key: 'fuelType', label: '燃料类型' },
    { key: 'stepType', label: '踏步' },
    { key: 'suspension', label: '悬挂' },
    { key: 'axle', label: '车桥' }
];

export const SUB_TYPE_LABELS = {
    INTERVAL: '区间',
    BRANCH: '支线',
    EXPRESS: '快线',
    NIGHT: '夜班',
    DIRECT: '直达'
};

export const ROUTE_TYPE_LABELS = {
    REGULAR: '常规',
    BRT: '快速公交',
    AIRPORT: '机场巴士',
    TOURIST: '旅游专线',
    COMMUNITY: '微循环',
    SUBWAY: '地铁接驳'
};
