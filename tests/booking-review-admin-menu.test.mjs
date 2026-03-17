import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');
const menuSqlPath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-17-hxy-booking-review-menu.sql',
);

test('booking review admin menu sql exists with route and permission grants', () => {
  assert.ok(fs.existsSync(menuSqlPath), 'missing booking review admin menu sql');
  const source = fs.readFileSync(menuSqlPath, 'utf8');

  assert.match(source, /booking-review/);
  assert.match(source, /mall\/booking\/review\/index/);
  assert.match(source, /booking:review:query/);
  assert.match(source, /booking:review:update/);
  assert.match(source, /system_role_menu/);
});
