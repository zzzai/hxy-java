import request from '@/sheep/request';

const MemberLevelApi = {
  getLevelList: () => {
    return request({
      url: '/member/level/list',
      method: 'GET',
      custom: {
        auth: true,
        showLoading: false,
      },
    });
  },
  getExperienceRecordPage: (params) => {
    return request({
      url: '/member/experience-record/page',
      method: 'GET',
      params,
      custom: {
        auth: true,
        showLoading: false,
      },
    });
  },
};

export default MemberLevelApi;
