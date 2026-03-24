import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function loadBookingApi() {
  const bookingPath = path.resolve(__dirname, '../sheep/api/trade/booking.js');
  let source = fs.readFileSync(bookingPath, 'utf8');
  source = source.replace(
    "import request from '@/sheep/request';",
    'const request = (config) => config;'
  );
  source = source.replace(/export default BookingApi;\s*$/, 'module.exports = BookingApi;');

  const context = {
    module: { exports: {} },
    exports: {},
  };
  vm.runInNewContext(source, context, { filename: bookingPath });
  return context.module.exports;
}

function normalize(value) {
  return JSON.parse(JSON.stringify(value));
}

test('booking api aligns technician list and slot list routes', () => {
  const bookingApi = loadBookingApi();

  assert.deepEqual(normalize(bookingApi.getTechnicianList(12)), {
    url: '/booking/technician/list',
    method: 'GET',
    params: { storeId: 12 },
    custom: { showLoading: true },
  });

  assert.deepEqual(normalize(bookingApi.getTimeSlots(8, '2026-03-15')), {
    url: '/booking/slot/list-by-technician',
    method: 'GET',
    params: { technicianId: 8, date: '2026-03-15' },
    custom: { showLoading: false },
  });

  assert.deepEqual(normalize(bookingApi.getTimeSlot(18)), {
    url: '/booking/slot/get',
    method: 'GET',
    params: { id: 18 },
    custom: { showLoading: false },
  });
});

test('booking cancel uses POST params and addon uses app-api route', () => {
  const bookingApi = loadBookingApi();

  assert.deepEqual(normalize(bookingApi.cancelOrder(99, '用户主动取消')), {
    url: '/booking/order/cancel',
    method: 'POST',
    params: { id: 99, reason: '用户主动取消' },
    custom: { auth: true },
  });

  const addonPayload = { parentOrderId: 1, addonType: 2, skuId: 3 };
  assert.deepEqual(normalize(bookingApi.createAddonOrder(addonPayload)), {
    url: '/app-api/booking/addon/create',
    method: 'POST',
    data: addonPayload,
    custom: { auth: true, showLoading: true },
  });
});
