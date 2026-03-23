import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '..');

const ledgerPagePath = path.join(
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
  'index.vue'
);

const dashboardPagePath = path.join(
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
  'dashboard',
  'index.vue'
);

const queryHelpersPath = path.join(
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
  'queryHelpers.mjs'
);

const reviewApiPath = path.join(
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
  'review.ts'
);

const {
  resolveManagerTodoSlaStage,
  BOOKING_REVIEW_LEDGER_QUERY_FIELDS,
  BOOKING_REVIEW_DISPLAY_ONLY_RETURN_FIELDS,
  BOOKING_REVIEW_DASHBOARD_SUMMARY_FIELDS,
} = await import(pathToFileURL(queryHelpersPath).href);

const extractInterfaceBlock = (source, interfaceName) => {
  const match = source.match(new RegExp(`export interface ${interfaceName}(?: extends [^{]+)? \\{([\\s\\S]*?)\\n\\}`));
  assert.ok(match, `missing interface ${interfaceName}`);
  return match[1];
};

test('review ledger exposes quick SLA filters for ops', () => {
  const source = fs.readFileSync(ledgerPagePath, 'utf8');
  assert.match(source, /待认领优先/);
  assert.match(source, /即将认领超时/);
  assert.match(source, /认领超时/);
  assert.match(source, /即将首次处理超时/);
  assert.match(source, /首次处理超时/);
  assert.match(source, /即将闭环超时/);
  assert.match(source, /闭环超时/);
  assert.match(source, /历史待初始化/);
});

test('review ledger exposes manager todo quick actions', () => {
  const source = fs.readFileSync(ledgerPagePath, 'utf8');
  assert.match(source, /claimManagerTodo/);
  assert.match(source, /recordManagerTodoFirstAction/);
  assert.match(source, /closeManagerTodo/);
  assert.match(source, /快速认领/);
  assert.match(source, /记录首次处理/);
  assert.match(source, /标记闭环/);
});

test('manager todo sla fallback keeps CLOSED explicit in read-path fallback', () => {
  assert.equal(resolveManagerTodoSlaStage({ reviewLevel: 3, managerTodoStatus: 4 }), 'CLOSED');
  assert.equal(
    resolveManagerTodoSlaStage({
      reviewLevel: 3,
      managerTodoStatus: 4,
      managerSlaStage: 'NORMAL',
      managerCloseDeadlineAt: '2026-03-23 10:00:00',
    }),
    'CLOSED',
  );
  assert.equal(
    resolveManagerTodoSlaStage({
      reviewLevel: 3,
      managerClosedAt: '2026-03-23 10:00:00',
      managerSlaStage: 'FIRST_ACTION_TIMEOUT',
    }),
    'CLOSED',
  );
  assert.equal(resolveManagerTodoSlaStage({ reviewLevel: 3 }), 'PENDING_INIT');
});

test('review api keeps query truth separate from display-only return fields', () => {
  const source = fs.readFileSync(reviewApiPath, 'utf8');
  const pageReqBlock = extractInterfaceBlock(source, 'BookingReviewPageReq');
  const reviewRespBlock = extractInterfaceBlock(source, 'BookingReview');

  assert.deepEqual(BOOKING_REVIEW_LEDGER_QUERY_FIELDS, [
    'id',
    'bookingOrderId',
    'storeId',
    'technicianId',
    'memberId',
    'reviewLevel',
    'riskLevel',
    'followStatus',
    'onlyManagerTodo',
    'onlyPendingInit',
    'managerTodoStatus',
    'managerSlaStatus',
    'replyStatus',
    'submitTime',
  ]);

  BOOKING_REVIEW_LEDGER_QUERY_FIELDS.forEach((field) => {
    assert.match(pageReqBlock, new RegExp(`\\b${field}\\?:`));
  });

  assert.deepEqual(BOOKING_REVIEW_DISPLAY_ONLY_RETURN_FIELDS, [
    'priorityLevel',
    'priorityReason',
    'notifyRiskSummary',
  ]);

  BOOKING_REVIEW_DISPLAY_ONLY_RETURN_FIELDS.forEach((field) => {
    assert.doesNotMatch(pageReqBlock, new RegExp(`\\b${field}\\?:`));
    assert.match(reviewRespBlock, new RegExp(`\\b${field}\\?:`));
  });

  ['managerSlaStage', 'managerClaimDeadlineAt', 'managerFirstActionDeadlineAt', 'managerCloseDeadlineAt'].forEach(
    (field) => {
      assert.match(reviewRespBlock, new RegExp(`\\b${field}\\?:`));
    },
  );

  ['managerTimeoutCategory', 'priorityReasonCode', 'notifyAuditStage', 'degraded', 'degradeReason'].forEach((field) => {
    assert.doesNotMatch(pageReqBlock, new RegExp(`\\b${field}\\??:`));
    assert.doesNotMatch(reviewRespBlock, new RegExp(`\\b${field}\\??:`));
  });
});

test('review dashboard only uses committed summary counters', () => {
  const dashboardSource = fs.readFileSync(dashboardPagePath, 'utf8');
  const apiSource = fs.readFileSync(reviewApiPath, 'utf8');
  const dashboardSummaryBlock = extractInterfaceBlock(apiSource, 'BookingReviewDashboardSummary');

  assert.deepEqual(BOOKING_REVIEW_DASHBOARD_SUMMARY_FIELDS, [
    'totalCount',
    'positiveCount',
    'neutralCount',
    'negativeCount',
    'pendingFollowCount',
    'urgentCount',
    'repliedCount',
    'managerTodoPendingCount',
    'managerTodoClaimTimeoutCount',
    'managerTodoClaimDueSoonCount',
    'managerTodoFirstActionTimeoutCount',
    'managerTodoFirstActionDueSoonCount',
    'managerTodoCloseTimeoutCount',
    'managerTodoCloseDueSoonCount',
    'managerTodoClosedCount',
  ]);

  BOOKING_REVIEW_DASHBOARD_SUMMARY_FIELDS.forEach((field) => {
    assert.match(dashboardSummaryBlock, new RegExp(`\\b${field}\\?:`));
  });

  ['priorityP0Count', 'priorityP1Count', 'priorityP2Count', 'priorityP3Count', 'managerTimeoutDueSoonCount', 'notifyAuditAnyBlockedCount'].forEach((field) => {
    assert.doesNotMatch(dashboardSummaryBlock, new RegExp(`\\b${field}\\??:`));
    assert.doesNotMatch(dashboardSource, new RegExp(`\\.${field}\\b`));
  });

  assert.match(dashboardSource, /只使用 dashboard-summary 已正式返回的计数/);
  assert.match(dashboardSource, /店长待办 CLOSED/);
  assert.match(dashboardSource, /不代表提醒已派发成功或门店已处理完成/);
});
