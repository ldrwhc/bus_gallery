import http from './axiosInstance';

const unwrapData = (response) => response?.data ?? response ?? null;

/**
 * 按图片解析/创建交易绑定（goodsId + activityId）。
 */
export const resolveTradeBindingByImage = (imageId, payload = {}) =>
    http.post(`/bridge/images/${imageId}/binding`, payload).then(unwrapData);

/**
 * 按 goodsId 查询已存在的交易绑定（包含 imageId）。
 */
export const fetchTradeBridgeBindingByGoods = (goodsId) =>
    http.get(`/bridge/goods/${goodsId}/binding`).then(unwrapData);

/**
 * 查询商品拼团配置。
 */
export const fetchTradeBridgeConfig = (payload) =>
    http.post('/bridge/index/config', payload).then(unwrapData);

/**
 * 锁单（创建拼团单或参团单）。
 */
export const lockTradeBridgeOrder = (payload) =>
    http.post('/bridge/trade/lock', payload).then(unwrapData);

/**
 * 结算（模拟支付回调）。
 */
export const settleTradeBridgeOrder = (payload) =>
    http.post('/bridge/trade/settle', payload).then(unwrapData);

/**
 * 退款。
 */
export const refundTradeBridgeOrder = (payload) =>
    http.post('/bridge/trade/refund', payload).then(unwrapData);

/**
 * 查询正在拼团的队伍。
 * activityId 为空时返回全局进行中队伍（按时间倒序）。
 */
export const fetchPortalActiveTeams = (activityId, limit = 5) =>
    http.get('/bridge/portal/teams', {
        params: activityId ? { activityId, limit } : { limit }
    }).then(unwrapData);

/**
 * 直接下单并支付。
 */
export const directBuyCheckout = (payload) =>
    http.post('/bridge/portal/direct-buy', payload).then(unwrapData);

/**
 * 发起/加入拼团并支付。
 */
export const groupBuyCheckout = (payload) =>
    http.post('/bridge/portal/group-buy', payload).then(unwrapData);

/**
 * 查询当前用户交易消息。
 */
export const fetchPortalMessages = (limit = 20) =>
    http.get('/bridge/portal/messages', { params: { limit } }).then(unwrapData);

/**
 * 查询当前用户交易记录。
 */
export const fetchPortalRecords = (limit = 50) =>
    http.get('/bridge/portal/records', { params: { limit } }).then(unwrapData);
