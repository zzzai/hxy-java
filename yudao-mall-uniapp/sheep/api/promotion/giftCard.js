import request from '@/sheep/request';

const GiftCardApi = {
  getTemplatePage: (params) => {
    return request({
      url: '/promotion/gift-card/template/page',
      method: 'GET',
      params,
      custom: { auth: true, showLoading: false },
    });
  },
  createOrder: (data) => {
    return request({
      url: '/promotion/gift-card/order/create',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
  getOrder: (params) => {
    return request({
      url: '/promotion/gift-card/order/get',
      method: 'GET',
      params,
      custom: { auth: true, showLoading: false },
    });
  },
  redeem: (data) => {
    return request({
      url: '/promotion/gift-card/redeem',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
  applyRefund: (data) => {
    return request({
      url: '/promotion/gift-card/refund/apply',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
};

export default GiftCardApi;
