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

test('review api exposes notify outbox query and retry methods', () => {
  const apiSource = fs.readFileSync(apiPath, 'utf8');
  assert.match(apiSource, /BookingReviewNotifyOutboxPageReq/);
  assert.match(apiSource, /getReviewNotifyOutboxList/);
  assert.match(apiSource, /getReviewNotifyOutboxPage/);
  assert.match(apiSource, /retryReviewNotifyOutbox/);
  assert.match(apiSource, /\/booking\/review\/notify-outbox\/retry/);
});

test('detail page contains notify observability block and ledger link', () => {
  const detailSource = fs.readFileSync(detailPagePath, 'utf8');
  assert.match(detailSource, /通知观测/);
  assert.match(detailSource, /BLOCKED_NO_OWNER/);
  assert.match(detailSource, /查看通知台账/);
  assert.match(detailSource, /getReviewNotifyOutboxList/);
  assert.match(detailSource, /\/mall\/booking\/review\/notify-outbox/);
});

test('notify outbox page exists and uses review notify outbox api', () => {
  assert.equal(fs.existsSync(outboxPagePath), true);
  const pageSource = fs.readFileSync(outboxPagePath, 'utf8');
  assert.match(pageSource, /通知出站台账/);
  assert.match(pageSource, /getReviewNotifyOutboxPage/);
  assert.match(pageSource, /retryReviewNotifyOutbox/);
  assert.match(pageSource, /reviewId/);
  assert.match(pageSource, /receiverUserId/);
  assert.match(pageSource, /BLOCKED_NO_OWNER/);
  assert.match(pageSource, /重试/);
  assert.match(pageSource, /FAILED/);
  assert.match(pageSource, /manual-retry/);
  assert.match(pageSource, /已重新入队/);
  assert.match(pageSource, /只看阻断/);
  assert.match(pageSource, /只看失败/);
  assert.match(pageSource, /只看待派发/);
  assert.match(pageSource, /diagnosticLabel/);
  assert.match(pageSource, /repairHint/);
  assert.match(pageSource, /manualRetryAllowed/);
});
