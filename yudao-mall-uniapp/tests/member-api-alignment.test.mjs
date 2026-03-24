import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

function loadApi(relativePath, exportName) {
  const apiPath = path.join(projectRoot, relativePath);
  let source = fs.readFileSync(apiPath, 'utf8');
  source = source.replace("import request from '@/sheep/request';", 'const request = (config) => config;');
  source = source.replace(new RegExp(`export default ${exportName};\\s*$`), `module.exports = ${exportName};`);

  const context = {
    module: { exports: {} },
    exports: {},
  };
  vm.runInNewContext(source, context, { filename: apiPath });
  return context.module.exports;
}

function normalize(value) {
  return JSON.parse(JSON.stringify(value));
}

test('member level api keeps canonical routes', () => {
  const api = loadApi('sheep/api/member/level.js', 'MemberLevelApi');

  assert.deepEqual(normalize(api.getLevelList()), {
    url: '/member/level/list',
    method: 'GET',
    custom: { auth: true, showLoading: false },
  });

  assert.deepEqual(normalize(api.getExperienceRecordPage({ pageNo: 2, pageSize: 10 })), {
    url: '/member/experience-record/page',
    method: 'GET',
    params: { pageNo: 2, pageSize: 10 },
    custom: { auth: true, showLoading: false },
  });
});

test('member asset api keeps canonical route and optional assetType', () => {
  const api = loadApi('sheep/api/member/asset.js', 'MemberAssetApi');

  assert.deepEqual(normalize(api.getAssetLedgerPage({ pageNo: 1, pageSize: 20, assetType: 'POINT' })), {
    url: '/member/asset-ledger/page',
    method: 'GET',
    params: { pageNo: 1, pageSize: 20, assetType: 'POINT' },
    custom: { auth: true, showLoading: false },
  });
});

test('member tag api keeps canonical route', () => {
  const api = loadApi('sheep/api/member/tag.js', 'MemberTagApi');

  assert.deepEqual(normalize(api.getMyTags()), {
    url: '/member/tag/my',
    method: 'GET',
    custom: { auth: true, showLoading: false },
  });
});
