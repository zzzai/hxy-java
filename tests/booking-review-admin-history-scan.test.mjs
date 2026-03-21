import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '..');

const apiFilePath = path.join(
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

const historyScanPagePath = path.join(
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
  'historyScan',
  'index.vue'
);

test('review api exposes history scan types and request helper', () => {
  const source = fs.readFileSync(apiFilePath, 'utf8');
  assert.match(source, /export interface BookingReviewHistoryScanReq/);
  assert.match(source, /export interface BookingReviewHistoryScanSummary/);
  assert.match(source, /export interface BookingReviewHistoryScanItem/);
  assert.match(source, /export const getReviewHistoryScan = async/);
  assert.match(source, /\/booking\/review\/history-scan/);
});

test('history scan page shows manual trigger, summary cards and risk guidance', () => {
  const source = fs.readFileSync(historyScanPagePath, 'utf8');
  assert.match(source, /开始扫描/);
  assert.match(source, /getReviewHistoryScan/);
  assert.match(source, /只识别治理候选/);
  assert.match(source, /不会自动修复历史数据/);
  assert.match(source, /扫描总量/);
  assert.match(source, /可人工推进/);
  assert.match(source, /高风险待核实/);
  assert.match(source, /不在本轮范围/);
  assert.match(source, /查看详情/);
});

test('review ledger page links to history scan route', () => {
  const source = fs.readFileSync(ledgerPagePath, 'utf8');
  assert.match(source, /历史治理扫描/);
  assert.match(source, /\/mall\/booking\/review\/history-scan/);
});
