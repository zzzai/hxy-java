# Handoff - Trade Order Item Template Snapshot Chain

- Date: 2026-03-02
- Scope: P0 订单模板快照落单（第一阶段）

## Background

交易订单项缺少模板版本、模板快照、价格来源快照字段，导致审计与结算追溯链路不完整。

## Changes

1. 新增下单请求字段：`templateVersionId/templateSnapshotJson/priceSourceSnapshotJson`。
2. 新增价格计算请求与响应字段并透传。
3. 新增订单项 DO 与前后端 VO 字段，并通过 `TradeOrderConvert` 落库。
4. 增加增量 SQL：`sql/mysql/hxy/2026-03-02-hxy-trade-order-item-template-snapshot.sql`。
5. 增加单测：`TradeOrderTemplateSnapshotConvertTest`。

## Verification

Command:

```bash
mvn -f ruoyi-vue-pro-master/pom.xml \
  -pl yudao-module-mall/yudao-module-trade -am \
  -Dtest=TradeOrderTemplateSnapshotConvertTest,TradeServiceOrderServiceImplTest,TradeServiceOrderBookingRetryJobTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: PASS（17 tests, 0 failure）

## Risks / Follow-up

1. 当前阶段为“请求透传”模型，尚未做“模板发布版强校验”。
2. 后续需建立 `SKU/SPU -> 模板版本` 稳定映射后，再切到服务端强校验。
