import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function loadReviewApi(requestImpl = (config) => config) {
  const reviewPath = path.resolve(__dirname, '../sheep/api/trade/review.js');
  let source = fs.readFileSync(reviewPath, 'utf8');
  source = source.replace(
    "import request from '@/sheep/request';",
    'const request = globalThis.__requestImpl__;'
  );
  source = source.replace(/export default BookingReviewApi;\s*$/, 'module.exports = BookingReviewApi;');

  const context = {
    module: { exports: {} },
    exports: {},
    globalThis: {
      __requestImpl__: requestImpl,
    },
  };
  vm.runInNewContext(source, context, { filename: reviewPath });
  return context.module.exports;
}

function normalize(value) {
  return JSON.parse(JSON.stringify(value));
}

test('booking review api aligns eligibility create and summary routes', async () => {
  const reviewApi = loadReviewApi();

  assert.deepEqual(normalize(await reviewApi.getEligibility(101)), {
    url: '/booking/review/eligibility',
    method: 'GET',
    params: { bookingOrderId: 101 },
    custom: { auth: true, showLoading: false },
  });

  const createPayload = {
    bookingOrderId: 101,
    overallScore: 5,
    content: '技师服务很细致',
  };
  assert.deepEqual(normalize(await reviewApi.createReview(createPayload)), {
    url: '/booking/review/create',
    method: 'POST',
    data: createPayload,
    custom: { auth: true, showLoading: true },
  });

  assert.deepEqual(normalize(await reviewApi.getSummary()), {
    url: '/booking/review/summary',
    method: 'GET',
    custom: { auth: true, showLoading: false },
  });
});

test('booking review api aligns list and detail routes', async () => {
  const reviewApi = loadReviewApi();

  assert.deepEqual(normalize(await reviewApi.getReviewPage({ pageNo: 1, pageSize: 10, reviewLevel: 3 })), {
    url: '/booking/review/page',
    method: 'GET',
    params: { pageNo: 1, pageSize: 10, reviewLevel: 3 },
    custom: { auth: true, showLoading: false },
  });

  assert.deepEqual(normalize(await reviewApi.getReview(9001)), {
    url: '/booking/review/get',
    method: 'GET',
    params: { id: 9001 },
    custom: { auth: true, showLoading: false },
  });
});

test('booking review api normalizes false responses into structured failures', async () => {
  const reviewApi = loadReviewApi(async () => false);

  assert.deepEqual(normalize(await reviewApi.getEligibility(101)), {
    code: -1,
    data: null,
    msg: 'BOOKING_REVIEW_REQUEST_FAILED',
  });
});

test('booking review api normalizes rejected requests into structured failures', async () => {
  const reviewApi = loadReviewApi(async () => {
    throw new Error('network down');
  });

  assert.deepEqual(normalize(await reviewApi.createReview({ bookingOrderId: 101 })), {
    code: -1,
    data: null,
    msg: 'network down',
  });
});
