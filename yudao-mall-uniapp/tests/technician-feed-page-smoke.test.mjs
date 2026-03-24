import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8');
}

test('pages.json registers technician feed route', () => {
  const pages = read('pages.json');

  assert.match(pages, /"root": "pages\/technician"/);
  assert.match(pages, /"path": "feed"/);
});

test('technician feed page exists and exposes list like and comment actions', () => {
  const source = read('pages/technician/feed.vue');

  assert.match(source, /TechnicianFeedApi/);
  assert.match(source, /getFeedPage/);
  assert.match(source, /toggleLike/);
  assert.match(source, /createComment/);
  assert.match(source, /技师动态|发表评论/);
});

test('technician detail page exposes a real technician feed entry', () => {
  const source = read('pages/booking/technician-detail.vue');

  assert.match(source, /\/pages\/technician\/feed/);
  assert.match(source, /技师动态/);
});
