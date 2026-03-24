import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');

const gateScriptPath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master',
  'script',
  'dev',
  'check_reserved_runtime_release_evidence_gate.sh',
);

const samplePackDir = path.join(
  repoRoot,
  'tests',
  'fixtures',
  'reserved-runtime-release-evidence-simulated',
);

const requiredFiles = [
  'gift-card-template-page.json',
  'gift-card-order-create.json',
  'referral-overview.json',
  'referral-bind-inviter.json',
  'technician-feed-page.json',
  'technician-feed-comment-create.json',
  'switch-snapshot.json',
  'gray-stage.json',
  'rollback-drill.json',
  'signoff.json',
];

test('reserved runtime simulated release evidence pack exists with required files', () => {
  assert.ok(fs.existsSync(samplePackDir), 'missing reserved runtime simulated evidence pack directory');
  requiredFiles.forEach((filename) => {
    const filePath = path.join(samplePackDir, filename);
    assert.ok(fs.existsSync(filePath), `missing reserved runtime evidence file: ${filename}`);
    const source = fs.readFileSync(filePath, 'utf8').trim();
    assert.notEqual(source, '', `reserved runtime evidence file is empty: ${filename}`);
  });
});

test('reserved runtime release evidence gate script validates simulated evidence pack', () => {
  assert.ok(fs.existsSync(gateScriptPath), 'missing reserved runtime release evidence gate script');

  const output = execFileSync(
    gateScriptPath,
    ['--repo-root', repoRoot, '--sample-pack-dir', samplePackDir],
    { encoding: 'utf8' },
  );

  assert.match(output, /\[reserved-runtime-evidence-gate\] result=PASS/);
  assert.match(output, /sample_pack_dir=/);
});
