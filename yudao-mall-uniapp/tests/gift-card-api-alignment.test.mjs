import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const projectRoot = path.resolve(import.meta.dirname, '..');

function loadGiftCardApi() {
  const apiPath = path.join(projectRoot, 'sheep/api/promotion/giftCard.js');
  let source = fs.readFileSync(apiPath, 'utf8');
  source = source.replace(
    "import request from '@/sheep/request';",
    'const request = (config) => config;'
  );
  source = source.replace(/export default GiftCardApi;\s*$/, 'module.exports = GiftCardApi;');
  const context = { module: { exports: {} }, exports: {} };
  vm.runInNewContext(source, context, { filename: apiPath });
  return context.module.exports;
}

function normalize(value) {
  return JSON.parse(JSON.stringify(value));
}

test('gift card api aligns template order redeem and refund routes', () => {
  const giftCardApi = loadGiftCardApi();
  const createPayload = { templateId: 10, quantity: 2, sendScene: 'SELF', clientToken: 'gift-create-001' };
  const redeemPayload = { cardNo: 'GC1001', redeemCode: '8888', clientToken: 'gift-redeem-001' };
  const refundPayload = { orderId: 9001, reason: '未使用', clientToken: 'gift-refund-001' };

  assert.deepEqual(normalize(giftCardApi.getTemplatePage({ pageNo: 1, pageSize: 10, status: 'ENABLE' })), {
    url: '/promotion/gift-card/template/page',
    method: 'GET',
    params: { pageNo: 1, pageSize: 10, status: 'ENABLE' },
    custom: { auth: true, showLoading: false },
  });

  assert.deepEqual(normalize(giftCardApi.createOrder(createPayload)), {
    url: '/promotion/gift-card/order/create',
    method: 'POST',
    data: createPayload,
    custom: { auth: true, showLoading: true },
  });

  assert.deepEqual(normalize(giftCardApi.getOrder({ orderId: 9001 })), {
    url: '/promotion/gift-card/order/get',
    method: 'GET',
    params: { orderId: 9001 },
    custom: { auth: true, showLoading: false },
  });

  assert.deepEqual(normalize(giftCardApi.redeem(redeemPayload)), {
    url: '/promotion/gift-card/redeem',
    method: 'POST',
    data: redeemPayload,
    custom: { auth: true, showLoading: true },
  });

  assert.deepEqual(normalize(giftCardApi.applyRefund(refundPayload)), {
    url: '/promotion/gift-card/refund/apply',
    method: 'POST',
    data: refundPayload,
    custom: { auth: true, showLoading: true },
  });
});
