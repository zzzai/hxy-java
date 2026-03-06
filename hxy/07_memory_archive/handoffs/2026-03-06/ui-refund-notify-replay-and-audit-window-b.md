# Window B Handoff - 退款回调重放与四账退款审计汇总收口

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 本批目标完成情况
1. 退款回调日志管理页（booking）
- API：
  - `GET /booking/refund-notify-log/page`
  - `POST /booking/refund-notify-log/replay`
- 页面能力：
  - 筛选：`orderId / merchantRefundId / payRefundId / status / errorCode / createTime`
  - 分页
  - 行级“重放”按钮，仅失败状态可点击
  - 二次确认后发起重放
  - 结果提示（成功提示 + 失败错误码透传）
- 详情抽屉：
  - 展示日志元数据与 `rawPayload`
  - `rawPayload` 可解析 JSON 时格式化展示
  - 非 JSON 时降级展示原文并给出提示

2. 四账页退款审计增强接入
- 筛选接入并透传：
  - `refundAuditStatus`
  - `refundExceptionType`
  - `refundLimitSource`
  - `payRefundId`
  - `refundTimeRange`
- 接入汇总接口：
  - `GET /booking/four-account-reconcile/refund-audit-summary`
- 展示增强：
  - 审计状态 Tag
  - 异常类型 Tag
  - 证据链摘要列（由 `refundEvidenceJson` 提取，异常时降级原文摘要）
  - 空值统一 `--`

3. 交互与降级
- `refund-audit-summary` 不可用时，页面不崩溃：
  - 自动降级为列表近似汇总
  - 展示“汇总降级”提示与原因
- 错误码透传展示（重点）：
  - `1030004011`（商户退款单号不合法）
  - `1030004013`（退款回调日志不存在）
  - `1030004014`（日志状态非法，仅失败可重放）

4. 菜单 SQL
- 新增并保持幂等：
  - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-booking-refund-notify-log-menu.sql`
- 菜单名：`退款回调日志管理`
- 按钮权限：
  - `booking:refund-notify-log:query`
  - `booking:refund-notify-log:replay`

## 手工验收步骤与预期
1. 菜单可见性
- 步骤：执行菜单 SQL，刷新后台菜单。
- 预期：`booking` 下出现“退款回调日志管理”。

2. 日志页筛选透传
- 步骤：依次输入 `orderId/merchantRefundId/payRefundId/status/errorCode/createTime`，点击搜索。
- 预期：分页请求携带对应参数，列表刷新正常。

3. 重放按钮状态
- 步骤：观察不同状态行的“重放”按钮。
- 预期：仅 `fail` 行可点击；非失败行置灰并提示“仅失败记录可重放”。

4. 重放二次确认
- 步骤：点击失败行“重放”。
- 预期：先弹确认框；取消不发请求，确认后才请求 `POST /booking/refund-notify-log/replay`。

5. 错误码透传
- 步骤：构造重放失败场景（无效日志ID/非法状态/非法商户退款号等）。
- 预期：前端提示包含后端错误码与可理解文案，重点覆盖 `1030004011/4013/4014`。

6. rawPayload 详情抽屉
- 步骤：打开日志详情。
- 预期：
  - 合法 JSON：格式化展示
  - 非 JSON：提示“非法 JSON，降级展示原文”并显示原始内容

7. 四账退款审计筛选
- 步骤：设置 `refundAuditStatus/refundExceptionType/refundLimitSource/payRefundId/refundTimeRange` 后查询。
- 预期：列表按条件刷新，筛选值与分页联动正常。

8. 四账退款审计汇总
- 步骤：查询后观察“退款佣金审计”汇总区。
- 预期：
  - 汇总来自 `refund-audit-summary`
  - 展示总数、差异金额、未收口工单
  - 展示状态聚合与异常类型聚合 Tag

9. 汇总 fail-open 验证
- 步骤：模拟 `refund-audit-summary` 接口异常（如 404/500）。
- 预期：页面不崩溃，显示“汇总降级”提示并回退为列表近似统计。

10. 证据链摘要
- 步骤：查看列表“证据链摘要”列（含合法 JSON / 非 JSON / 空值）。
- 预期：
  - JSON：展示提取摘要
  - 非 JSON：展示原文截断摘要
  - 空值：展示 `--`
