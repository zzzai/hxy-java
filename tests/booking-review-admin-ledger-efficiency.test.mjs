import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

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

test('review ledger exposes priority and notify risk columns', () => {
  const source = fs.readFileSync(ledgerPagePath, 'utf8');
  assert.match(source, /优先级/);
  assert.match(source, /优先级原因/);
  assert.match(source, /通知风险/);
});

test('review dashboard exposes due soon cards for ops watch', () => {
  const source = fs.readFileSync(dashboardPagePath, 'utf8');
  assert.match(source, /即将认领超时/);
  assert.match(source, /即将首次处理超时/);
  assert.match(source, /即将闭环超时/);
});

test('review api types include sla priority and notify risk fields', () => {
  const source = fs.readFileSync(reviewApiPath, 'utf8');
  assert.match(source, /managerSlaStage\?: string/);
  assert.match(source, /priorityLevel\?: string/);
  assert.match(source, /priorityReason\?: string/);
  assert.match(source, /notifyRiskSummary\?: string/);
  assert.match(source, /managerTodoClaimDueSoonCount\?: number/);
  assert.match(source, /managerTodoFirstActionDueSoonCount\?: number/);
  assert.match(source, /managerTodoCloseDueSoonCount\?: number/);
});
