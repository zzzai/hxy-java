import request from '@/sheep/request';

const MemberTagApi = {
  getMyTags: () => {
    return request({
      url: '/member/tag/my',
      method: 'GET',
      custom: {
        auth: true,
        showLoading: false,
      },
    });
  },
};

export default MemberTagApi;
