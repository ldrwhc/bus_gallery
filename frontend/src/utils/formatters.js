export const formatDate = (value, fallback = '—', options = {}) => {
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

export const formatBoolean = (
    value,
    truthyLabel = '是',
    falsyLabel = '否',
    fallback = '—'
) => {
    if (value === null || value === undefined) return fallback;
    return value ? truthyLabel : falsyLabel;
};

export const formatVehicleTitle = (vehicle = {}) => {
    if (!vehicle) return '车辆信息';
    return (
        vehicle.plateNumber ||
        vehicle.modelName ||
        vehicle.companyName ||
        '车辆信息'
    );
};

export const formatFuelType = (fuelType) => fuelType || '燃料类型未知';

export const ensureArray = (value) => (Array.isArray(value) ? value : []);