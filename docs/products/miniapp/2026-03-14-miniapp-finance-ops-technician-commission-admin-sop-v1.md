# MiniApp 财务运营技师提成明细 / 计提管理 SOP v1（2026-03-14）

## 1. 目标与适用范围
- 目标：为 `BO-004 技师提成明细 / 计提管理` 补齐后台运营、人工复核、资金解释、异常升级与审计 SOP。
- 适用范围仅限后台运营能力与接口运行治理：
  - `GET /booking/commission/list-by-technician`
  - `GET /booking/commission/list-by-order`
  - `GET /booking/commission/pending-amount`
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `GET /booking/commission/config/list`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`
- 代码与文档真值来源：
  - `TechnicianCommissionController`
  - `TechnicianCommissionServiceImpl`
  - `TechnicianCommissionRespVO`
  - `TechnicianCommissionConfigSaveReqVO`
  - `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
- 当前单一真值：
  - `BO-004` 已核到真实 controller 接口
  - 当前未核到独立后台页面文件，也未核到独立前端 API 文件
  - 所以本文针对的是“后台运营能力与接口运行治理”，不是“页面功能已上线”

## 2. 当前代码真值与处理原则
- 页面边界固定：
  - `BO-003 /booking/commission-settlement/*` 已有真实后台页面
  - `BO-004 /booking/commission/*` 当前没有独立后台页面闭环证据
  - 不得把 `/booking/commission-settlement/*` 的页面成功样本冲抵 `/booking/commission/*` 的页面样本
- 当前无服务端 `degraded=true / degradeReason` 证据：
  - `TechnicianCommissionController` 返回模型只有 `CommonResult.code/msg/data`
  - 本 SOP 不得杜撰服务端降级逻辑
- 写操作失败一律不得伪成功：
  - 当前 `settle`、`batch-settle`、`config/save`、`config/delete` 都返回 `success(true)`
  - 但代码未统一校验“记录不存在 / 已结算 / 更新 0 行 / 删除 0 行”
  - 所以后台运营必须执行“写前查询 + 写后回读”双步骤，不能把 `code=0 && data=true` 直接当成状态已落库
- 权限边界固定：
  - 查询：`booking:commission:query`
  - 结算：`booking:commission:settle`
  - 配置：`booking:commission:config`
- 资金解释边界固定：
  - `BO-004` 的直结接口只会把佣金记录状态改为“已结算”
  - 它本身不创建新的结算单，不承诺已打款，不等于 `BO-003` 的审核 / 打款闭环
  - 若回读后 `settlementId` 仍为空，客服与财务不得解释为“已有结算单 / 已完成付款”

## 3. 角色与职责

| 层级 | 角色 | 职责 | 首响时限 |
|---|---|---|---|
| `L1` | 财务运营值班 / 后台运营 | 执行按技师 / 按订单查询、待结算金额核对、配置查询、写前写后回读 | 5 分钟 |
| `L2` | 财务负责人 / 区域运营复核 | 人工复核状态变更、金额解释、配置重复或误删、权限异常 | 15 分钟 |
| `L3` | Booking Admin on-call / 发布负责人 | 执行冻结写操作、暂停批量结算、回滚配置变更、发布阻断 | P0 5 分钟，P1 15 分钟 |

## 4. 标准处理流程

### 4.1 通用步骤
1. 先确认当前是：
   - 按技师查佣金
   - 按订单查佣金
   - 查待结算金额
   - 单条结算
   - 按技师批量结算
   - 佣金配置查询 / 保存 / 删除
2. 记录最小审计键：
   - `technicianId`
   - `storeId`
   - `orderId`
   - `commissionId`
   - `settlementId`
   - `runId`
   - `sourceBizNo`
   - `errorCode`
   - 无值统一填 `"0"`
3. 判断是查询类还是写类：
   - 查询类允许空态，但不得误报页面成功
   - 写类必须强制执行写后回读，不得只看 `success(true)`
4. 按场景表执行运营审核、人工复核、资金解释和升级动作。

### 4.2 写操作统一双读规则
1. 写前先查询，拿到目标记录真实主键和旧状态。
2. 执行写接口。
3. 写后重新查询同一作用域。
4. 只有读到目标状态真实变化，才允许标记“操作成功”。
5. 若接口返回 `success(true)` 但读后状态未变：
   - 统一按“写操作失败 / 疑似伪成功”处理
   - 不得对财务、运营、门店或其他窗口回报成功

## 5. 场景操作 SOP

### 5.1 按技师查佣金

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 返回佣金列表 | 核对 `technicianId`、`storeId`、`commissionAmount`、`status`、`sourceBizNo` | 无需默认升级 | 这是佣金记录查询，不代表已有页面上线证据 | 同一技师 15 分钟内多次查无结果但业务预期应有记录 |
| 返回空列表 | 先按合法空态记录 | 若业务预期应有记录，转 `L2` 复核履约与计提链路 | “当前未查到该技师佣金记录”，不得解释为页面成功闭环 | 同门店 / 同技师连续空态投诉 3 次以上 |
| 请求失败 / 权限失败 | 记录 `errorCode` 与权限点 | 转 `L2/L3` 校验 `booking:commission:query` | 仅解释为后台查询失败，不得解释成“暂无佣金” | 集中 `403` 或接口失败 |

### 5.2 按订单查佣金

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 返回订单佣金记录 | 核对 `orderId`、`commissionId`、`technicianId`、`commissionAmount` | 无需默认升级 | 这是订单关联佣金查询，不等于结算单查询 | 记录状态与其他账务链路冲突 |
| 返回空列表 | 先按合法空态记录 | 若订单已履约且预期已计提，转 `L2` 复核 | “当前未查到该订单佣金记录”，不得说成“订单没有提成问题” | 履约完成订单持续空态 |
| 请求失败 | 记录 `orderId/errorCode` | 转 `L2/L3` | 不得说成“订单无佣金” | 15 分钟集中失败 |

### 5.3 查待结算金额

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 返回正金额 | 同步核对该技师 `PENDING` 记录明细 | 一般不升级 | 仅代表待结算佣金总额，不代表已创建结算单 | 待结算金额与明细合计不一致 |
| 返回 `0` | 先按合法空金额记录 | 若需要解释原因，转 `L2` 复核是否无待结算记录或待结算记录金额合计为 0 | “当前待结算总额为 0”，不得自动解释为“已打款完结” | 与明细存在明显冲突 |
| 请求失败 | 记录 `technicianId/errorCode` | 转 `L2/L3` | 不得说成“待结算金额为 0” | 连续 2 个窗口失败 |

### 5.4 单条结算

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 写前存在 `PENDING` 记录，写后变 `SETTLED` | 记录 `commissionId`、`technicianId`、`orderId`、`sourceBizNo`、新 `settlementTime` | 无需默认升级 | 仅表示“佣金记录已结算”，不表示“已打款” | 若 `settlementId` 为空仍被外部口径解释成已打款 |
| 接口返回 `true` 但写后仍是 `PENDING` / 记录不存在 | 统一判定为写失败 | 立即转 `L2/L3` | 不得汇报成功，这是伪成功风险 | 直接按 `P0` |
| 写前记录已是 `SETTLED/CANCELLED` | 不允许直接操作，先回退到查询与人工复核 | 转 `L2` | 不得继续说“已再次结算成功” | 操作员重复提交或状态误判集中出现 |

### 5.5 按技师批量结算

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 写前有待结算，写后待结算数与待结算金额下降到预期 | 记录 `technicianId`、写前后 `pendingCount/pendingAmount` | 无需默认升级 | 仅表示该技师待结算佣金已被标记结算，不表示已经完成结算单审批或付款 | 若与 BO-003 审批口径被混写 |
| 接口返回 `true` 但写后待结算数 / 金额无变化 | 统一判定为批量结算失败 | 立即转 `L2/L3` | 不得说成“已批量结算完成” | 直接按 `P0` |
| 写前就无待结算记录 | 按合法空操作记录 | 如需原因解释，转 `L2` | “当前无可批量结算记录”，不得说成“本次批量结算成功” | 连续误操作或脚本重复触发 |

### 5.6 佣金配置查询

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 返回配置列表 | 核对 `storeId/commissionType/rate/fixedAmount` | 无需默认升级 | 这只是门店覆盖配置，不查到配置时系统会回退默认佣金比例 | 查询结果与预期门店配置不一致 |
| 返回空列表 | 按合法空配置记录 | 若门店应有覆盖配置，转 `L2` | “当前门店没有覆盖配置，系统将按默认佣金类型比例计算” | 覆盖配置误丢失集中出现 |
| 请求失败 | 记录 `storeId/errorCode` | 转 `L2/L3` | 不得说成“当前门店无配置” | 集中失败或权限失败 |

### 5.7 佣金配置保存

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 写后回读存在且仅存在 1 条目标 `storeId+commissionType` 配置，字段与预期一致 | 记录 `storeId/commissionType/rate/fixedAmount` | 无需默认升级 | 仅表示配置已落库，不表示历史佣金会自动重算 | 历史金额被误解释为已追溯更新 |
| 接口返回 `true` 但读后无记录 / 仍是旧字段 | 判定为保存失败 | 立即转 `L2/L3` | 不得回报“配置保存成功” | 直接按 `P0` |
| 写后同一 `storeId+commissionType` 出现多条记录 | 判定为配置重复风险 | 转 `L2/L3` 处理重复项 | 不得继续放量使用该配置 | 直接按 `P1` |

### 5.8 佣金配置删除

| 场景 | 运营审核动作 | 人工复核 | 资金解释口径 | 升级条件 |
|---|---|---|---|---|
| 写后回读目标 `id` 已消失 | 记录删除前快照与删除后结果 | 无需默认升级 | 仅表示覆盖配置被移除，后续计算将回退默认佣金比例 | 删除导致金额解释争议 |
| 接口返回 `true` 但目标 `id` 仍存在 | 判定为删除失败 | 立即转 `L2/L3` | 不得回报“已删除成功” | 直接按 `P0` |
| 删除后仍存在同类重复配置 | 转 `L2` 做人工清理 | 如有跨门店影响转 `L3` | 不得解释为“已完全回退默认配置” | 配置残留影响多个门店 |

## 6. 资金解释与运营审核口径
- `BO-004` 只允许使用以下口径：
  - “佣金记录已结算 / 待结算 / 已取消”
  - “当前未查到佣金记录”
  - “当前门店未配置覆盖规则，系统按默认比例计算”
- `BO-004` 禁止使用以下口径：
  - “页面已上线并可稳定使用”
  - “已完成打款”
  - “已进入结算单审批流”
  - “已生成结算单”
- 只有当回读到真实 `settlementId` 且另有 BO-003 审批 / 打款证据时，才允许解释结算单状态。
- `pending-amount=0` 的合法原因只允许写成：
  - 当前无待结算记录
  - 待结算记录金额合计为 0
  - 不得自动写成“财务已结清”

## 7. 异常升级路径

| 级别 | 典型触发 | Owner | 升级链 | 首响 / 缓解 / 关闭 |
|---|---|---|---|---|
| `P0` | 任一写接口 `success(true)` 但读后状态未变；BO-003 页面样本被用来冲抵 BO-004；批量结算伪成功 | Booking Admin on-call + 发布负责人 | 财务运营值班 -> 财务负责人 -> Booking Admin on-call -> 发布负责人 | `5m / 15m / 4h` |
| `P1` | 待结算金额与明细合计不一致；配置保存后重复；配置删除残留；权限集中失败 | 财务负责人 / Booking Admin on-call | 财务运营值班 -> 财务负责人 -> Booking Admin on-call | `15m / 1h / 24h` |
| `P2` | 单技师空态争议、单门店空配置争议、局部查询失败 | 财务运营支撑 | 财务运营值班 -> 财务负责人 | `30m / 4h / 72h` |

## 8. 审计字段模板

| 字段 | 说明 | 规则 |
|---|---|---|
| `technicianId` | 技师主键 | 无值填 `"0"` |
| `storeId` | 门店主键 | 无值填 `"0"` |
| `orderId` | 订单主键 | 无值填 `"0"` |
| `commissionId` | 佣金主键 | 写操作必填；无值填 `"0"` |
| `settlementId` | 结算单主键 | 仅回读到真实值时填写；BO-004 直结可填 `"0"` |
| `runId` | 发布 / 巡检 / 手工批次号 | controller 未显式提供时填 `"0"` |
| `sourceBizNo` | 佣金追溯业务号 | 查询回读有值则透传；配置操作无值填 `"0"` |
| `errorCode` | 接口业务码或 `"0"` | 无真实服务端错误码时填 `"0"`，不得自造 |
| `permission` | 本次调用权限点 | `booking:commission:query|settle|config` |
| `readbackResult` | 写后回读结论 | `changed/unchanged/not-found/duplicate` |

## 9. 验收清单
- [ ] 文档明确 `BO-004` 当前未核到独立后台页面文件。
- [ ] 文档明确本 SOP 针对的是“后台运营能力与接口运行治理”，不是页面已上线。
- [ ] 文档明确不得拿 `/booking/commission-settlement/*` 页面成功样本冲抵 `/booking/commission/*`。
- [ ] 文档明确当前无服务端 `degraded=true / degradeReason` 证据。
- [ ] 文档明确所有写操作必须写后回读，失败不得伪成功。
- [ ] 文档覆盖按技师查佣金、按订单查佣金、查待结算金额、单条结算、批量结算、配置查询 / 保存 / 删除。
