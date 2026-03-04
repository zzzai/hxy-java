# Window B Handoff - SLA Config Center (2026-03-04)

## 输入背景
- 目标分支：`feat/p1-sla-config-center`
- 任务范围：booking/trade 域完成 SLA 工单规则配置中心化（表驱动）
- 强约束：不修改 `product/store-sku` 相关文件
- 交付要求：后端、SQL、测试、治理文档、门禁命令一次性收口

## 变更清单
1. 新增统一规则表能力：`trade_ticket_sla_rule`
- 管理字段：`ticket_type / rule_code / severity / scope_type / scope_store_id / enabled / priority`
- 输出字段：`escalate_to / sla_minutes / warn_lead_minutes / escalate_delay_minutes`
- 审计字段：`creator / updater / last_action / last_action_at`
- 规则冲突拦截：同 scope 唯一

2. 新增 trade-api 合同
- `TradeTicketSlaRuleApi`
- `TradeTicketSlaRuleMatchReqDTO / RespDTO`
- 枚举：scope/match-level/ticket-type

3. 新增 trade 管理端 API
- 分页：`GET /trade/ticket-sla-rule/page`
- 详情：`GET /trade/ticket-sla-rule/get`
- 创建：`POST /trade/ticket-sla-rule/create`
- 更新：`PUT /trade/ticket-sla-rule/update`
- 启停：`PUT /trade/ticket-sla-rule/update-status`
- 预览：`POST /trade/ticket-sla-rule/preview-match`

4. 运行时接入
- trade：`AfterSaleReviewTicketServiceImpl` 改为“规则中心优先 + fallback 路由”
- booking：`TechnicianCommissionSettlementServiceImpl.submitForReview` 使用规则中心 SLA 默认值，常量兜底

5. SQL
- 新增：`ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-04-hxy-sla-config-center.sql`
- 特性：建表/索引/默认规则初始化均可重复执行

6. 测试
- 新增 `TicketSlaRuleServiceImplTest`：优先级、冲突校验、启停生效
- 新增 `TicketSlaRuleControllerTest`：增改查分页+预览
- 扩展 `AfterSaleReviewTicketServiceImplTest`：规则中心命中
- 扩展 `TechnicianCommissionSettlementServiceImplTest`：提审 SLA 命中

## 风险与回滚
- 风险1：规则配置不完整导致命中为空
  - 保护：trade/booking 均保留默认兜底常量或路由映射
- 风险2：作用域配置错误导致误命中
  - 保护：scope 唯一冲突拦截 + 预览 API 校验
- 回滚策略：
  1. 回滚 SQL（删除 `trade_ticket_sla_rule` 相关变更）
  2. 回滚 `AfterSaleReviewTicketServiceImpl` 与 `TechnicianCommissionSettlementServiceImpl` 到硬编码版本
  3. 保留治理文档但标注回滚记录

## 下一窗口接力点
1. 补充更多 ticket_type 规则初始化（服务履约、提成争议）并接入真实业务触发。
2. 增加 store 维度的 booking 通知任务窗口策略（当前 warn/escalate 仍以全局默认优先）。
3. 在管理端前端页面补齐规则配置页和命中预览交互。
4. 观察线上命中率与误配率，决定是否引入 DB 级更严格唯一约束策略。
