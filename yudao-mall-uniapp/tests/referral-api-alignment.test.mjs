import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const projectRoot = path.resolve(import.meta.dirname, '..');

function loadReferralApi() {
  const apiPath = path.join(projectRoot, 'sheep/api/promotion/referral.js');
  let source = fs.readFileSync(apiPath, 'utf8');
  source = source.replace(
    "import request from '@/sheep/request';",
    'const request = (config) => config;'
  );
  source = source.replace(/export default ReferralApi;\s*$/, 'module.exports = ReferralApi;');
  const context = { module: { exports: {} }, exports: {} };
  vm.runInNewContext(source, context, { filename: apiPath });
  return context.module.exports;
}

function normalize(value) {
  return JSON.parse(JSON.stringify(value));
}

test('referral api aligns bind overview and reward ledger routes', () => {
  const referralApi = loadReferralApi();
  const bindPayload = { inviterMemberId: 12, clientToken: 'ref-bind-001' };

  assert.deepEqual(normalize(referralApi.bindInviter(bindPayload)), {
    url: '/promotion/referral/bind-inviter',
    method: 'POST',
    data: bindPayload,
    custom: { auth: true, showLoading: true },
  });

  assert.deepEqual(normalize(referralApi.getOverview()), {
    url: '/promotion/referral/overview',
    method: 'GET',
    custom: { auth: true, showLoading: false },
  });

  assert.deepEqual(normalize(referralApi.getRewardLedgerPage({ pageNo: 1, pageSize: 10, status: 'SETTLED' })), {
    url: '/promotion/referral/reward-ledger/page',
    method: 'GET',
    params: { pageNo: 1, pageSize: 10, status: 'SETTLED' },
    custom: { auth: true, showLoading: false },
  });
});
