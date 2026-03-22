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
  assert.match(apiSource, /BookingReviewManagerAccountRoutingSummary/);
  assert.match(apiSource, /getReviewManagerAccountRouting\(/);
  assert.match(apiSource, /getReviewManagerAccountRoutingPage\(/);
  assert.match(apiSource, /getReviewManagerAccountRoutingCoverageSummary\(/);
  assert.match(apiSource, /managerWecomUserId/);
  assert.match(apiSource, /appRoutingLabel/);
  assert.match(apiSource, /wecomRoutingLabel/);
  assert.match(apiSource, /governanceStageLabel/);
  assert.match(apiSource, /verificationFreshnessLabel/);
  assert.match(apiSource, /sourceClosureLabel/);
  assert.match(apiSource, /sourceTruthStage/);
  assert.match(apiSource, /sourceTruthLabel/);
  assert.match(apiSource, /sourceTruthDetail/);
  assert.match(apiSource, /sourceTruthActionHint/);
  assert.match(apiSource, /governanceActionSummary/);
  assert.match(apiSource, /dualReadyCount/);
  assert.match(apiSource, /missingBothCount/);
  assert.match(apiSource, /immediateFixCount/);
  assert.match(apiSource, /verifySourceCount/);
  assert.match(apiSource, /staleVerifyCount/);
  assert.match(apiSource, /sourcePendingCount/);
  assert.match(apiSource, /observeReadyCount/);
  assert.match(apiSource, /routeConfirmedCount/);
  assert.match(apiSource, /sourceMissingCount/);
  assert.match(apiSource, /contactOnlyPendingBindCount/);
  assert.match(apiSource, /contactMissingCount/);
  assert.match(apiSource, /verifyStaleCount/);
  assert.match(apiSource, /governanceStage\?:/);
  assert.match(apiSource, /verificationFreshnessStatus\?:/);
  assert.match(apiSource, /sourceClosureStatus\?:/);
  assert.match(apiSource, /sourceTruthStage\?:/);
  assert.match(apiSource, /\/booking\/review\/manager-routing\/summary/);
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
  assert.match(pageSource, /覆盖率概览/);
  assert.match(pageSource, /双通道覆盖率/);
  assert.match(pageSource, /App 覆盖率/);
  assert.match(pageSource, /企微覆盖率/);
  assert.match(pageSource, /只看缺任一绑定/);
  assert.match(pageSource, /只看缺 App/);
  assert.match(pageSource, /只看缺企微/);
  assert.match(pageSource, /只看双缺失/);
  assert.match(pageSource, /治理工作台概览/);
  assert.match(pageSource, /只看立即治理/);
  assert.match(pageSource, /只看来源待闭环/);
  assert.match(pageSource, /只看长期未核验/);
  assert.match(pageSource, /只看可观察就绪/);
  assert.match(pageSource, /来源闭环概览/);
  assert.match(pageSource, /只看来源已确认/);
  assert.match(pageSource, /只看来源缺失/);
  assert.match(pageSource, /只看联系人待转绑定/);
  assert.match(pageSource, /只看联系人缺失/);
  assert.match(pageSource, /只看来源待复核/);
  assert.match(pageSource, /治理优先级/);
  assert.match(pageSource, /治理分组/);
  assert.match(pageSource, /核验状态/);
  assert.match(pageSource, /来源闭环/);
  assert.match(pageSource, /来源结论/);
  assert.match(pageSource, /来源说明/);
  assert.match(pageSource, /下一步动作/);
  assert.match(pageSource, /治理归口/);
  assert.match(pageSource, /交接摘要/);
  assert.match(pageSource, /getReviewManagerAccountRoutingCoverageSummary/);
  assert.match(pageSource, /appRoutingStatus/);
  assert.match(pageSource, /wecomRoutingStatus/);
  assert.match(pageSource, /governanceStage/);
  assert.match(pageSource, /verificationFreshnessStatus/);
  assert.match(pageSource, /sourceClosureStatus/);
  assert.match(pageSource, /sourceTruthStage/);
  assert.match(pageSource, /sourceTruthLabel/);
  assert.match(pageSource, /sourceTruthDetail/);
  assert.match(pageSource, /sourceTruthActionHint/);
});

test('notify pages link blocked diagnostics to manager routing page', () => {
  const outboxSource = fs.readFileSync(outboxPagePath, 'utf8');
  const detailSource = fs.readFileSync(detailPagePath, 'utf8');
  assert.match(outboxSource, /查看店长路由/);
  assert.match(outboxSource, /\/mall\/booking\/review\/manager-routing/);
  assert.match(detailSource, /查看店长路由/);
  assert.match(detailSource, /\/mall\/booking\/review\/manager-routing/);
});
