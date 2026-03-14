# HXY Admin Store Lifecycle Governance PRD v1（2026-03-15）

## 1. 文档目标与边界
- 目标：把当前后台 `ADM-011` 门店生命周期批次执行台账 / 批次复核执行、`ADM-012` 门店生命周期变更单、`ADM-013` 门店生命周期复核台账，收口为一份正式 PRD。
- 本文覆盖：
  - 生命周期变更单的申请、提交、审批、驳回、取消
  - 生命周期守卫批次执行日志查看、按批次复核执行
  - 生命周期复核台账查看与守卫明细审查
  - 生命周期守卫 `WARN / BLOCK` 语义和上线检查边界
- 本文不覆盖：
  - 门店主数据、分类、标签等主数据页面的基础录入规则
  - 商品映射、库存调整、跨店调拨等供应链页面细节
  - 小程序门店展示页或加盟侧流程

## 2. 单一真值来源
### 2.1 页面真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleBatchLog/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleChangeOrder/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleRecheckLog/index.vue`

### 2.2 API / Controller 真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/lifecycleBatchLog.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/lifecycleChangeOrder.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/lifecycleRecheckLog.ts`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/store/ProductStoreController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/store/ProductStoreLifecycleBatchLogController.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/store/ProductStoreLifecycleRecheckLogController.java`

### 2.3 上游产品 / 架构参考
- `docs/plans/2026-03-03-ticket-sla-store-lifecycle-guard-design.md`
- `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`
- `hxy/01_product/HXY-门店管理产品设计-万店版-v1-2026-02-28.md`

## 3. 当前业务结论
1. 当前后台已经存在真实生命周期治理页面，不是规划态或文档态。
2. 门店生命周期状态真值固定为：
   - `10 PREPARING`
   - `20 TRIAL`
   - `30 OPERATING`
   - `35 SUSPENDED`
   - `40 CLOSED`
3. 生命周期变更单采用独立审批状态机：
   - `0 草稿`
   - `10 待审批`
   - `20 已通过`
   - `30 已驳回`
   - `40 已取消`
4. 生命周期治理不是“直接改状态”，而是“先守卫评估，再通过变更单和批次台账完成状态流转”。
5. 当前生命周期守卫的核心治理项，按设计真值至少包含五项：
   - 商品映射 `mapping`
   - 库存 `stock`
   - 库存流水 `stock-flow`
   - 未结订单 `pending-order`
   - 在途售后工单 `inflight-ticket`
6. 每个守卫项都允许按配置在 `WARN` 与 `BLOCK` 之间切换。产品口径必须区分：
   - `WARN`：允许流转，但追加审计告警
   - `BLOCK`：直接阻断生命周期变更
7. 当前三类页面分工固定：
   - 变更单页：发起和审批生命周期动作
   - 批次台账页：看批量执行结果，并触发按批次复核
   - 复核台账页：看复核结果，不负责直接改状态

## 4. 角色与使用场景
| 角色 | 目标 | 使用页面 |
|---|---|---|
| 总部门店运营 | 为门店申请生命周期切换，如试营业、正式营业、暂停营业、闭店 | `mall/store/lifecycleChangeOrder/index` |
| 审批人 / 区域负责人 | 审批生命周期变更单，判断是否允许状态流转 | `mall/store/lifecycleChangeOrder/index` |
| 运营治理 / 质量保障 | 查看生命周期批量执行结果、识别守卫阻断和告警 | `mall/store/lifecycleBatchLog/index` |
| 审核 / 稽核人员 | 基于历史批次再执行复核，核实守卫命中详情 | `mall/store/lifecycleBatchLog/index`; `mall/store/lifecycleRecheckLog/index` |

## 5. 页面与能力边界
### 5.1 生命周期变更单页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleChangeOrder/index.vue`

查询能力：
1. 变更单号 `orderNo`
2. 门店 `storeId`
3. 审批状态 `status`
4. 原生命周期 `fromLifecycleStatus`
5. 目标生命周期 `toLifecycleStatus`
6. 申请人 `applyOperator`
7. 是否超时 `overdue`
8. 最近动作编码 / 最近动作人
9. 申请时间范围 `createTime[]`

写能力：
1. 创建：`POST /product/store/lifecycle-change-order/create`
2. 提交：`POST /product/store/lifecycle-change-order/submit`
3. 审批通过：`POST /product/store/lifecycle-change-order/approve`
4. 驳回：`POST /product/store/lifecycle-change-order/reject`
5. 取消：`POST /product/store/lifecycle-change-order/cancel`
6. 详情：`GET /product/store/lifecycle-change-order/get`
7. 分页：`GET /product/store/lifecycle-change-order/page`

固定边界：
1. 当前页面以单门店变更单为主，不替代批量生命周期批次执行台账。
2. 页面会展示 `guardSnapshotJson`、`guardBlocked`、`guardWarnings`，说明生命周期守卫是变更单审批前后的核心判断依据。
3. `check-launch-readiness` 属于辅助只读能力，用于判断门店是否具备投放/上线条件，不是直接写操作。

### 5.2 生命周期批次执行台账页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleBatchLog/index.vue`

能力：
1. 分页查看批次号、目标生命周期、总数、成功数、阻塞数、告警数、规则版本、来源、操作人、创建时间
2. 查看批次详情 `GET /product/store/lifecycle-batch-log/get`
3. 通过 `POST /product/store/lifecycle-guard/recheck-by-batch/execute` 发起按批次复核
4. 展示 `guardConfigSnapshotJson` 与 `detailJson` 解析结果

固定边界：
1. 批次台账页用于审计和复核触发，不直接创建生命周期变更单。
2. 若 `detailJson` 或配置快照解析失败，页面保留原文并明确 warning，不能假装解析成功。

### 5.3 生命周期复核台账页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleRecheckLog/index.vue`

能力：
1. 分页查看复核编号、批次台账 ID、批次号、目标生命周期、阻塞数、告警数、规则版本、操作人、来源、创建时间
2. 查看复核详情 `GET /product/store/lifecycle-recheck-log/get`
3. 展示复核后的明细行，包括阻塞原因、告警、guardItems

固定边界：
1. 复核台账页不负责审批生命周期变更单。
2. 复核页的职责是“重新判定并留痕”，不是替代守卫配置中心或手工放行工具。

## 6. 关键业务对象与字段最小集
### 6.1 生命周期变更单
- `id`
- `orderNo`
- `storeId` `storeName`
- `fromLifecycleStatus` `toLifecycleStatus`
- `status`
- `reason`
- `guardSnapshotJson`
- `guardBlocked`
- `guardWarnings`
- `applyOperator` `applySource`
- `submitTime`
- `slaDeadlineTime`
- `overdue`
- `lastActionCode` `lastActionOperator` `lastActionTime`
- `approveOperator` `approveTime` `approveRemark`
- `creator` `createTime`

### 6.2 生命周期批次执行台账
- `id`
- `batchNo`
- `targetLifecycleStatus`
- `totalCount`
- `successCount`
- `blockedCount`
- `warningCount`
- `auditSummary`
- `guardRuleVersion`
- `guardConfigSnapshotJson`
- `detailJson`
- `operator` `source` `createTime`

### 6.3 生命周期复核台账
- `id`
- `recheckNo`
- `logId`
- `batchNo`
- `targetLifecycleStatus`
- `totalCount`
- `blockedCount`
- `warningCount`
- `guardRuleVersion`
- `guardConfigSnapshotJson`
- `detailJson`
- `detailParseError`
- `operator` `source` `createTime`

## 7. 关键流程
### 7.1 单门店生命周期变更流程
1. 运营选择门店和目标生命周期。
2. 填写变更原因，创建草稿单。
3. 系统生成守卫快照，记录 `guardBlocked / guardWarnings`。
4. 草稿确认后进入待审批。
5. 审批人根据守卫命中和业务背景决定通过、驳回或取消。
6. 审批通过后，门店生命周期完成变更并留下审计轨迹。

### 7.2 批量执行台账审查与按批次复核
1. 运营在批次台账页查看目标生命周期和执行结果。
2. 若批次内存在阻塞或告警，进入详情查看每家门店的 `detailJson`。
3. 需要复核时，按 `logId` 或 `batchNo` 触发 `recheck-by-batch/execute`。
4. 系统产出新的复核编号和复核明细，供复核台账页继续审查。

### 7.3 上线准备度检查
1. 生命周期变更单和批量执行前，可以先走 `GET /product/store/check-launch-readiness` 或守卫查询 API。
2. 当前只读结果用于辅助判断“是否可流转”，不能替代正式审批或复核留痕。

## 8. 状态、空态、错误与降级
### 8.1 守卫语义
- `WARN`：允许流转，但要追加审计原因，格式固定为 `LIFECYCLE_GUARD_WARN:<guardKey>:count=<n>`。
- `BLOCK`：直接阻断变更或批量执行。
- 产品文档必须继续维持五个守卫项的独立判断，不允许把所有阻断原因模糊成一个“检查未通过”。

### 8.2 查询与空态
- 批次台账、复核台账、变更单列表为空都是合法空态。
- 守卫明细为空时，页面应明确显示“无可用明细”，不能补造默认通过样本。

### 8.3 失败与降级
- 当前未核到服务端 `degraded=true / degradeReason` 返回。
- 生命周期守卫允许的“降级”只有配置层把某个守卫项从 `BLOCK` 下调为 `WARN`，这属于治理动作，不是接口返回层降级字段。
- 若 `detailJson` / `guardConfigSnapshotJson` 解析失败，只能展示 warning 和原文，不能影响原始执行记录。

## 9. 禁止性边界
1. 不得把生命周期变更单写成“直接修改门店状态”的无审计动作。
2. 不得把批次台账和复核台账混写成同一页面职责。
3. 不得因为守卫配置可切 `WARN`，就把所有命中都当成可放行；是否放行仍取决于配置与审批链。
4. 不得把门店主数据、库存调整、跨店调拨规则直接塞进生命周期 PRD 主体；这里只引用与守卫相关的最小边界。
5. 不得把历史设计文档继续当成正式产品文档替代品；本文落盘后，以本文为准。

## 10. 验收标准
### 10.1 ADM-011 门店生命周期批次执行台账 / 批次复核执行
- [ ] 批次台账、配置快照、明细解析和复核执行边界明确
- [ ] 阻塞 / 告警 / 成功统计口径明确
- [ ] `WARN / BLOCK` 语义明确

### 10.2 ADM-012 门店生命周期变更单
- [ ] 变更单状态机明确
- [ ] 守卫快照与审批动作关系明确
- [ ] 上线准备度检查是辅助判断，不替代审批链

### 10.3 ADM-013 门店生命周期复核台账
- [ ] 复核编号、批次号、阻塞 / 告警统计字段明确
- [ ] 明细查看和原文保留边界明确
- [ ] 复核页不被误写成写操作页面

## 11. 最终结论
1. `ADM-011` 的产品真值是“生命周期批次执行结果与复核执行治理台”。
2. `ADM-012` 的产品真值是“门店生命周期状态变更审批单”。
3. `ADM-013` 的产品真值是“生命周期复核留痕台账”，不是直接改状态工具。
4. 本文落盘后，`ADM-011`、`ADM-012`、`ADM-013` 在全项目业务台账中的 PRD 完整度应提升为 `完整`。
