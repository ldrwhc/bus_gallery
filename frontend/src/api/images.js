import http from './axiosInstance';

/**
 * 上传车辆图片及相关信息
 * @param {Object} payload
 * @param {File}   payload.file           必填，图片文件
 * @param {Number} payload.vehicleId      可选，已存在车辆 ID
 * @param {String} payload.plateNumber    车牌号
 * @param {String} payload.modelName      车型/型号
 * @param {Number} payload.regionId       地区 ID
 * @param {String} payload.regionName     地区名称
 * @param {Number} payload.companyId      公司 ID
 * @param {String} payload.companyName    公司名称
 * @param {String} payload.customNumber   自编号
 * @param {String} payload.produceDate    出厂日期（YYYY-MM-DD）
 * @param {String} payload.launchDate     上线日期（YYYY-MM-DD）
 * @param {Boolean}payload.airConditioned 是否空调
 * @param {String} payload.description    备注
 */
export const uploadImage = (payload = {}) => {
    const { file, ...rest } = payload;
    if (!file) {
        return Promise.reject(new Error('请选择要上传的图片'));
    }

    const formData = new FormData();
    formData.append('file', file);

    Object.entries(rest).forEach(([key, value]) => {
        if (
            value === null ||
            value === undefined ||
            value === '' ||
            (typeof value === 'number' && Number.isNaN(value))
        ) {
            return;
        }
        formData.append(key, value);
    });

    return http
        .post('/images/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        .then((response) => {
            // 兼容多种返回格式：{code, data:{...}}、{...}、直接是 Image
            if (response && typeof response === 'object') {
                if (response.data && typeof response.data === 'object') {
                    return response.data;
                }
                if (response.result && typeof response.result === 'object') {
                    return response.result;
                }
            }
            return response;
        });
};

export const fetchLatestImages = (params = {}) =>
    http.get('/images/latest', { params });

export const fetchImageDetail = (imageId) =>
    http.get(`/images/${imageId}`);

export const deleteImage = (imageId) =>
    http.delete(`/images/${imageId}`);
