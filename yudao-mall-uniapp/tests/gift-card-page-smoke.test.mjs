import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8');
}

test('pages.json registers gift card routes', () => {
  const pages = read('pages.json');

  assert.match(pages, /"root": "pages\/gift-card"/);
  assert.match(pages, /"path": "list"/);
  assert.match(pages, /"path": "order-detail"/);
  assert.match(pages, /"path": "redeem"/);
  assert.match(pages, /"path": "refund"/);
});

test('gift card pages exist and expose template order redeem refund actions', () => {
  const listSource = read('pages/gift-card/list.vue');
  const detailSource = read('pages/gift-card/order-detail.vue');
  const redeemSource = read('pages/gift-card/redeem.vue');
  const refundSource = read('pages/gift-card/refund.vue');

  assert.match(listSource, /GiftCardApi/);
  assert.match(listSource, /getTemplatePage/);
  assert.match(listSource, /createOrder/);
  assert.match(detailSource, /GiftCardApi/);
  assert.match(detailSource, /getOrder/);
  assert.match(redeemSource, /redeem/);
  assert.match(redeemSource, /礼品卡核销|核销礼品卡/);
  assert.match(refundSource, /applyRefund/);
  assert.match(refundSource, /礼品卡退款|申请退款/);
});

test('profile assets page exposes a real gift card entry', () => {
  const source = read('pages/profile/assets.vue');

  assert.match(source, /\/pages\/gift-card\/list/);
  assert.match(source, /礼品卡/);
});
