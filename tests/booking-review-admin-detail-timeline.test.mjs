import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import fs from 'node:fs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const helperPath = path.join(
  __dirname,
  '..',
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
  'timelineHelpers.mjs'
);

const { buildReviewDetailTimeline } = await import(pathToFileURL(helperPath).href);

const detailPagePath = path.join(
  __dirname,
  '..',
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
  'index.vue'
);

test('timeline helper returns only submit node when rest missing', () => {
  const review = {
    submitTime: '2026-03-20 10:00:00',
    overallScore: 4,
    reviewLevel: 2
  };
  const result = buildReviewDetailTimeline(review);
  assert.equal(result.timelineItems.length, 1);
  assert.equal(result.timelineItems[0].label, '提交评价');
  assert.equal(result.summaryItems.some(item => item.label === '当前跟进状态'), true);
});

test('timeline order respects reply, claim, close', () => {
  const review = {
    submitTime: '2026-03-20 10:00:00',
    replyTime: '2026-03-20 11:00:00',
    managerClaimedAt: '2026-03-20 12:00:00',
    managerClosedAt: '2026-03-20 13:00:00'
  };
  const result = buildReviewDetailTimeline(review);
  const labels = result.timelineItems.map(item => item.label);
  assert.deepEqual(labels, ['提交评价', '已正式回复用户', '店长待办已认领', '店长待办已闭环']);
});

test('managerLatestActionRemark only appears in summary', () => {
  const review = {
    submitTime: '2026-03-20 10:00:00',
    managerLatestActionRemark: '跟进中',
    followStatus: 2
  };
  const result = buildReviewDetailTimeline(review);
  assert.equal(result.timelineItems.some(item => item.description.includes('跟进中')), false);
  assert.equal(result.summaryItems.some(item => item.value === '跟进中'), true);
});

test('followStatus does not create extra timeline nodes', () => {
  const review = {
    submitTime: '2026-03-20 10:00:00',
    followStatus: 3
  };
  const result = buildReviewDetailTimeline(review);
  assert.equal(result.timelineItems.length, 1);
});

test('detail page references helper and renders new block', () => {
  const detailSource = fs.readFileSync(detailPagePath, 'utf8');
  assert.match(detailSource, /buildReviewDetailTimeline/);
  assert.match(detailSource, /最近动作时间线/);
  assert.match(detailSource, /当前状态摘要/);
});
