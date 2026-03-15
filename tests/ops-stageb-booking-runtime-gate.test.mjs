import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');

const localCiScript = fs.readFileSync(
  path.join(repoRoot, 'ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh'),
  'utf8',
);
const workflow = fs.readFileSync(
  path.join(repoRoot, '.github/workflows/ops-stageb-p1-guard.yml'),
  'utf8',
);

test('shared local CI includes booking miniapp runtime gate toggles and invocation', () => {
  assert.match(localCiScript, /RUN_BOOKING_MINIAPP_RUNTIME_GATE/);
  assert.match(localCiScript, /REQUIRE_BOOKING_MINIAPP_RUNTIME_GATE/);
  assert.match(localCiScript, /check_booking_miniapp_runtime_gate\.sh/);
  assert.match(localCiScript, /booking_miniapp_runtime_gate_rc=/);
});

test('workflow watches booking runtime gate inputs and reports booking runtime rc', () => {
  assert.match(workflow, /ruoyi-vue-pro-master\/script\/dev\/check_booking_miniapp_runtime_gate\.sh/);
  assert.match(workflow, /yudao-mall-uniapp\/sheep\/api\/trade\/booking\.js/);
  assert.match(workflow, /yudao-mall-uniapp\/pages\/booking\/\*\*/);
  assert.match(workflow, /yudao-mall-uniapp\/tests\/booking-api-alignment\.test\.mjs/);
  assert.match(workflow, /yudao-mall-uniapp\/tests\/booking-page-smoke\.test\.mjs/);
  assert.match(workflow, /booking_miniapp_runtime_gate_rc/);
});
