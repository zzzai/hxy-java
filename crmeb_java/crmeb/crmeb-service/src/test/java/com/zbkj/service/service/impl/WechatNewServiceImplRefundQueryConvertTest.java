package com.zbkj.service.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.vo.MyRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WechatNewServiceImplRefundQueryConvertTest {

    @Test
    void convertV3RefundQueryResponseShouldSupportDefaultObjectShape() {
        WechatNewServiceImpl service = new WechatNewServiceImpl();
        JSONObject payload = new JSONObject();
        payload.put("out_refund_no", "order_refund_001");
        payload.put("out_trade_no", "wx_refund_001");
        payload.put("transaction_id", "420000000000000001");
        payload.put("refund_id", "500001");
        payload.put("status", "SUCCESS");
        payload.put("success_time", "2026-02-18T13:23:17+08:00");

        JSONObject amount = new JSONObject();
        amount.put("refund", 1);
        amount.put("total", 1);
        JSONObject from = new JSONObject();
        from.put("payer_refund", 1);
        amount.put("from", from);
        payload.put("amount", amount);

        MyRecord record = ReflectionTestUtils.invokeMethod(service, "convertV3RefundQueryResponseToRecord", payload);

        Assertions.assertEquals("order_refund_001", record.getStr("out_refund_no"));
        Assertions.assertEquals("SUCCESS", record.getStr("refund_status_0"));
        Assertions.assertEquals(1, record.getInt("refund_fee_0"));
        Assertions.assertEquals(1, record.getInt("cash_refund_fee_0"));
    }

    @Test
    void convertV3RefundQueryResponseShouldSupportArrayShape() {
        WechatNewServiceImpl service = new WechatNewServiceImpl();
        JSONObject payload = new JSONObject();

        JSONObject refundNode = new JSONObject();
        refundNode.put("out_refund_no", "order_refund_002");
        refundNode.put("out_trade_no", "wx_refund_002");
        refundNode.put("transaction_id", "420000000000000002");
        refundNode.put("refund_id", "500002");
        refundNode.put("refund_status", "SUCCESS");
        refundNode.put("success_time", "2026-02-18T13:23:18+08:00");

        JSONObject amount = new JSONObject();
        amount.put("refund", 1);
        amount.put("total", 1);
        amount.put("payer_refund", 1);
        refundNode.put("amount", amount);

        JSONArray refunds = new JSONArray();
        refunds.add(refundNode);
        payload.put("refunds", refunds);
        payload.put("amount", refunds);

        MyRecord record = ReflectionTestUtils.invokeMethod(service, "convertV3RefundQueryResponseToRecord", payload);

        Assertions.assertEquals("order_refund_002", record.getStr("out_refund_no"));
        Assertions.assertEquals("wx_refund_002", record.getStr("out_trade_no"));
        Assertions.assertEquals("SUCCESS", record.getStr("refund_status_0"));
        Assertions.assertEquals(1, record.getInt("refund_fee_0"));
        Assertions.assertEquals(1, record.getInt("cash_refund_fee_0"));
    }
}
