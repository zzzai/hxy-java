# Window A Handoff - 2026-03-06 - P1 Stock Transfer + Four Account Summary

## 1) 输入背景
- 分支固定：`feat/ui-four-account-reconcile-ops`（窗口A集成分支，不切分支）。
- 本批目标：
  1. `product` 跨店调拨审批闭环。
  2. `product` 盘点差异稽核工单（阈值配置 + fail-open）。
  3. `booking` 四账对账汇总接口（可降级）。

## 2) 变更清单
- `product`
  - 新增跨店调拨模型：`hxy_store_sku_transfer_order`（DO/Mapper/VO/Controller/Service）。
  - 状态机：`DRAFT(0)->PENDING(10)->APPROVED(20)/REJECTED(30)/CANCELLED(40)`。
  - 审批通过时执行双向库存流水：
    - 源门店：`MANUAL_TRANSFER_OUT`
    - 目标门店：`MANUAL_TRANSFER_IN`
    - 幂等键统一 `bizNo=orderNo`。
  - 盘点审批联动稽核工单：
    - 配置键：`product.stocktake.audit-ticket.enabled`、`product.stocktake.audit-threshold`
    - 兼容旧键：`hxy.product.stocktake.audit-ticket.enabled`、`hxy.product.stocktake.audit-threshold`
    - 超阈值触发 `TradeReviewTicketApi.upsertReviewTicket`
    - 调用失败 fail-open，仅日志告警。
  - 新增 SQL：`2026-03-06-hxy-store-sku-transfer-order-and-stocktake-audit-config.sql`
    - 建表 `hxy_store_sku_transfer_order`
    - 补索引
    - 注入盘点稽核开关/阈值配置种子。
- `booking`
  - 新增 `GET /booking/four-account-reconcile/summary`
  - 查询维度：`bizDate[] + status + relatedTicketLinked`
  - 返回：`total/pass/warn/tradeMinusFulfillmentSum/tradeMinusCommissionSplitSum/unresolvedTicketCount/ticketSummaryDegraded`
  - 工单摘要联查异常时降级：不抛错，主链路可用，返回 `ticketSummaryDegraded=true`。

## 3) 风险与回滚
- 风险1：摘要联查降级时，`relatedTicketLinked` 过滤会退化为不过滤。
  - 处理：通过 `ticketSummaryDegraded` 明确标识，前端可提示“当前为降级统计”。
- 风险2：盘点稽核开关误配置导致工单量突增。
  - 处理：可通过 `infra_config` 关闭 `product.stocktake.audit-ticket.enabled`；库存审批主链路不受影响。
- 回滚策略：
  - 快速回滚：回退本批 commit。
  - 配置回滚：将稽核开关置 `false`。

## 4) 下一窗口接力点
1. 将 `summary` 接到 overlay 四账页面“顶部汇总卡片 + 降级提示”。
2. 调拨单与库存调整单在管理端统一展示 `lastAction*` 审计字段。
3. 追加 SQL 演练脚本（本地一键 MySQL 引导 + 调拨审批 + 稽核联动冒烟）。
