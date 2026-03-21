import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');
const helperPath = path.join(
  repoRoot,
  'ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/queryHelpers.mjs',
);

const loadHelpers = async () => {
  try {
    return await import(pathToFileURL(helperPath).href);
  } catch (error) {
    assert.fail(`query helper module unavailable: ${error.message}`);
  }
};

test('dashboard negative card maps to review ledger filter', async () => {
  const { buildLedgerQueryFromDashboardCardKey } = await loadHelpers();
  assert.deepEqual(buildLedgerQueryFromDashboardCardKey('negative'), {
    reviewLevel: '3',
  });
});

test('dashboard manager todo timeout card maps to manager todo ledger filter', async () => {
  const { buildLedgerQueryFromDashboardCardKey } = await loadHelpers();
  assert.deepEqual(buildLedgerQueryFromDashboardCardKey('managerTodoFirstActionTimeout'), {
    onlyManagerTodo: 'true',
    managerSlaStatus: 'FIRST_ACTION_TIMEOUT',
  });
});

test('dashboard manager todo due soon card maps to manager todo ledger filter', async () => {
  const { buildLedgerQueryFromDashboardCardKey } = await loadHelpers();
  assert.deepEqual(buildLedgerQueryFromDashboardCardKey('managerTodoCloseDueSoon'), {
    onlyManagerTodo: 'true',
    managerSlaStatus: 'CLOSE_DUE_SOON',
  });
});

test('ledger route query is parsed into typed filter state', async () => {
  const { parseLedgerQuery } = await loadHelpers();
  assert.deepEqual(
    parseLedgerQuery({
      pageNo: '3',
      pageSize: '20',
      reviewLevel: '3',
      onlyManagerTodo: 'true',
      managerTodoStatus: '1',
      replyStatus: 'false',
      managerSlaStatus: 'CLAIM_TIMEOUT',
    }),
    {
      pageNo: 3,
      pageSize: 20,
      id: undefined,
      bookingOrderId: undefined,
      storeId: undefined,
      technicianId: undefined,
      memberId: undefined,
      reviewLevel: 3,
      riskLevel: undefined,
      followStatus: undefined,
      onlyManagerTodo: true,
      onlyPendingInit: undefined,
      managerTodoStatus: 1,
      managerSlaStatus: 'CLAIM_TIMEOUT',
      replyStatus: false,
      submitTime: undefined,
    },
  );
});

test('ledger route query keeps due soon sla stage as string filter', async () => {
  const { parseLedgerQuery } = await loadHelpers();
  assert.equal(
    parseLedgerQuery({
      managerSlaStatus: 'CLAIM_DUE_SOON',
    }).managerSlaStatus,
    'CLAIM_DUE_SOON',
  );
});

test('ledger route query parses onlyPendingInit flag', async () => {
  const { parseLedgerQuery } = await loadHelpers();
  assert.deepEqual(
    parseLedgerQuery({
      onlyPendingInit: 'true',
    }),
    {
      pageNo: 1,
      pageSize: 10,
      id: undefined,
      bookingOrderId: undefined,
      storeId: undefined,
      technicianId: undefined,
      memberId: undefined,
      reviewLevel: undefined,
      riskLevel: undefined,
      followStatus: undefined,
      onlyManagerTodo: undefined,
      onlyPendingInit: true,
      managerTodoStatus: undefined,
      managerSlaStatus: undefined,
      replyStatus: undefined,
      submitTime: undefined,
    },
  );
});
