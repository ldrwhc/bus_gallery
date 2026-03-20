const toNullableNumber = (value) => {
    if (value === null || value === undefined || value === '') return null;
    const numeric = Number(value);
    return Number.isNaN(numeric) ? null : numeric;
};

export const normalizeRegionName = (name = '') =>
    String(name || '')
        .replace(/(特别行政区|自治区|自治州|自治县|地区|盟|市|县|区)$/g, '')
        .trim();

export const buildRegionMap = (regions = []) =>
    (Array.isArray(regions) ? regions : []).reduce((acc, item) => {
        if (item?.id != null) acc[item.id] = item;
        return acc;
    }, {});

export const isProvinceRegion = (region) => {
    if (!region) return false;
    if (Number(region.level) === 1) return true;
    if (String(region.regionType || '').toUpperCase() === 'PROVINCE') return true;
    return region.parentId == null;
};

export const resolveProvinceId = (region, regionMap = {}) => {
    if (!region) return null;
    if (isProvinceRegion(region)) return toNullableNumber(region.id);
    if (region.provinceId != null) return toNullableNumber(region.provinceId);
    if (region.parentId != null) {
        const parent = regionMap[region.parentId];
        if (parent?.id != null) return toNullableNumber(parent.id);
    }
    return null;
};

export const buildProvinceOptions = (regions = []) =>
    (Array.isArray(regions) ? regions : [])
        .filter(isProvinceRegion)
        .map((item) => ({
            value: toNullableNumber(item.id),
            label: item.name
        }))
        .filter((item) => item.value != null)
        .sort((a, b) => String(a.label).localeCompare(String(b.label), 'zh-CN'));

export const buildCityOptions = (regions = [], provinceId, includeProvinceWhenNoCity = true) => {
    const normalizedProvinceId = toNullableNumber(provinceId);
    if (!normalizedProvinceId) return [];
    const regionMap = buildRegionMap(regions);
    const cities = (Array.isArray(regions) ? regions : [])
        .filter((item) => {
            if (!item || isProvinceRegion(item)) return false;
            return resolveProvinceId(item, regionMap) === normalizedProvinceId;
        })
        .map((item) => ({
            value: toNullableNumber(item.id),
            label: item.name
        }))
        .filter((item) => item.value != null)
        .sort((a, b) => String(a.label).localeCompare(String(b.label), 'zh-CN'));

    if (cities.length > 0 || !includeProvinceWhenNoCity) return cities;
    const province = regionMap[normalizedProvinceId];
    if (!province?.id) return [];
    return [{ value: toNullableNumber(province.id), label: province.name }];
};

export const formatRegionLabel = (regionId, regions = []) => {
    const regionMap = buildRegionMap(regions);
    const region = regionMap[toNullableNumber(regionId)];
    if (!region) return '-';
    if (isProvinceRegion(region)) return region.name || '-';
    const province = regionMap[resolveProvinceId(region, regionMap)];
    return province ? `${province.name} / ${region.name}` : region.name || '-';
};

export const splitProvinceCity = (regionId, regions = []) => {
    const regionMap = buildRegionMap(regions);
    const region = regionMap[toNullableNumber(regionId)];
    if (!region) return { provinceName: null, cityName: null };
    if (isProvinceRegion(region)) {
        return { provinceName: region.name || null, cityName: null };
    }
    const province = regionMap[resolveProvinceId(region, regionMap)];
    return {
        provinceName: province?.name || region.name || null,
        cityName: region.name || null
    };
};

export const findRegionIdByProvinceCity = (regions = [], provinceName, cityName) => {
    const regionMap = buildRegionMap(regions);
    const normalizedProvince = normalizeRegionName(provinceName);
    const normalizedCity = normalizeRegionName(cityName || provinceName);
    if (!normalizedProvince || !normalizedCity) return null;

    const provinces = (Array.isArray(regions) ? regions : []).filter((item) =>
        isProvinceRegion(item) && normalizeRegionName(item.name) === normalizedProvince
    );
    const province = provinces[0] || null;
    if (!province) return null;

    if (normalizedCity === normalizedProvince) {
        return toNullableNumber(province.id);
    }

    const city = (Array.isArray(regions) ? regions : []).find((item) => {
        if (!item || isProvinceRegion(item)) return false;
        return (
            normalizeRegionName(item.name) === normalizedCity &&
            resolveProvinceId(item, regionMap) === toNullableNumber(province.id)
        );
    });
    if (city?.id != null) return toNullableNumber(city.id);
    return null;
};
