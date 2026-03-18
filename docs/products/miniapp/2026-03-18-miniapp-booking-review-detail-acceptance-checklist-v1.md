# MiniApp Booking Review Detail Acceptance Checklist v1（2026-03-18）

## 1. 目标
- 固定 `/pages/booking/review-detail` 当前真实验收口径。
- 只吸收当前已提交代码、测试与既有 booking review 文档真值，不扩写奖励、补偿、自动通知等未冻结能力。

## 2. 证据范围
- 页面：
  - `yudao-mall-uniapp/pages/booking/review-list.vue`
  - `yudao-mall-uniapp/pages/booking/review-detail.vue`
- 路由：
  - `yudao-mall-uniapp/pages.json`
- 前端 API：
  - `yudao-mall-uniapp/sheep/api/trade/review.js`
- 测试：
  - `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
  - `yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs`
  - `yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs`
- 已有单一真值：
  - `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
  - `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
  - `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
  - `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
  - `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`

## 3. 当前结论

| 判断项 | 当前结论 | 说明 |
|---|---|---|
| 文档状态 | `Doc Closed` | 页面、字段、contract、failure mode、acceptance 已落盘 |
| 工程状态 | `Query-side Implemented` | `review-list -> review-detail -> getReview` 已真实闭环 |
| 当前是否可开发 | `Yes` | 可继续补充样本与运营动作，但不能越界写新能力 |
| 当前是否可放量 | `No` | 不能把详情页补齐外推成 booking review 已 release-ready |
| Release Decision | `No-Go` | 缺 feature flag、rollout、runtime sample pack、自动升级链路证据 |

## 4. 页面范围与入口真值
1. 当前唯一用户入口是 `/pages/booking/review-list` 卡片点击进入 `/pages/booking/review-detail?id=<reviewId>`。
2. 详情页当前只认 route query `id`，不认 `bookingOrderId`、`serviceOrderId` 或其他内部字段。
3. 详情页当前只消费 `GET /booking/review/get`，不消费后台恢复接口，不改写 submit / recovery 结论。

## 5. 字段验收清单

### 5.1 必须展示
- `reviewLevel`
- `submitTime`
- `bookingOrderId`
- `overallScore`
- `serviceScore`
- `technicianScore`
- `environmentScore`
- `tags`
- `content`
- `picUrls`
- `replyContent`

### 5.2 展示规则
- 标题区必须包含评价等级、`提交时间：` 文案、订单号。
- 评分区必须展示总分与三项分项评分。
- `tags[]` 有值展示，无值隐藏。
- `content` 为空时显示 `用户未填写文字评价`。
- `picUrls[]` 有值展示，并支持 `uni.previewImage` 预览。
- `replyContent` 为空时显示 `商家暂未回复`。

### 5.3 禁止展示
- `serviceOrderId`
- `riskLevel`
- `followStatus`
- `auditStatus`
- 后台恢复说明、内部工单字段、人工判责字段

## 6. 状态验收清单

| 场景 | 触发条件 | 页面表现 | 验收结论 |
|---|---|---|---|
| 加载中 | 首次进入详情，已拿到合法 `id` | 文案显示 `正在加载评价详情...` | 通过 |
| 明细成功 | `GET /booking/review/get` 返回 `code=0` 且 `data` 存在 | 展示评分、正文、图片、回复、返回动作 | 通过 |
| 参数非法 | route 缺少 `id` 或 `id=0` | 显示 `评价不存在或参数异常` | 通过 |
| 查无记录 | 返回 `1030008000` 或 `code=0` 但 `data` 为空 | 显示 `评价不存在或参数异常` | 通过 |
| 接口失败 | 非 `1030008000` 且无有效 `data` | 显示 `评价加载失败，请稍后重试` | 通过 |

## 7. 动作验收清单

| 动作 | 当前真实行为 | 验收结论 |
|---|---|---|
| 返回我的评价 | 点击 `返回我的评价` 跳 `/pages/booking/review-list` | 通过 |
| 查看订单详情 | 有 `bookingOrderId` 时跳 `/pages/booking/order-detail?id=<bookingOrderId>` | 通过 |
| 查看订单详情回退 | 缺 `bookingOrderId` 时回退到 `/pages/booking/review-list` | 通过 |
| 图片预览 | 点击图片调用 `uni.previewImage` | 通过 |

## 8. 合法空态与失败语义
1. `review-list` 的 `[]` 与 `summary` 的 `0` 只算合法空态，不算成功样本，不算发布样本。
2. `review-detail` 的 `1030008000` 与 `code=0 + data=null` 统一按“不可查看”处理，不得伪造为独立业务成功分支。
3. 当前没有 booking review 服务端 `degraded=true / degradeReason` 证据；任何“降级展示”都不能写进本页验收结论。

## 9. No-Go 条件
1. 把 `review-detail` 补齐写成 booking review 全链路已放量。
2. 把后台恢复字段下放到会员端页面。
3. 把 `[] / 0 / null` 写成发布级成功样本。
4. 把 `node --test` 通过写成 runtime sample pack。
5. 把“差评通知店长 / 自动补偿 / 好评奖励”写成当前已上线动作。

## 10. 建议验收命令
```bash
node --test \
  yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs \
  yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs \
  yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs
```

## 11. 单一真值引用
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- `docs/products/miniapp/2026-03-18-miniapp-booking-review-detail-acceptance-checklist-v1.md`
