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
  'index.vue'
);

test('review ledger exposes SLA reminder visibility for ops', () => {
  const source = fs.readFileSync(ledgerPagePath, 'utf8');
  assert.match(source, /SLA 提醒/);
  assert.match(source, /认领超时提醒/);
  assert.match(source, /首次处理超时提醒/);
  assert.match(source, /闭环超时提醒/);
});

test('review detail explains SLA reminder is dual-channel and still admin-only', () => {
  const source = fs.readFileSync(detailPagePath, 'utf8');
  assert.match(source, /SLA 提醒/);
  assert.match(source, /App \/ 企微/);
  assert.match(source, /仍不代表 booking review 已可放量/);
});
