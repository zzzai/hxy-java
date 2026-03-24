import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8');
}

test('member missing pages are registered in pages.json', () => {
  const pagesJson = read('pages.json');

  assert.match(pagesJson, /"path": "level"/);
  assert.match(pagesJson, /"root": "pages\/profile"/);
  assert.match(pagesJson, /"path": "assets"/);
  assert.match(pagesJson, /"path": "tag"/);
});

test('member page files exist', () => {
  assert.equal(fs.existsSync(path.join(projectRoot, 'pages/user/level.vue')), true);
  assert.equal(fs.existsSync(path.join(projectRoot, 'pages/profile/assets.vue')), true);
  assert.equal(fs.existsSync(path.join(projectRoot, 'pages/user/tag.vue')), true);
});

test('member info page exposes real entry links for level assets and tags', () => {
  const userInfoPage = read('pages/user/info.vue');

  assert.match(userInfoPage, /\/pages\/user\/level/);
  assert.match(userInfoPage, /\/pages\/profile\/assets/);
  assert.match(userInfoPage, /\/pages\/user\/tag/);
});
