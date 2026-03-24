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
  'check_bo004_admin_release_evidence_gate.sh',
);

const samplePackDir = path.join(
  repoRoot,
  'tests',
  'fixtures',
  'bo004-admin-release-evidence-simulated',
);

const requiredFiles = [
  'menu-navigation.json',
  'query-list-by-technician.json',
  'write-settle-readback.json',
  'config-save-readback.json',
  'gray-stage.json',
  'rollback-drill.json',
  'signoff.json',
];

test('bo-004 simulated release evidence pack exists with required files', () => {
  assert.ok(fs.existsSync(samplePackDir), 'missing simulated evidence pack directory');
  requiredFiles.forEach((filename) => {
    const filePath = path.join(samplePackDir, filename);
    assert.ok(fs.existsSync(filePath), `missing simulated evidence file: ${filename}`);
    const source = fs.readFileSync(filePath, 'utf8').trim();
    assert.notEqual(source, '', `evidence file is empty: ${filename}`);
  });
});

test('bo-004 release evidence gate script validates simulated evidence pack', () => {
  assert.ok(fs.existsSync(gateScriptPath), 'missing bo-004 release evidence gate script');

  const output = execFileSync(
    gateScriptPath,
    ['--repo-root', repoRoot, '--sample-pack-dir', samplePackDir],
    { encoding: 'utf8' },
  );

  assert.match(output, /\[bo004-admin-evidence-gate\] result=PASS/);
  assert.match(output, /sample_pack_dir=/);
});
