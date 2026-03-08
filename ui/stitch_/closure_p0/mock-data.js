(function () {
  const now = new Date().toISOString();

  const orderItems = [
    {
      id: 91001,
      spuName: '艾草肩颈热敷贴',
      picUrl: 'https://dummyimage.com/120x120/f3f4f6/111827&text=A1',
      price: 6900,
      payPrice: 6900,
      count: 1,
      properties: [{ valueName: '10片装' }],
    },
    {
      id: 91002,
      spuName: '经络理疗按摩仪',
      picUrl: 'https://dummyimage.com/120x120/e5e7eb/111827&text=A2',
      price: 15900,
      payPrice: 15900,
      count: 1,
      properties: [{ valueName: '标准版' }],
      afterSaleId: 7002,
      afterSaleStatus: 30,
    },
  ];

  const orders = [
    {
      id: 5001,
      no: 'T202603080001',
      status: 0,
      payOrderId: 8801,
      payPrice: 22800,
      productCount: 2,
      createTime: '2026-03-08 09:12:00',
      items: orderItems,
      buttons: ['pay', 'cancel'],
    },
    {
      id: 5002,
      no: 'T202603070109',
      status: 20,
      payOrderId: 8802,
      payPrice: 9900,
      productCount: 1,
      createTime: '2026-03-07 12:30:00',
      items: [
        {
          id: 91003,
          spuName: '暖宫草本贴',
          picUrl: 'https://dummyimage.com/120x120/fde68a/111827&text=A3',
          price: 9900,
          payPrice: 9900,
          count: 1,
          properties: [{ valueName: '5片装' }],
        },
      ],
      buttons: ['confirm', 'express'],
    },
    {
      id: 5003,
      no: 'T202603060054',
      status: 30,
      payOrderId: 8803,
      payPrice: 39900,
      productCount: 1,
      createTime: '2026-03-06 17:25:00',
      items: [
        {
          id: 91004,
          spuName: '肩颈理疗套装',
          picUrl: 'https://dummyimage.com/120x120/dbeafe/111827&text=A4',
          price: 39900,
          payPrice: 39900,
          count: 1,
          properties: [{ valueName: '家庭装' }],
        },
      ],
      buttons: ['comment', 'delete'],
    },
  ];

  const afterSales = [
    {
      id: 7001,
      no: 'AS202603080001',
      status: 10,
      way: 10,
      refundPrice: 6900,
      picUrl: 'https://dummyimage.com/120x120/f3f4f6/111827&text=A1',
      spuName: '艾草肩颈热敷贴',
      count: 1,
      properties: [{ valueName: '10片装' }],
      applyReason: '效果不明显',
      applyDescription: '使用后无明显改善，申请退款。',
      createTime: '2026-03-08 10:11:00',
      updateTime: '2026-03-08 10:11:00',
      buttons: ['cancel'],
    },
    {
      id: 7002,
      no: 'AS202603070008',
      status: 30,
      way: 20,
      refundPrice: 15900,
      picUrl: 'https://dummyimage.com/120x120/e5e7eb/111827&text=A2',
      spuName: '经络理疗按摩仪',
      count: 1,
      properties: [{ valueName: '标准版' }],
      applyReason: '商品有划痕',
      applyDescription: '开箱发现外壳有划痕，已上传图片。',
      createTime: '2026-03-07 15:00:00',
      updateTime: '2026-03-08 09:05:00',
      buttons: ['delivery'],
    },
    {
      id: 7003,
      no: 'AS202603050015',
      status: 50,
      way: 10,
      refundPrice: 9900,
      picUrl: 'https://dummyimage.com/120x120/fde68a/111827&text=A3',
      spuName: '暖宫草本贴',
      count: 1,
      properties: [{ valueName: '5片装' }],
      applyReason: '买错规格',
      applyDescription: '已重新下单，申请原单退款。',
      createTime: '2026-03-05 18:01:00',
      updateTime: '2026-03-06 11:20:00',
      buttons: [],
    },
  ];

  const payOrders = [
    {
      id: 8801,
      no: 'P202603080001',
      status: 0,
      price: 22800,
      merchantOrderId: 5001,
      payChannelCode: 'wx_lite',
      createTime: now,
    },
    {
      id: 8802,
      no: 'P202603070109',
      status: 10,
      price: 9900,
      merchantOrderId: 5002,
      payChannelCode: 'wx_lite',
      createTime: now,
    },
    {
      id: 8803,
      no: 'P202603060054',
      status: 30,
      price: 39900,
      merchantOrderId: 5003,
      payChannelCode: 'wx_lite',
      createTime: now,
    },
  ];

  function clone(data) {
    return JSON.parse(JSON.stringify(data));
  }

  function parseInteger(value) {
    const n = Number(value);
    return Number.isFinite(n) ? n : null;
  }

  function byStatus(order, status, commentStatus) {
    if (status === null || status === undefined || status === '') {
      return true;
    }
    const current = parseInteger(status);
    if (current === null) {
      return true;
    }
    if (current === 30 && commentStatus === false) {
      return order.status === 30;
    }
    return order.status === current;
  }

  function matchStatuses(record, statuses) {
    if (!statuses) {
      return true;
    }
    const values = String(statuses)
      .split(',')
      .map((item) => parseInteger(item))
      .filter((item) => item !== null);
    if (values.length === 0) {
      return true;
    }
    return values.includes(record.status);
  }

  const Mock = {
    getOrderPage(input) {
      const params = input && input.params ? input.params : {};
      const pageNo = parseInteger(params.pageNo) || 1;
      const pageSize = parseInteger(params.pageSize) || 10;
      const filtered = orders.filter((item) => byStatus(item, params.status, params.commentStatus));
      const start = (pageNo - 1) * pageSize;
      const end = start + pageSize;
      return {
        list: clone(filtered.slice(start, end)),
        total: filtered.length,
        pageNo,
        pageSize,
      };
    },

    getOrderDetail(input) {
      const params = input && input.params ? input.params : {};
      const id = parseInteger(params.id);
      const hit = orders.find((item) => item.id === id) || orders[0];
      return clone(hit);
    },

    createAfterSale(input) {
      const data = input && input.data ? input.data : {};
      const nowStamp = Date.now();
      const newId = 7100 + (nowStamp % 1000);
      const sourceOrder = orders.find((order) =>
        order.items.some((it) => Number(it.id) === Number(data.orderItemId)),
      );
      const sourceItem = sourceOrder
        ? sourceOrder.items.find((it) => Number(it.id) === Number(data.orderItemId))
        : orders[0].items[0];

      afterSales.unshift({
        id: newId,
        no: `AS${newId}`,
        status: 10,
        way: Number(data.way) || 10,
        refundPrice: Number(data.refundPrice) || sourceItem.payPrice,
        picUrl: sourceItem.picUrl,
        spuName: sourceItem.spuName,
        count: sourceItem.count,
        properties: clone(sourceItem.properties || []),
        applyReason: data.applyReason || '其他',
        applyDescription: data.applyDescription || '',
        createTime: now,
        updateTime: now,
        buttons: ['cancel'],
      });

      return {
        id: newId,
      };
    },

    getAfterSalePage(input) {
      const params = input && input.params ? input.params : {};
      const pageNo = parseInteger(params.pageNo) || 1;
      const pageSize = parseInteger(params.pageSize) || 10;
      const filtered = afterSales.filter((item) => matchStatuses(item, params.statuses));
      const start = (pageNo - 1) * pageSize;
      const end = start + pageSize;
      return {
        list: clone(filtered.slice(start, end)),
        total: filtered.length,
        pageNo,
        pageSize,
      };
    },

    getAfterSale(input) {
      const params = input && input.params ? input.params : {};
      const id = parseInteger(params.id);
      const hit = afterSales.find((item) => item.id === id) || afterSales[0];
      return clone(hit);
    },

    getPayOrder(input) {
      const params = input && input.params ? input.params : {};
      const id = parseInteger(params.id);
      const no = params.no;
      const hit = payOrders.find((item) => (id ? item.id === id : item.no === no)) || payOrders[0];
      return clone(hit);
    },
  };

  window.ClosureP0Mock = Mock;
})();
