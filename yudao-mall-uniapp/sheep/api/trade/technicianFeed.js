import request from '@/sheep/request';

const TechnicianFeedApi = {
  getFeedPage: (params) => {
    return request({
      url: '/booking/technician/feed/page',
      method: 'GET',
      params,
      custom: { showLoading: false },
    });
  },
  toggleLike: (data) => {
    return request({
      url: '/booking/technician/feed/like',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: false },
    });
  },
  createComment: (data) => {
    return request({
      url: '/booking/technician/feed/comment/create',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
};

export default TechnicianFeedApi;
