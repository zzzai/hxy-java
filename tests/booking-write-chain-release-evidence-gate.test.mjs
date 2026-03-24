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
  'check_booking_write_chain_release_evidence_gate.sh',
);

const samplePackDir = path.join(
  repoRoot,
  'tests',
  'fixtures',
  'booking-write-chain-release-evidence-simulated',
);

const requiredFiles = [
  'technician-list-success.json',
  'slot-list-success.json',
  'create-success.json',
  'create-conflict.json',
  'cancel-success.json',
  'addon-success.json',
  'addon-conflict.json',
  'gray-stage.json',
  'rollback-drill.json',
  'signoff.json',
];

test('booking write-chain simulated release evidence pack exists with required files', () => {
  assert.ok(fs.existsSync(samplePackDir), 'missing booking write-chain simulated evidence pack directory');
  requiredFiles.forEach((filename) => {
    const filePath = path.join(samplePackDir, filename);
    assert.ok(fs.existsSync(filePath), `missing booking write-chain evidence file: ${filename}`);
    const source = fs.readFileSync(filePath, 'utf8').trim();
    assert.notEqual(source, '', `booking write-chain evidence file is empty: ${filename}`);
  });
});

test('booking write-chain release evidence gate script validates simulated evidence pack', () => {
  assert.ok(fs.existsSync(gateScriptPath), 'missing booking write-chain release evidence gate script');

  const output = execFileSync(
    gateScriptPath,
    ['--repo-root', repoRoot, '--sample-pack-dir', samplePackDir],
    { encoding: 'utf8' },
  );

  assert.match(output, /\[booking-write-chain-evidence-gate\] result=PASS/);
  assert.match(output, /sample_pack_dir=/);
});
