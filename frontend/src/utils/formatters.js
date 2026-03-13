export const formatDate = (value, fallback = '-', options = {}) => {
    if (!value) return fallback;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return fallback;

    return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        ...options
    });
};

export const formatYearMonth = (value, fallback = '-') => {
    if (!value) return fallback;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return fallback;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
};

export const formatDateTime = (value, fallback = '-') => {
    if (!value) return fallback;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return fallback;
    const dateParts = [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0')
    ];
    const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
    return `${dateParts.join('-')} ${time}`;
};

export const formatBoolean = (value, truthyLabel = '是', falsyLabel = '否', fallback = '未知') => {
    if (value === null || value === undefined) return fallback;
    return value ? truthyLabel : falsyLabel;
};

export const formatVehicleTitle = (vehicle = {}) => {
    if (!vehicle) return '车辆信息';
    return vehicle.plateNumber || vehicle.modelName || vehicle.companyName || '车辆信息';
};

const fuelLabelMap = {
    gasoline: '汽油',
    diesel: '柴油',
    electric: '纯电',
    hybrid: '混动',
    gas: '燃气',
    diesel_electric: '柴油 + 电',
    cng: '压缩天然气',
    cng_electric: '压缩天然气 + 电',
    lng: '液化天然气',
    lng_electric: '液化天然气 + 电',
    hydrogen_electric: '压缩氢气 + 电',
    compressed_hydrogen_electric: '压缩氢气 + 电'
};

export const formatFuelType = (fuelType) => {
    if (!fuelType) return '燃料类型未知';
    return fuelLabelMap[fuelType] || fuelType;
};

export const ensureArray = (value) => (Array.isArray(value) ? value : []);
