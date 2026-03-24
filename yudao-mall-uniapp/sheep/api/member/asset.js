import request from '@/sheep/request';

const MemberAssetApi = {
  getAssetLedgerPage: (params) => {
    return request({
      url: '/member/asset-ledger/page',
      method: 'GET',
      params,
      custom: {
        auth: true,
        showLoading: false,
      },
    });
  },
};

export default MemberAssetApi;
