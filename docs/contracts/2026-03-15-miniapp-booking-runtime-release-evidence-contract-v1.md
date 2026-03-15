# MiniApp Booking Runtime Release Evidence Contract v1 (2026-03-15)

## 1. 目标与引用
- 目标：把 booking runtime 当前可用于发布判断的 contract 证据分成三层写清：
  - 静态对齐证据
  - 运行证据
  - 发布证据
- 当前 booking contract / canonical API / stable errorCode 的唯一细表引用：
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`

## 2. 当前 contract 结论
- 当前只确认以下事实已经成立：
  - `booking.js` 与 booking app controller 的 canonical method/path 已收口。
  - `logic.js` 的成功跳转 / 失败不跳转、成功刷新 / 失败不刷新 helper 行为有 node smoke 证据。
  - booking runtime gate 与 shared chain 接入只证明“边界仍是 `Can Develop / Cannot Release`”。
- 当前仍不能确认以下事实已经成立：
  - booking 六个页面的字段绑定已闭环
  - create / cancel / addon 已有读后回写样本
  - create / cancel / addon 已有发布级 success/failure 样本包

## 3. 当前必须继续保留的 blocker
- legacy blocker 继续保留：
  - `GET /booking/technician/list-by-store`
  - `GET /booking/time-slot/list`
  - `PUT /booking/order/cancel`
  - `POST /booking/addon/create`
- field / shape drift 继续保留：
  - technician 页 `title/specialties/status` 没有 backend 响应字段绑定
  - order-list 读取 `data.list/data.total`，controller 实际返回 `data[]`
  - order-list / order-detail 当前都没有 `payOrderId` 的响应字段绑定证据
  - order-confirm 依赖 `date/spuId/skuId/duration`，但当前 page -> slot response -> create req 链路没有闭环
  - addon 页只提交 `parentOrderId,addonType`，没有当前真实 `skuId/spuId` 绑定

## 4. Stable ErrorCode 只认以下边界

| code | 当前 stable scope | 当前不能误写成什么 |
|---:|---|---|
| `1030003001` | create、addon(extend) | 不得误写成 `1030002001/1030003002` |
| `1030004000` | cancel、addon | 不得误写成 order-detail miss 的稳定 code；也不得误简化成 add-on 只会表示“母单不存在” |
| `1030004001` | addon only | 不得误写成 cancel 稳定分支 |
| `1030004005` | cancel only | 不得被 `1030004001` 覆盖 |
| `1030004006` | order-detail、cancel、addon | 只能按 code 分支，不能按错误文案分支 |

补充：
- 当前不得把 `1030001000/1030001001/1030002001/1030003002` 写成 booking runtime page 的稳定运行分支。
- 当前 booking runtime page contract 没有 `MANUAL_RETRY_3` 的已提交证据。

## 5. Fail-open / fail-close / retryClass
- `FAIL_OPEN`
  - `GET /booking/technician/list`
  - `GET /booking/technician/get`
  - `GET /booking/slot/list-by-technician`
  - `GET /booking/order/list`
  - 以上范围的 `[]` 或 `success(null)` 只是结构态，不是成功样本，也不是 `degraded`
- `FAIL_CLOSE`
  - `GET /booking/order/get` 的越权分支
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- `NO_AUTO_RETRY`
  - create
  - technician-list / technician-detail query
  - technician slot query
  - order-detail query
  - addon
- `REFRESH_ONCE`
  - order-list query 的人工刷新
  - cancel 成功后的单次刷新

## 6. `pseudo success / no-op risk`
- add-on 页当前最大风险不是 message 漂移，而是 payload 绑定漂移：
  - `upgrade` 路径缺 `skuId` 时，当前可能直接落 `1030004000`
  - `add-item` 路径缺 `spuId/skuId` 时，当前服务实现可能仍返回 `code=0` 并写入零价格空商品订单
- 因此当前 `POST /app-api/booking/addon/create` 必须保留：
  - `pseudo success / no-op risk`
  - 不能把 `code=0` 直接写成 add-on 成功
- cancel 也必须保留：
  - 只有读后回写状态确实变化，才能写成成功
  - 当前没有已提交读后回写样本，不能把单次 `code=0` 直接当发布证据

## 7. 静态对齐、运行证据、发布证据
- 静态对齐证据：
  - `booking.js`
  - `logic.js`
  - `booking-api-alignment.test.mjs`
  - `booking-page-smoke.test.mjs`
  - booking app controller `@GetMapping/@PostMapping`
- 运行证据：
  - 当前只核到 helper 控制流 smoke
  - 当前没有页面字段绑定、read-after-write、真实 response shape 的已提交运行样本
- 发布证据：
  - 当前没有 create / cancel / addon 的发布级 success/failure 样本包
  - 当前没有 allowlist、巡检日志、回放证据
  - 因此 booking 当前只能写成 `Can Develop=Yes / Can Release=No`

## 8. 当前没有证据的 `degraded=true / degradeReason`
- `booking.js`
- `logic.js`
- booking 六个真实页面
- booking app controller/service
- 当前两份 booking node smoke test

以上范围都没有已提交服务端 `degraded=true / degradeReason` 证据。

## 9. 当前 No-Go 条件
1. 把 route 对齐误写成页面字段闭环。
2. 把 smoke test、runtime gate `PASS`、shared chain `rc=0` 写成发布证据闭环。
3. 把 `[] / null / 0` 写成成功样本。
4. 把 `code=0` 但写后未读到预期变化写成成功。
5. 把 `1030001000/1030001001/1030002001/1030003002` 写成 booking runtime page 稳定 code。
