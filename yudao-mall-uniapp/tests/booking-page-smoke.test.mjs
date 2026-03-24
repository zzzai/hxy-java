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
  loadTimeSlotDetail,
  loadReviewEligibility,
  submitBookingOrder,
  submitBookingOrderAndGo,
  cancelBookingOrder,
  cancelBookingOrderAndRefresh,
  submitAddonOrder,
  submitAddonOrderAndGo,
  goToTechnicianDetail,
  goToOrderConfirm,
  goToOrderDetail,
  goToReviewAdd,
};
`;

  const context = {
    module: { exports: {} },
    exports: {},
  };
  vm.runInNewContext(source, context, { filename: logicPath });
  return context.module.exports;
}

function normalize(value) {
  return JSON.parse(JSON.stringify(value));
}

test('booking page smoke logic exports the expected helper surface', () => {
  const logic = loadBookingLogic();

  assert.equal(typeof logic.loadTechnicianList, 'function');
  assert.equal(typeof logic.loadTechnicianDetail, 'function');
  assert.equal(typeof logic.loadTimeSlots, 'function');
  assert.equal(typeof logic.loadTimeSlotDetail, 'function');
  assert.equal(typeof logic.loadReviewEligibility, 'function');
  assert.equal(typeof logic.submitBookingOrder, 'function');
  assert.equal(typeof logic.submitBookingOrderAndGo, 'function');
  assert.equal(typeof logic.cancelBookingOrder, 'function');
  assert.equal(typeof logic.cancelBookingOrderAndRefresh, 'function');
  assert.equal(typeof logic.submitAddonOrder, 'function');
  assert.equal(typeof logic.submitAddonOrderAndGo, 'function');
  assert.equal(typeof logic.goToTechnicianDetail, 'function');
  assert.equal(typeof logic.goToOrderConfirm, 'function');
  assert.equal(typeof logic.goToOrderDetail, 'function');
  assert.equal(typeof logic.goToReviewAdd, 'function');
});

test('booking technician list and detail helpers call canonical apis', async () => {
  const logic = loadBookingLogic();
  const calls = [];
  const api = {
    getTechnicianList(storeId) {
      calls.push(['getTechnicianList', storeId]);
      return Promise.resolve({ code: 0, data: [{ id: 1 }] });
    },
    getTechnician(technicianId) {
      calls.push(['getTechnician', technicianId]);
      return Promise.resolve({ code: 0, data: { id: technicianId } });
    },
    getTimeSlots(technicianId, date) {
      calls.push(['getTimeSlots', technicianId, date]);
      return Promise.resolve({ code: 0, data: [{ id: 9, date }] });
    },
    getTimeSlot(timeSlotId) {
      calls.push(['getTimeSlot', timeSlotId]);
      return Promise.resolve({ code: 0, data: { id: timeSlotId, duration: 60 } });
    },
  };

  const listResult = await logic.loadTechnicianList(api, 12);
  const technicianResult = await logic.loadTechnicianDetail(api, 8);
  const slotResult = await logic.loadTimeSlots(api, 8, '2026-03-15');
  const slotDetailResult = await logic.loadTimeSlotDetail(api, 9);

  assert.deepEqual(normalize(calls), [
    ['getTechnicianList', 12],
    ['getTechnician', 8],
    ['getTimeSlots', 8, '2026-03-15'],
    ['getTimeSlot', 9],
  ]);
  assert.deepEqual(normalize(listResult), { code: 0, data: [{ id: 1 }] });
  assert.deepEqual(normalize(technicianResult), { code: 0, data: { id: 8 } });
  assert.deepEqual(normalize(slotResult), { code: 0, data: [{ id: 9, date: '2026-03-15' }] });
  assert.deepEqual(normalize(slotDetailResult), { code: 0, data: { id: 9, duration: 60 } });
});

test('booking review eligibility helper calls review api and review navigation keeps canonical route', async () => {
  const logic = loadBookingLogic();
  const calls = [];
  const api = {
    getEligibility(bookingOrderId) {
      calls.push(['getEligibility', bookingOrderId]);
      return Promise.resolve({ code: 0, data: { eligible: true } });
    },
  };
  const routeCalls = [];
  const router = {
    go(route, query) {
      routeCalls.push({ route, query });
      return { route, query };
    },
  };

  const eligibility = await logic.loadReviewEligibility(api, 1001);
  const nav = logic.goToReviewAdd(router, 1001);

  assert.deepEqual(normalize(calls), [['getEligibility', 1001]]);
  assert.deepEqual(normalize(eligibility), { code: 0, data: { eligible: true } });
  assert.deepEqual(normalize(routeCalls), [{
    route: '/pages/booking/review-add',
    query: { bookingOrderId: 1001 },
  }]);
  assert.deepEqual(normalize(nav), {
    route: '/pages/booking/review-add',
    query: { bookingOrderId: 1001 },
  });
});

test('booking technician navigation helpers keep canonical routes and query keys', () => {
  const logic = loadBookingLogic();
  const routeCalls = [];
  const router = {
    go(route, query) {
      routeCalls.push({ route, query });
      return { route, query };
    },
  };

  const detailNav = logic.goToTechnicianDetail(router, 22, 7);
  const confirmNav = logic.goToOrderConfirm(router, {
    timeSlotId: 101,
    technicianId: 22,
    storeId: 7,
  });

  assert.deepEqual(normalize(routeCalls), [
    {
      route: '/pages/booking/technician-detail',
      query: { id: 22, storeId: 7 },
    },
    {
      route: '/pages/booking/order-confirm',
      query: { timeSlotId: 101, technicianId: 22, storeId: 7 },
    },
  ]);
  assert.deepEqual(normalize(detailNav), {
    route: '/pages/booking/technician-detail',
    query: { id: 22, storeId: 7 },
  });
  assert.deepEqual(normalize(confirmNav), {
    route: '/pages/booking/order-confirm',
    query: { timeSlotId: 101, technicianId: 22, storeId: 7 },
  });
});

test('booking submit helper adds dispatchMode and jumps to order detail on success', async () => {
  const logic = loadBookingLogic();
  const apiCalls = [];
  const routeCalls = [];
  const api = {
    createOrder(payload) {
      apiCalls.push(payload);
      return Promise.resolve({ code: 0, data: 5566 });
    },
  };
  const router = {
    go(route, query) {
      routeCalls.push({ route, query });
    },
  };

  const result = await logic.submitBookingOrderAndGo(api, router, {
    timeSlotId: 10,
    spuId: 20,
    skuId: 30,
    userRemark: 'test',
  });

  assert.deepEqual(normalize(apiCalls), [{
    timeSlotId: 10,
    spuId: 20,
    skuId: 30,
    userRemark: 'test',
    dispatchMode: 1,
  }]);
  assert.deepEqual(normalize(routeCalls), [{
    route: '/pages/booking/order-detail',
    query: { id: 5566 },
  }]);
  assert.deepEqual(normalize(result), { code: 0, data: 5566 });
});

test('booking submit helper does not jump to order detail on failure', async () => {
  const logic = loadBookingLogic();
  const apiCalls = [];
  const routeCalls = [];
  const api = {
    createOrder(payload) {
      apiCalls.push(payload);
      return Promise.resolve({ code: 9001, msg: 'CREATE_BLOCKED' });
    },
  };
  const router = {
    go(route, query) {
      routeCalls.push({ route, query });
    },
  };

  const result = await logic.submitBookingOrderAndGo(api, router, {
    timeSlotId: 10,
    userRemark: 'failure-case',
  });

  assert.deepEqual(normalize(apiCalls), [{
    timeSlotId: 10,
    userRemark: 'failure-case',
    dispatchMode: 1,
  }]);
  assert.deepEqual(normalize(routeCalls), []);
  assert.deepEqual(normalize(result), { code: 9001, msg: 'CREATE_BLOCKED' });
});

test('booking cancel helper uses canonical reason and refreshes on success', async () => {
  const logic = loadBookingLogic();
  const apiCalls = [];
  const refreshCalls = [];
  const api = {
    cancelOrder(id, reason) {
      apiCalls.push({ id, reason });
      return Promise.resolve({ code: 0 });
    },
  };

  const result = await logic.cancelBookingOrderAndRefresh(api, 88, async () => {
    refreshCalls.push('refreshed');
  });

  assert.deepEqual(normalize(apiCalls), [{
    id: 88,
    reason: '用户主动取消',
  }]);
  assert.deepEqual(normalize(refreshCalls), ['refreshed']);
  assert.deepEqual(normalize(result), { code: 0 });
});

test('booking cancel helper does not refresh on failure', async () => {
  const logic = loadBookingLogic();
  const apiCalls = [];
  const refreshCalls = [];
  const api = {
    cancelOrder(id, reason) {
      apiCalls.push({ id, reason });
      return Promise.resolve({ code: 9102, msg: 'CANCEL_BLOCKED' });
    },
  };

  const result = await logic.cancelBookingOrderAndRefresh(api, 88, async () => {
    refreshCalls.push('refreshed');
  });

  assert.deepEqual(normalize(apiCalls), [{
    id: 88,
    reason: '用户主动取消',
  }]);
  assert.deepEqual(normalize(refreshCalls), []);
  assert.deepEqual(normalize(result), { code: 9102, msg: 'CANCEL_BLOCKED' });
});

test('booking addon helper posts canonical payload and jumps to order detail on success', async () => {
  const logic = loadBookingLogic();
  const apiCalls = [];
  const routeCalls = [];
  const api = {
    createAddonOrder(payload) {
      apiCalls.push(payload);
      return Promise.resolve({ code: 0, data: 7788 });
    },
  };
  const router = {
    go(route, query) {
      routeCalls.push({ route, query });
    },
  };

  const result = await logic.submitAddonOrderAndGo(api, router, {
    parentOrderId: 99,
    addonType: 2,
  });

  assert.deepEqual(normalize(apiCalls), [{
    parentOrderId: 99,
    addonType: 2,
  }]);
  assert.deepEqual(normalize(routeCalls), [{
    route: '/pages/booking/order-detail',
    query: { id: 7788 },
  }]);
  assert.deepEqual(normalize(result), { code: 0, data: 7788 });
});

test('booking addon helper does not jump to order detail on failure', async () => {
  const logic = loadBookingLogic();
  const apiCalls = [];
  const routeCalls = [];
  const api = {
    createAddonOrder(payload) {
      apiCalls.push(payload);
      return Promise.resolve({ code: 9203, msg: 'ADDON_BLOCKED' });
    },
  };
  const router = {
    go(route, query) {
      routeCalls.push({ route, query });
    },
  };

  const result = await logic.submitAddonOrderAndGo(api, router, {
    parentOrderId: 99,
    addonType: 2,
  });

  assert.deepEqual(normalize(apiCalls), [{
    parentOrderId: 99,
    addonType: 2,
  }]);
  assert.deepEqual(normalize(routeCalls), []);
  assert.deepEqual(normalize(result), { code: 9203, msg: 'ADDON_BLOCKED' });
});
