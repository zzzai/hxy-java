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
      { channel: 'IN_APP', status: 'SENT', diagnosticLabel: '已派发' },
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

test('review api exposes notify outbox types and methods without fake degraded fields', () => {
  const apiSource = fs.readFileSync(apiPath, 'utf8');
  assert.match(apiSource, /BookingReviewNotifyOutboxPageReq/);
  assert.match(apiSource, /BookingReviewNotifyOutboxSummary/);
  assert.match(apiSource, /getReviewNotifyOutboxList/);
  assert.match(apiSource, /getReviewNotifyOutboxPage/);
  assert.match(apiSource, /getReviewNotifyOutboxSummary/);
  assert.match(apiSource, /retryReviewNotifyOutbox/);
  assert.match(apiSource, /lastActionCode/);
  assert.match(apiSource, /receiverAccount/);
  assert.match(apiSource, /reviewAuditLabel/);
  assert.match(apiSource, /reviewAuditDetail/);
  assert.match(apiSource, /WECOM/);
  assert.match(apiSource, /\/booking\/review\/notify-outbox\/summary/);
  assert.match(apiSource, /\/booking\/review\/notify-outbox\/retry/);
  assert.doesNotMatch(apiSource, /\bdegraded\b/);
  assert.doesNotMatch(apiSource, /\bdegradeReason\b/);
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

test('notify outbox page distinguishes list subset from review-level audit summary', () => {
  assert.equal(fs.existsSync(outboxPagePath), true);
  const pageSource = fs.readFileSync(outboxPagePath, 'utf8');

  assert.match(pageSource, /通知出站台账/);
  assert.match(pageSource, /列表结果只展示当前筛选命中的 notify outbox 记录子集/);
  assert.match(pageSource, /审计摘要按 review 维度聚合同一条差评在 App \/ 企微的当前状态/);
  assert.match(pageSource, /不代表全链路送达/);
  assert.match(pageSource, /不代表门店已处理完成/);
  assert.match(pageSource, /只看阻断/);
  assert.match(pageSource, /只看失败/);
  assert.match(pageSource, /只看待派发/);
  assert.match(pageSource, /只看人工重试/);
  assert.match(pageSource, /双通道均已派发/);
  assert.match(pageSource, /存在阻断/);
  assert.match(pageSource, /存在失败/);
  assert.match(pageSource, /人工重试后待观察/);
  assert.match(pageSource, /跨通道状态分裂/);
  assert.match(pageSource, /跨通道结论/);
  assert.match(pageSource, /跨通道说明/);
  assert.match(pageSource, /已派发/);
  assert.doesNotMatch(pageSource, /label="已送达"|>已送达</);
  assert.doesNotMatch(pageSource, /label="已闭环"|>已闭环</);
  assert.doesNotMatch(pageSource, /label="已处理完成"|>已处理完成</);
});

test('notify outbox summary request ignores subset-only filters', () => {
  const pageSource = fs.readFileSync(outboxPagePath, 'utf8');
  const summaryQueryBlock = pageSource.match(/const buildSummaryQueryParams = \(\): BookingReviewApi\.BookingReviewNotifyOutboxPageReq => \(\{([\s\S]*?)\}\)/);

  assert.ok(summaryQueryBlock, 'missing buildSummaryQueryParams');
  const body = summaryQueryBlock[1];
  assert.match(body, /reviewId: queryParams\.reviewId/);
  assert.match(body, /storeId: queryParams\.storeId/);
  assert.match(body, /receiverRole: queryParams\.receiverRole/);
  assert.match(body, /notifyType: queryParams\.notifyType/);
  assert.doesNotMatch(body, /receiverUserId: queryParams\.receiverUserId/);
  assert.doesNotMatch(body, /receiverAccount: queryParams\.receiverAccount/);
  assert.doesNotMatch(body, /status: queryParams\.status/);
  assert.doesNotMatch(body, /channel: queryParams\.channel/);
  assert.doesNotMatch(body, /lastActionCode: queryParams\.lastActionCode/);
});
