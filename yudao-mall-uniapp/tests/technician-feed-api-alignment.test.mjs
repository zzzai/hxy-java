import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const projectRoot = path.resolve(import.meta.dirname, '..');

function loadTechnicianFeedApi() {
  const apiPath = path.join(projectRoot, 'sheep/api/trade/technicianFeed.js');
  let source = fs.readFileSync(apiPath, 'utf8');
  source = source.replace(
    "import request from '@/sheep/request';",
    'const request = (config) => config;'
  );
  source = source.replace(/export default TechnicianFeedApi;\s*$/, 'module.exports = TechnicianFeedApi;');
  const context = { module: { exports: {} }, exports: {} };
  vm.runInNewContext(source, context, { filename: apiPath });
  return context.module.exports;
}

function normalize(value) {
  return JSON.parse(JSON.stringify(value));
}

test('technician feed api aligns page like and comment routes', () => {
  const technicianFeedApi = loadTechnicianFeedApi();
  const likePayload = { postId: 10, action: 1, clientToken: 'like-001' };
  const commentPayload = { postId: 10, content: '手法很好', clientToken: 'comment-001' };

  assert.deepEqual(normalize(technicianFeedApi.getFeedPage({ storeId: 9, pageNo: 1, pageSize: 10 })), {
    url: '/booking/technician/feed/page',
    method: 'GET',
    params: { storeId: 9, pageNo: 1, pageSize: 10 },
    custom: { showLoading: false },
  });

  assert.deepEqual(normalize(technicianFeedApi.toggleLike(likePayload)), {
    url: '/booking/technician/feed/like',
    method: 'POST',
    data: likePayload,
    custom: { auth: true, showLoading: false },
  });

  assert.deepEqual(normalize(technicianFeedApi.createComment(commentPayload)), {
    url: '/booking/technician/feed/comment/create',
    method: 'POST',
    data: commentPayload,
    custom: { auth: true, showLoading: true },
  });
});
