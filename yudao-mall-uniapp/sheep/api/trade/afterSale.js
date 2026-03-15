import request from '@/sheep/request';
import { degradedError, degradedSuccess, isSuccessResult } from '@/sheep/api/compat';

const AfterSaleApi = {
  // 获得售后分页
  getAfterSalePage: async (params) => {
    const result = await request({
      url: `/trade/after-sale/page`,
      method: 'GET',
      params,
      custom: {
        showLoading: false,
        showError: false,
      },
    });
    if (isSuccessResult(result)) {
      return result;
    }
    return degradedSuccess({
      endpoint: '/trade/after-sale/page',
      data: {
        list: [],
        total: 0,
      },
      reason: 'AFTER_SALE_PAGE_UNAVAILABLE',
      msg: '当前后端暂不支持售后分页接口，已降级为空列表展示，不影响下单主流程。',
    });
  },
  // 创建售后
  createAfterSale: async (data) => {
    const result = await request({
      url: `/trade/after-sale/create`,
      method: 'POST',
      data,
      custom: {
        showError: false,
      },
    });
    if (isSuccessResult(result)) {
      return result;
    }
    return degradedError({
      endpoint: '/trade/after-sale/create',
      reason: 'AFTER_SALE_CREATE_UNSUPPORTED',
      msg: '当前后端暂不支持在线提交售后，已降级为人工处理，请联系客服。',
    });
  },
  // 获得售后
  getAfterSale: async (id) => {
    const result = await request({
      url: `/trade/after-sale/get`,
      method: 'GET',
      params: {
        id,
      },
      custom: {
        showError: false,
      },
    });
    if (isSuccessResult(result)) {
      return result;
    }
    return degradedSuccess({
      endpoint: '/trade/after-sale/get',
      data: null,
      reason: 'AFTER_SALE_GET_UNSUPPORTED',
      msg: '当前后端暂不支持售后详情接口，已降级为基础展示，请稍后重试。',
    });
  },
  // 取消售后
  cancelAfterSale: (id) => {
    return request({
      url: `/trade/after-sale/cancel`,
      method: 'DELETE',
      params: {
        id,
      },
    });
  },
  // 获得售后日志列表
  getAfterSaleLogList: async (afterSaleId) => {
    const result = await request({
      url: `/trade/after-sale-log/list`,
      method: 'GET',
      params: {
        afterSaleId,
      },
      custom: {
        showError: false,
      },
    });
    if (isSuccessResult(result)) {
      return result;
    }
    return degradedSuccess({
      endpoint: '/trade/after-sale-log/list',
      data: [],
      reason: 'AFTER_SALE_LOG_UNAVAILABLE',
      msg: '当前后端暂不支持售后进度日志接口，已降级为空进度展示。',
    });
  },
  // 退回货物
  deliveryAfterSale: async (data) => {
    const result = await request({
      url: `/trade/after-sale/delivery`,
      method: 'PUT',
      data,
      custom: {
        showError: false,
      },
    });
    if (isSuccessResult(result)) {
      return result;
    }
    return degradedError({
      endpoint: '/trade/after-sale/delivery',
      reason: 'AFTER_SALE_DELIVERY_UNSUPPORTED',
      msg: '当前后端暂不支持在线回寄录入，请联系客服人工处理。',
    });
  },
};

export default AfterSaleApi;
