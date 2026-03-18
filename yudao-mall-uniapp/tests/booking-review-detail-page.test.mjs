import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8');
}

test('review detail exposes member-facing fields and empty-state copy', () => {
  const source = read('pages/booking/review-detail.vue');

  assert.match(source, /BookingReviewApi/);
  assert.match(source, /getReview/);
  assert.match(source, /bookingOrderId/);
  assert.match(source, /提交时间/);
  assert.match(source, /overallScore/);
  assert.match(source, /serviceScore/);
  assert.match(source, /technicianScore/);
  assert.match(source, /environmentScore/);
  assert.match(source, /评价图片/);
  assert.match(source, /官方回复/);
  assert.match(source, /评价不存在或参数异常/);
  assert.doesNotMatch(source, /serviceOrderId/);
});
