import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import fs from 'node:fs';

const repoRoot = path.resolve(new URL('.', import.meta.url).pathname, '..');
const apiPath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master',
  'script',
  'docker',
  'hxy-ui-admin',
  'overlay-vue3',
  'src',
  'api',
  'mall',
  'booking',
  'review.ts',
);
const outboxPagePath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master',
  'script',
  'docker',
  'hxy-ui-admin',
  'overlay-vue3',
  'src',
  'views',
  'mall',
  'booking',
  'review',
  'notifyOutbox',
  'index.vue',
);
const detailPagePath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master',
  'script',
  'docker',
  'hxy-ui-admin',
  'overlay-vue3',
  'src',
  'views',
  'mall',
  'booking',
  'review',
  'detail',
  'index.vue',
);
const managerRoutingPagePath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master',
  'script',
  'docker',
  'hxy-ui-admin',
  'overlay-vue3',
  'src',
  'views',
  'mall',
  'booking',
  'review',
  'managerRouting',
  'index.vue',
);

test('review api exposes dual-channel manager routing query types and methods', () => {
  const apiSource = fs.readFileSync(apiPath, 'utf8');
  assert.match(apiSource, /BookingReviewManagerAccountRouting/);
  assert.match(apiSource, /getReviewManagerAccountRouting\(/);
  assert.match(apiSource, /getReviewManagerAccountRoutingPage\(/);
  assert.match(apiSource, /managerWecomUserId/);
  assert.match(apiSource, /appRoutingLabel/);
  assert.match(apiSource, /wecomRoutingLabel/);
  assert.match(apiSource, /\/booking\/review\/manager-routing\/get/);
  assert.match(apiSource, /\/booking\/review\/manager-routing\/page/);
});

test('manager routing page exists and renders app/wecom routing truth fields', () => {
  assert.equal(fs.existsSync(managerRoutingPagePath), true);
  const pageSource = fs.readFileSync(managerRoutingPagePath, 'utf8');
  assert.match(pageSource, /店长账号路由核查/);
  assert.match(pageSource, /getReviewManagerAccountRoutingPage/);
  assert.match(pageSource, /getReviewManagerAccountRouting/);
  assert.match(pageSource, /routingLabel/);
  assert.match(pageSource, /repairHint/);
  assert.match(pageSource, /managerAdminUserId/);
  assert.match(pageSource, /managerWecomUserId/);
  assert.match(pageSource, /App 路由/);
  assert.match(pageSource, /企微路由/);
});

test('notify pages link blocked diagnostics to manager routing page', () => {
  const outboxSource = fs.readFileSync(outboxPagePath, 'utf8');
  const detailSource = fs.readFileSync(detailPagePath, 'utf8');
  assert.match(outboxSource, /查看店长路由/);
  assert.match(outboxSource, /\/mall\/booking\/review\/manager-routing/);
  assert.match(detailSource, /查看店长路由/);
  assert.match(detailSource, /\/mall\/booking\/review\/manager-routing/);
});
