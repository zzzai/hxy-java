# MiniApp Finance Ops Commission Admin Runbook - Window D Handoff (2026-03-14)

## 1. 变更摘要
- 新增 `BO-004` 运营 SOP：
  - `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-admin-sop-v1.md`
  - 覆盖按技师查佣金、按订单查佣金、待结算金额、单条结算、批量结算、配置查询 / 保存 / 删除，以及运营审核、人工复核、资金解释、异常升级路径。
- 新增 `BO-004` runbook：
  - `docs/plans/2026-03-14-miniapp-finance-ops-technician-commission-admin-runbook-v1.md`
  - 覆盖最小监控字段、关键主键、合法空态、写操作告警等级、回滚 / 降级 / 暂停操作、页面未闭环时的仅接口治理。
- 更新告警 owner 路由：
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
  - 新增 `finance-ops-admin` 的 `P0/P1/P2` 路由，不改既有 03-10 五域冻结规则。

## 2. 当前真值结论
- `BO-004` 当前只有 `TechnicianCommissionController` 接口真值。
- 当前未核到独立后台页面文件，也未核到独立前端 API 文件。
- 因此本批 SOP / runbook 固定服务于“后台运营能力与接口运行治理”，不是“页面功能已上线”。
- 当前没有服务端 `degraded=true / degradeReason` 证据：
  - 本批文档没有写任何后端 degraded 逻辑
  - 降级只允许写成查询保留、冻结写操作、切换人工复核

## 3. 关键运行边界
- `/booking/commission/*` 与 `/booking/commission-settlement/*` 必须严格分池：
  - `BO-003` 页面成功样本不能冲抵 `BO-004`
  - `BO-004` 接口成功也不能反推页面闭环
- `settle/batch-settle/config save/delete` 当前都可能出现 `success(true)` 但无真实状态变化：
  - 文档已固定必须“写前查询 + 写后回读”
  - 读后未变统一按伪成功风险处理
- `pending-amount=0`、空列表、空配置都可能是合法空态：
  - 但只能记为空态，不能记成页面成功、结算成功或打款成功

## 4. 对窗口 A / B / C 的联调提醒
- A（集成 / 发布）
  - `BO-004` 仍只能写成“仅接口闭环 + 页面真值待核”。
  - 不能拿 `commission-settlement/index.vue` 或 `commission-settlement/outbox/index.vue` 的页面样本冲抵 `BO-004`。
  - `finance-ops-admin` 告警路由是新增分组，不改变既有 03-10 五域冻结规则。
- B（产品 / 运营）
  - 资金解释必须收口到“佣金记录已结算 / 待结算 / 已取消”，不能把 `BO-004` 直结说成“已打款”或“已生成结算单”。
  - `pending-amount=0` 只能解释为当前待结算合计为 0，不等于财务已结清。
  - 配置空列表只能解释为门店无覆盖配置、系统按默认比例回退。
- C（契约 / 后端）
  - 当前 `COMMISSION_NOT_EXISTS(1030007000)`、`COMMISSION_ALREADY_SETTLED(1030007001)` 虽在错误码枚举中存在，但 `TechnicianCommissionController` 这条 admin 写路径当前没有实际抛出证据，不能按它们做当前稳定分支判断。
  - `errorCode=0` 但读后未变时，只能记为 readback 异常或伪成功，不能补写不存在的业务错误码。
  - 当前无服务端 `degraded=true / degradeReason` 字段，不能补写降级返回。

## 5. 固定验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
