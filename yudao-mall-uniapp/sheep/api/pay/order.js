import request from '@/sheep/request';
import { degradedSuccess, isSuccessResult } from '@/sheep/api/compat';

const PayOrderApi = {
  // 获得支付订单
  getOrder: async (id, sync, no) => {
    const params = {};
    if (id) params.id = id;
    if (no) params.no = no;
    if (sync !== undefined) params.sync = sync;
    const result = await request({
      url: '/pay/order/get',
      method: 'GET',
      params,
      custom: {
        showError: false,
      },
    });
    if (isSuccessResult(result)) {
      return result;
    }
    return degradedSuccess({
      endpoint: '/pay/order/get',
      data: null,
      reason: 'PAY_ORDER_GET_UNAVAILABLE',
      msg: '当前后端暂不支持支付单查询接口，已降级为待确认状态，请稍后在订单页核对。',
    });
  },
  // 提交支付订单
  submitOrder: (data) => {
    return request({
      url: '/pay/order/submit',
      method: 'POST',
      data,
    });
  },
};

export default PayOrderApi;
