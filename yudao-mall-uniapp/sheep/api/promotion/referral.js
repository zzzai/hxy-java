import request from '@/sheep/request';

const ReferralApi = {
  bindInviter: (data) => {
    return request({
      url: '/promotion/referral/bind-inviter',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
  getOverview: () => {
    return request({
      url: '/promotion/referral/overview',
      method: 'GET',
      custom: { auth: true, showLoading: false },
    });
  },
  getRewardLedgerPage: (params) => {
    return request({
      url: '/promotion/referral/reward-ledger/page',
      method: 'GET',
      params,
      custom: { auth: true, showLoading: false },
    });
  },
};

export default ReferralApi;
