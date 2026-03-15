# Window B Handoff - Booking Runtime 页面字段与恢复动作补充收口（2026-03-16）

## 1. 窗口范围
- 窗口：`B`
- worktree：`/root/crmeb-java/.worktrees/window-b-booking-runtime-prd-20260315`
- branch：`window-b-booking-runtime-prd-20260315`
- 本轮目标：
  - 补齐 booking runtime 页面字段字典 PRD
  - 补齐 booking runtime 用户结构态与恢复动作 PRD
  - 只回写既有 booking PRD，不改业务代码、不改 overlay 页面、不改历史 handoff

## 2. 本轮新增 / 更新文件
- 新增：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`
  - `hxy/07_memory_archive/handoffs/2026-03-16/booking-runtime-product-window-b.md`
- 更新：
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`

## 3. 本轮固定结论
- 页面 / 功能边界：
  - 只认 6 个真实 booking 页面
  - `technician-list / technician-detail / order-list / order-detail` 的查询侧文档真值 `Ready`
  - `order-confirm / cancel / addon` 的写链产品真值已闭环，但能力状态继续 `Still Blocked`
- formal contract 边界：
  - 当前页面/helper 已核出的 method/path/errorCode，若本分支 contract 尚未同步，统一保留 `Pending formal contract truth`
  - 不用 message 分支补产品口径

## 4. 对窗口 A / C / D / E 的联调注意点
- A：
  - 只能把 booking 写成“文档已闭环、工程未闭环、可开发但不可放量”
  - 不得把 `order-confirm`、取消、add-on 写成 `ACTIVE` 或 `Release Ready`
- C：
  - 可直接吸收的字段真值：
    - create：`timeSlotId`,`spuId?`,`skuId?`,`userRemark`,`dispatchMode=1`
    - cancel：`id`,`reason`
    - addon：`parentOrderId`,`addonType`
  - 需要保留 `Pending formal contract truth` 的点：
    - `order-confirm` 的 `loadTimeSlots(technicianId, null)`
    - `order-list` 的 `pageNo/pageSize/status`
- D：
  - 验收只认真实结构态和 helper 行为：
    - `暂无可用技师`
    - `该日期暂无可用时段`
    - `暂无预约`
    - `订单不存在`
    - create 失败不跳详情
    - cancel 失败不刷新
    - addon 失败不跳详情
- E：
  - 不得把空态 `[] / null / 0` 归档为成功样本
  - 不得把页面按钮存在归档为放量证据
