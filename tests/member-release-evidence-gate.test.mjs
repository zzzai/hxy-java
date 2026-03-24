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
  'check_member_release_evidence_gate.sh',
);

const samplePackDir = path.join(
  repoRoot,
  'tests',
  'fixtures',
  'member-release-evidence-simulated',
);

const requiredFiles = [
  'level-page-sample.json',
  'level-experience-page.json',
  'asset-ledger-page.json',
  'tag-page-sample.json',
  'switch-snapshot.json',
  'gray-stage.json',
  'rollback-drill.json',
  'signoff.json',
];

test('member simulated release evidence pack exists with required files', () => {
  assert.ok(fs.existsSync(samplePackDir), 'missing member simulated evidence pack directory');
  requiredFiles.forEach((filename) => {
    const filePath = path.join(samplePackDir, filename);
    assert.ok(fs.existsSync(filePath), `missing member evidence file: ${filename}`);
    const source = fs.readFileSync(filePath, 'utf8').trim();
    assert.notEqual(source, '', `member evidence file is empty: ${filename}`);
  });
});

test('member release evidence gate script validates simulated evidence pack', () => {
  assert.ok(fs.existsSync(gateScriptPath), 'missing member release evidence gate script');

  const output = execFileSync(
    gateScriptPath,
    ['--repo-root', repoRoot, '--sample-pack-dir', samplePackDir],
    { encoding: 'utf8' },
  );

  assert.match(output, /\[member-evidence-gate\] result=PASS/);
  assert.match(output, /sample_pack_dir=/);
});
