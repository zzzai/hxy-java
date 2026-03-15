import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function loadBookingLogic() {
  const logicPath = path.resolve(__dirname, '../pages/booking/logic.js');
  let source = fs.readFileSync(logicPath, 'utf8');

  source = source.replace(/export async function (\w+)\s*\(/g, 'async function $1(');
  source = source.replace(/export function (\w+)\s*\(/g, 'function $1(');
  source += `
module.exports = {
  loadTechnicianList,
  loadTechnicianDetail,
  loadTimeSlots,
  submitBookingOrder,
  cancelBookingOrder,
  submitAddonOrder,
  goToTechnicianDetail,
  goToOrderConfirm,
  goToOrderDetail,
};
`;

  const context = {
    module: { exports: {} },
    exports: {},
  };
  vm.runInNewContext(source, context, { filename: logicPath });
  return context.module.exports;
}

test('booking page smoke logic exports the expected helper surface', () => {
  const logic = loadBookingLogic();

  assert.equal(typeof logic.loadTechnicianList, 'function');
  assert.equal(typeof logic.loadTechnicianDetail, 'function');
  assert.equal(typeof logic.loadTimeSlots, 'function');
  assert.equal(typeof logic.submitBookingOrder, 'function');
  assert.equal(typeof logic.cancelBookingOrder, 'function');
  assert.equal(typeof logic.submitAddonOrder, 'function');
  assert.equal(typeof logic.goToTechnicianDetail, 'function');
  assert.equal(typeof logic.goToOrderConfirm, 'function');
  assert.equal(typeof logic.goToOrderDetail, 'function');
});
