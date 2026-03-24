import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');
const docsRoot = path.join(repoRoot, 'docs', 'products', 'miniapp');

const expectedDocs = [
  '2026-03-23-miniapp-community-store-private-domain-overview-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-public-to-private-acquisition-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-membership-consent-and-lead-capture-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-in-store-conversion-touchpoint-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-wecom-community-retention-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-member-segmentation-task-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-social-fission-growth-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-store-ops-sop-prd-v1.md',
  '2026-03-23-miniapp-community-store-private-domain-metrics-attribution-delivery-plan-v1.md',
];

const overviewDoc = path.join(docsRoot, expectedDocs[0]);
const topicalDocs = expectedDocs.slice(1);

test('community store private domain 9-doc pack exists', () => {
  expectedDocs.forEach((filename) => {
    const filePath = path.join(docsRoot, filename);
    assert.ok(fs.existsSync(filePath), `missing private-domain doc: ${filename}`);
    const source = fs.readFileSync(filePath, 'utf8').trim();
    assert.notEqual(source, '', `empty private-domain doc: ${filename}`);
  });
});

test('community store private domain overview references all topical docs', () => {
  assert.ok(fs.existsSync(overviewDoc), 'missing overview doc');
  const source = fs.readFileSync(overviewDoc, 'utf8');
  topicalDocs.forEach((filename) => {
    assert.match(source, new RegExp(filename.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  });
  assert.match(source, /当前真值/);
  assert.match(source, /开发优先级/);
});
