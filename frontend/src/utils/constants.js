export const NAV_LINKS = [
    { label: '首页', name: 'Home', path: '/' },
    { label: '地区', name: 'RegionCatalog', path: '/regions' },
    { label: '公司', name: 'CompanyCatalog', path: '/companies' },
    { label: '品牌', name: 'BrandCatalog', path: '/brands' },
    { label: '型号', name: 'ModelCatalog', path: '/models' }
];

import placeholderBus from '@/assets/images/placeholder-bus.png';
export const FALLBACK_IMAGE = placeholderBus;

export const VEHICLE_INFO_FIELDS = [
    { key: 'plateNumber', label: '车牌号' },
    { key: 'customNumber', label: '自编号' },
    { key: 'brandName', label: '品牌' },
    { key: 'modelName', label: '车型' },
    { key: 'companyName', label: '公司' },
    { key: 'regionName', label: '地区' },
    { key: 'factoryDate', label: '出厂日期', type: 'date' },
    { key: 'launchDate', label: '上线日期', type: 'date' },
    { key: 'airConditioned', label: '空调', type: 'boolean' }
];

export const CONFIG_INFO_FIELDS = [
    { key: 'motor', label: '电机' },
    { key: 'engine', label: '发动机' },
    { key: 'fuelType', label: '燃料' },
    { key: 'stepType', label: '踏步' },
    { key: 'suspension', label: '悬挂' },
    { key: 'axle', label: '车桥' }
];