import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8');
}

test('pages.json registers referral route', () => {
  const pages = read('pages.json');

  assert.match(pages, /"root": "pages\/referral"/);
  assert.match(pages, /"path": "index"/);
});

test('referral page exists and exposes binding plus ledger actions', () => {
  const source = read('pages/referral/index.vue');

  assert.match(source, /ReferralApi/);
  assert.match(source, /bindInviter|onBindInviter/);
  assert.match(source, /getOverview/);
  assert.match(source, /getRewardLedgerPage/);
  assert.match(source, /邀请有礼|奖励台账/);
});

test('commission center exposes a real referral entry', () => {
  const source = read('pages/commission/components/commission-menu.vue');

  assert.match(source, /\/pages\/referral\/index/);
  assert.match(source, /邀请有礼/);
});
