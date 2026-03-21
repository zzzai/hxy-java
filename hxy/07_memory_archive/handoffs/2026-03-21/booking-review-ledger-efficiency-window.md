# Booking Review Ledger Efficiency Window Handoff（2026-03-21）

## 1. 本批目标
- 在不新增后端接口的前提下，降低 booking review 后台值班处理的点击成本，并把 SLA 超时项拉成可直接处理的工作面。

## 2. 本批新增能力
1. 台账页新增快捷筛选：
   - `待认领优先`
   - `认领超时`
   - `首次处理超时`
   - `闭环超时`
   - `历史待初始化`
2. 台账页新增快捷动作：
   - `快速认领`
   - `记录首次处理`
   - `标记闭环`
3. 以上动作仍调用既有服务端接口：
   - `/booking/review/manager-todo/claim`
   - `/booking/review/manager-todo/first-action`
   - `/booking/review/manager-todo/close`
4. 本批只把动作入口前移，不改变状态机、不新增自动化修复、不改变 release 结论。

## 3. 涉及文件
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
- `tests/booking-review-admin-ledger-efficiency.test.mjs`
- `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

## 4. 当前结论

| 维度 | 当前结论 |
|---|---|
| 当前是否可开发 | `Yes` |
| 当前是否可放量 | `No` |
| 变更语义 | `Ops Efficiency Only` |
| Release Decision | `No-Go` |

## 5. 对后续开发的注意点
1. 不得把“台账页可以直接认领/闭环”写成“系统已自动通知店长”。
2. `历史待初始化` 仍然只读识别加人工写入触发，不能写成读链路自动补齐。
3. 后续若接企微/App 双通道通知，应优先把快捷动作与通知状态串联，而不是继续堆叠更多筛选项。
