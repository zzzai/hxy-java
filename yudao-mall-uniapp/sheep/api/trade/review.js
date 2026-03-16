import request from '@/sheep/request';

const BookingReviewApi = {
  getEligibility: (bookingOrderId) => {
    return request({
      url: '/booking/review/eligibility',
      method: 'GET',
      params: { bookingOrderId },
      custom: { auth: true, showLoading: false },
    });
  },
  createReview: (data) => {
    return request({
      url: '/booking/review/create',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
  getReviewPage: (params) => {
    return request({
      url: '/booking/review/page',
      method: 'GET',
      params,
      custom: { auth: true, showLoading: false },
    });
  },
  getReview: (id) => {
    return request({
      url: '/booking/review/get',
      method: 'GET',
      params: { id },
      custom: { auth: true, showLoading: false },
    });
  },
  getSummary: () => {
    return request({
      url: '/booking/review/summary',
      method: 'GET',
      custom: { auth: true, showLoading: false },
    });
  },
};

export default BookingReviewApi;
