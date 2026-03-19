const fuelAliasMap = {
    gasoline: '汽油',
    diesel: '柴油',
    electric: '纯电',
    hybrid: '混动',
    gas: '燃气',
    diesel_electric: '柴油+电',
    cng: '压缩天然气',
    cng_electric: '压缩天然气+电',
    lng: '液化天然气',
    lng_electric: '液化天然气+电',
    hydrogen_electric: '压缩氢气+电',
    compressed_hydrogen_electric: '压缩氢气+电',
    '柴油 + 电': '柴油+电',
    '压缩天然气 + 电': '压缩天然气+电',
    '液化天然气 + 电': '液化天然气+电',
    '压缩氢气 + 电': '压缩氢气+电'
};

export const FUEL_OPTIONS = [
    { label: '汽油', value: '汽油' },
    { label: '柴油', value: '柴油' },
    { label: '纯电', value: '纯电' },
    { label: '混动', value: '混动' },
    { label: '燃气', value: '燃气' },
    { label: '柴油+电', value: '柴油+电' },
    { label: '压缩天然气', value: '压缩天然气' },
    { label: '压缩天然气+电', value: '压缩天然气+电' },
    { label: '液化天然气', value: '液化天然气' },
    { label: '液化天然气+电', value: '液化天然气+电' },
    { label: '压缩氢气+电', value: '压缩氢气+电' }
];

export const normalizeFuelType = (raw) => {
    if (!raw) return '';
    const trimmed = String(raw).trim();
    return fuelAliasMap[trimmed] || fuelAliasMap[trimmed.toLowerCase()] || trimmed;
};

export const isElectricFuel = (raw) => {
    const value = normalizeFuelType(raw);
    return value.includes('电');
};

export const isCombustionFuel = (raw) => {
    const value = normalizeFuelType(raw);
    return (
        value.includes('柴油') ||
        value.includes('汽油') ||
        value.includes('天然气') ||
        value.includes('燃气')
    );
};
