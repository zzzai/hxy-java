# MiniApp Content Compliance Styleguide v1 (2026-03-09)

## 1. 目标
- 统一小程序文案与素材的合规标准，确保内容“可审计、可拦截、可复核”。
- 防止误导性营销与假成功反馈侵入交易、履约、售后关键链路。

## 2. 事件字典（内容治理）

| 事件名 | 说明 | 必填 | 枚举/取值 | 示例 |
|---|---|---|---|---|
| `content_publish_submit` | 内容提交审核 | 是 | `contentType=copy/banner/popup/video` | `contentType=banner` |
| `content_compliance_check` | 规则校验执行 | 是 | `checkResult=pass/warn/reject` | `checkResult=warn` |
| `content_compliance_intercept` | 内容拦截命中 | 否 | `riskLevel=P0/P1/P2` | `riskLevel=P0` |
| `content_revise_resubmit` | 修改后重提 | 否 | `reviseReason=misleading/fake_success/compliance` | `reviseReason=misleading` |
| `content_online` | 审核通过上线 | 否 | `publishChannel=miniapp/homepage/activity` | `publishChannel=activity` |
| `content_fake_success_block` | 假成功表达拦截 | 否 | `scene=pay/coupon/refund/fulfillment` | `scene=refund` |

## 3. 字段字典（必填、枚举、示例）

| 字段 | 必填 | 类型 | 枚举/规则 | 示例 |
|---|---|---|---|---|
| `contentId` | 是 | string | 内容主键 | `CNT20260309001` |
| `contentVersion` | 是 | string | 版本号 | `v12` |
| `contentType` | 是 | string | `copy/banner/popup/video` | `copy` |
| `scene` | 是 | string | `pay/refund/after_sale/coupon/points/fulfillment` | `pay` |
| `route` | 是 | string | 页面路由 | `/pages/pay/result` |
| `copyText` | 否 | string | 脱敏后文本摘要 | `支付处理中，请稍候` |
| `riskLevel` | 否 | string | `P0/P1/P2` | `P1` |
| `riskTag` | 否 | string | `COMPLIANCE/MISLEADING_MARKETING/FAKE_SUCCESS` | `FAKE_SUCCESS` |
| `interceptAction` | 否 | string | `BLOCK/WARN/PASS` | `BLOCK` |
| `resultCode` | 否 | string | 业务结果码 | `WAITING` |
| `errorCode` | 否 | string | 错误码 | `1030004012` |
| `degraded` | 否 | bool | 降级标记 | `false` |
| `reviewerId` | 否 | string | 审核人 ID | `ops_112` |
| `reviewDecisionTime` | 否 | string | 审核时间 | `2026-03-09T16:10:00+08:00` |

## 4. 口径定义与归因规则
### 4.1 口径定义
- `PASS`：无风险命中，允许上线。
- `WARN`：可上线但需标注风险并纳入复核清单。
- `BLOCK`：禁止上线，必须整改并重审。

### 4.2 归因规则
- 内容问题归因键：`contentId + contentVersion + scene`。
- 误导营销归因：以最终上线文本为准，不以草稿为准。
- 假成功归因：以“前端表达时点早于后端确认时点”判定。
- 复发率归因窗口：30 天内同 `riskTag` 复发计入复发率。

## 5. 风险分级与拦截规则

| 风险类型 | 分级 | 判定条件 | 拦截规则 |
|---|---|---|---|
| 合规风险 | P0 | 命中监管禁语、夸大承诺、敏感违规词 | `BLOCK`，禁止上线，升级法务复核 |
| 误导性营销 | P1 | “秒到账/必中/零风险”等不可证实承诺 | `BLOCK` 或 `WARN`（按场景），强制改稿 |
| 假成功动效 | P0 | 未获后端 SUCCESS 却展示“成功/到账/完成” | `BLOCK`，并记录 `content_fake_success_block` |
| 术语漂移 | P2 | 与统一术语不一致但不影响交易真值 | `WARN`，纳入周度清理 |

## 6. 与既有文档映射
- `analytics-funnel`：新增内容审核与拦截事件，补足“曝光到转化”前置质量层。
- `copy-terminology`：本指南是其合规执行层；术语规范为基础，本指南定义拦截边界。
- `motion-accessibility`：禁止仅通过动画表达成功；所有成功态必须有文本与状态码可见。

## 7. 验收标准
1. 所有上线内容均有 `contentId/contentVersion/reviewDecision` 审计记录。
2. 交易关键页（支付、退款、履约）0 条假成功文案/动效上线。
3. `P0` 风险拦截漏拦率 = 0。
4. 运营看板可按 `riskTag/riskLevel/scene/route` 过滤与追溯。

## 8. 运营监控指标
- `内容审核通过率`：`PASS / 提审总量`。
- `拦截命中率`：`BLOCK / 提审总量`。
- `误导营销复发率`：30 天内 `MISLEADING_MARKETING` 重复命中占比。
- `假成功拦截趋势`：`content_fake_success_block` 日/周趋势。
- `整改时长中位数`：从 `BLOCK` 到 `content_online` 的中位耗时。
- `高风险场景覆盖率`：支付/退款/履约场景内容的审核覆盖率。
