import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import fs from 'node:fs';
import { pathToFileURL } from 'node:url';

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
const notifyAuditHelperPath = path.join(
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
  'notifyAuditHelpers.mjs',
);

const { buildNotifyAuditSnapshot } = await import(pathToFileURL(notifyAuditHelperPath).href);

test('notify audit helper summarizes dual-channel blocked state', () => {
  const snapshot = buildNotifyAuditSnapshot({
    notifyRiskSummary: '双通道阻断',
    notifyOutboxList: [
      { channel: 'IN_APP', status: 'BLOCKED_NO_OWNER', diagnosticLabel: '缺店长 App 账号' },
      { channel: 'WECOM', status: 'BLOCKED_NO_OWNER', diagnosticLabel: '缺店长企微账号' },
    ],
  });

  assert.equal(snapshot.summary, '双通道阻断');
  assert.equal(snapshot.metrics.blockedCount, 2);
  assert.equal(snapshot.channels.IN_APP.statusText, '路由阻断');
  assert.equal(snapshot.channels.WECOM.statusText, '路由阻断');
});

test('notify audit helper highlights failed single channel', () => {
  const snapshot = buildNotifyAuditSnapshot({
    notifyOutboxList: [
      { channel: 'IN_APP', status: 'SENT', diagnosticLabel: '已发送' },
      { channel: 'WECOM', status: 'FAILED', diagnosticLabel: '发送失败', actionReason: 'dispatch failed' },
    ],
  });

  assert.equal(snapshot.summary, '企微发送失败');
  assert.equal(snapshot.metrics.sentCount, 1);
  assert.equal(snapshot.metrics.failedCount, 1);
  assert.equal(snapshot.channels.WECOM.statusText, '发送失败');
});

test('notify audit helper keeps missing channel explicit', () => {
  const snapshot = buildNotifyAuditSnapshot({
    notifyOutboxList: [
      { channel: 'IN_APP', status: 'PENDING', diagnosticLabel: '待派发' },
    ],
  });

  assert.equal(snapshot.channels.IN_APP.statusText, '待派发');
  assert.equal(snapshot.channels.WECOM.statusText, '未核出当前通道记录');
  assert.equal(snapshot.channels.WECOM.exists, false);
});

test('review api exposes dual-channel notify outbox types and methods', () => {
  const apiSource = fs.readFileSync(apiPath, 'utf8');
  assert.match(apiSource, /BookingReviewNotifyOutboxPageReq/);
  assert.match(apiSource, /getReviewNotifyOutboxList/);
  assert.match(apiSource, /getReviewNotifyOutboxPage/);
  assert.match(apiSource, /retryReviewNotifyOutbox/);
  assert.match(apiSource, /lastActionCode/);
  assert.match(apiSource, /receiverAccount/);
  assert.match(apiSource, /WECOM/);
  assert.match(apiSource, /\/booking\/review\/notify-outbox\/retry/);
});

test('detail page contains dual-channel notify observability block', () => {
  const detailSource = fs.readFileSync(detailPagePath, 'utf8');
  assert.match(detailSource, /通知观测/);
  assert.match(detailSource, /双通道摘要/);
  assert.match(detailSource, /App 通道/);
  assert.match(detailSource, /企微通道/);
  assert.match(detailSource, /BLOCKED_NO_OWNER/);
  assert.match(detailSource, /查看通知台账/);
  assert.match(detailSource, /getReviewNotifyOutboxList/);
  assert.match(detailSource, /buildNotifyAuditSnapshot/);
  assert.match(detailSource, /receiverAccount/);
  assert.match(detailSource, /IN_APP/);
  assert.match(detailSource, /WECOM/);
  assert.match(detailSource, /\/mall\/booking\/review\/notify-outbox/);
});

test('notify outbox page exists and uses dual-channel notify fields', () => {
  assert.equal(fs.existsSync(outboxPagePath), true);
  const pageSource = fs.readFileSync(outboxPagePath, 'utf8');
  assert.match(pageSource, /通知出站台账/);
  assert.match(pageSource, /getReviewNotifyOutboxPage/);
  assert.match(pageSource, /retryReviewNotifyOutbox/);
  assert.match(pageSource, /reviewId/);
  assert.match(pageSource, /receiverUserId/);
  assert.match(pageSource, /receiverAccount/);
  assert.match(pageSource, /BLOCKED_NO_OWNER/);
  assert.match(pageSource, /WECOM/);
  assert.match(pageSource, /IN_APP/);
  assert.match(pageSource, /重试/);
  assert.match(pageSource, /FAILED/);
  assert.match(pageSource, /manual-retry/);
  assert.match(pageSource, /已重新入队/);
  assert.match(pageSource, /只看阻断/);
  assert.match(pageSource, /只看失败/);
  assert.match(pageSource, /只看待派发/);
  assert.match(pageSource, /只看人工重试/);
  assert.match(pageSource, /diagnosticLabel/);
  assert.match(pageSource, /repairHint/);
  assert.match(pageSource, /manualRetryAllowed/);
  assert.match(pageSource, /actionLabel/);
  assert.match(pageSource, /actionOperatorLabel/);
  assert.match(pageSource, /actionReason/);
});
