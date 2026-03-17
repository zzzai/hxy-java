import request from '@/sheep/request';

const REQUEST_FAILED_RESULT = Object.freeze({
  code: -1,
  data: null,
  msg: 'BOOKING_REVIEW_REQUEST_FAILED',
});

async function requestBookingReview(config) {
  try {
    const result = await request(config);
    if (result === false || result == null) {
      return REQUEST_FAILED_RESULT;
    }
    return result;
  } catch (error) {
    return {
      code: error?.code ?? -1,
      data: null,
      msg: error?.msg || error?.message || REQUEST_FAILED_RESULT.msg,
    };
  }
}

const BookingReviewApi = {
  getEligibility: async (bookingOrderId) => {
    return requestBookingReview({
      url: '/booking/review/eligibility',
      method: 'GET',
      params: { bookingOrderId },
      custom: { auth: true, showLoading: false },
    });
  },
  createReview: async (data) => {
    return requestBookingReview({
      url: '/booking/review/create',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
  getReviewPage: async (params) => {
    return requestBookingReview({
      url: '/booking/review/page',
      method: 'GET',
      params,
      custom: { auth: true, showLoading: false },
    });
  },
  getReview: async (id) => {
    return requestBookingReview({
      url: '/booking/review/get',
      method: 'GET',
      params: { id },
      custom: { auth: true, showLoading: false },
    });
  },
  getSummary: async () => {
    return requestBookingReview({
      url: '/booking/review/summary',
      method: 'GET',
      custom: { auth: true, showLoading: false },
    });
  },
};

export default BookingReviewApi;
