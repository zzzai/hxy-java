import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');

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
  'commission.ts',
);

const pagePath = path.join(
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
  'commission',
  'index.vue',
);

const menuSqlPath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master',
  'sql',
  'mysql',
  'hxy',
  '2026-03-24-hxy-booking-commission-admin-menu.sql',
);

test('bo-004 admin api file exists and binds canonical commission endpoints', () => {
  assert.ok(fs.existsSync(apiPath), 'missing bo-004 api file');
  const source = fs.readFileSync(apiPath, 'utf8');

  [
    '/booking/commission/list-by-technician',
    '/booking/commission/list-by-order',
    '/booking/commission/pending-amount',
    '/booking/commission/settle',
    '/booking/commission/batch-settle',
    '/booking/commission/config/list',
    '/booking/commission/config/save',
    '/booking/commission/config/delete',
  ].forEach((endpoint) => {
    assert.match(source, new RegExp(endpoint.replace(/\//g, '\\/')));
  });

  assert.match(source, /export interface TechnicianCommission/);
  assert.match(source, /export interface TechnicianCommissionConfig/);
  assert.match(source, /export interface TechnicianCommissionConfigSaveReq/);
  assert.doesNotMatch(source, /\bdegraded\b/);
  assert.doesNotMatch(source, /\bdegradeReason\b/);
});

test('bo-004 admin page exists and contains write-after-read safeguards', () => {
  assert.ok(fs.existsSync(pagePath), 'missing bo-004 admin page');
  const source = fs.readFileSync(pagePath, 'utf8');

  assert.match(source, /技师提成明细/);
  assert.match(source, /计提管理/);
  assert.match(source, /写后回读/);
  assert.match(source, /接口返回成功但读后未变/);
  assert.match(source, /单条直结/);
  assert.match(source, /批量直结/);
  assert.match(source, /门店佣金配置/);
  assert.match(source, /getCommissionListByTechnician/);
  assert.match(source, /getCommissionListByOrder/);
  assert.match(source, /getPendingCommissionAmount/);
  assert.match(source, /saveCommissionConfig/);
  assert.match(source, /deleteCommissionConfig/);
  assert.match(source, /settleCommission/);
  assert.match(source, /batchSettleCommission/);
});

test('bo-004 menu sql exists with independent route and correct permissions', () => {
  assert.ok(fs.existsSync(menuSqlPath), 'missing bo-004 menu sql');
  const source = fs.readFileSync(menuSqlPath, 'utf8');

  assert.match(source, /booking-commission/);
  assert.match(source, /mall\/booking\/commission\/index/);
  assert.match(source, /booking:commission:query/);
  assert.match(source, /booking:commission:settle/);
  assert.match(source, /booking:commission:config/);
  assert.match(source, /system_role_menu/);
});
