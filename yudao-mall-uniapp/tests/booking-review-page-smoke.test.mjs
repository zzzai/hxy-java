import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8');
}

test('pages.json registers booking review routes', () => {
  const pages = read('pages.json');

  assert.match(pages, /"root": "pages\/booking"/);
  assert.match(pages, /"path": "review-add"/);
  assert.match(pages, /"path": "review-result"/);
  assert.match(pages, /"path": "review-list"/);
});

test('booking review add page contains submit flow and recovery states', () => {
  const source = read('pages/booking/review-add.vue');

  assert.match(source, /BookingReviewApi/);
  assert.match(source, /onSubmit/);
  assert.match(source, /state\.eligibility/);
  assert.match(source, /state\.submitting/);
  assert.match(source, /暂无可评价订单|暂不可评价/);
});

test('booking review result and list pages provide explicit empty states', () => {
  const resultSource = read('pages/booking/review-result.vue');
  const listSource = read('pages/booking/review-list.vue');

  assert.match(resultSource, /继续查看评价|返回订单详情|查看我的评价/);
  assert.match(resultSource, /提交成功|state\.reviewId/);
  assert.match(listSource, /BookingReviewApi/);
  assert.match(listSource, /暂无评价/);
  assert.match(listSource, /state\.summary/);
});
