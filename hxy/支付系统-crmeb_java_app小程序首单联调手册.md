# 支付系统-crmeb_java_app小程序首单联调手册

## 1. 关键结论

- 使用工程：`crmeb_java/app`
- 支付方式：JSAPI（小程序内 `wx.requestPayment`）
- 不是“商户静态收款二维码”模式

## 2. 联调前检查（1分钟）

在 `crmeb_java/crmeb` 目录执行：

```bash
EXPECTED_APPID='wx97fb30aed3983c2c' ./shell/payment_mp_app_precheck.sh
```

通过标准：
- `mp-weixin.appid` 为 `wx97fb30aed3983c2c`
- `pay_routine_appid/mchid/key` 已配置
- `前后端 AppID 一致性` 为 PASS

注意：
- 如果 `API 域名` 或 `api_url` 是 HTTP，会出现 WARN。真机支付建议 HTTPS 公网域名。

## 3. 用 HBuilderX 打开并运行

1. 打开 HBuilderX。
2. `文件 -> 打开目录`，选择 `crmeb_java/app`。
3. 打开 `manifest.json`，确认：
   - `微信小程序配置(mp-weixin)` 的 `AppID = wx97fb30aed3983c2c`
4. 菜单 `运行 -> 运行到小程序模拟器 -> 微信开发者工具`。

## 4. 上传体验版（用于手机真机支付）

1. 在 HBuilderX 菜单点：`发行 -> 小程序-微信`。
2. 选择“上传”（不是仅本地运行）。
3. 上传完成后，登录微信公众平台：
   - `开发管理 -> 开发版本`
   - 将该版本设置为“体验版”
4. 把体验二维码发给测试微信号（该号需在体验成员名单中）。

## 5. 手机真机发起 0.01 支付

1. 手机微信扫码体验版二维码打开小程序。
2. 登录账号 -> 选择商品/服务 -> 下单。
3. 支付页选择微信支付，点击“立即支付”。
4. 出现微信支付收银台并完成 0.01 元支付。

## 6. 取真实订单号（给联调脚本）

在数据库执行：

```sql
SELECT so.order_id, so.out_trade_no, so.pay_time, so.pay_price,
       wpi.transaction_id, wpi.trade_state
FROM eb_store_order so
LEFT JOIN eb_wechat_pay_info wpi ON wpi.out_trade_no = so.out_trade_no
WHERE so.pay_type='weixin' AND so.paid=1
ORDER BY so.pay_time DESC
LIMIT 10;
```

把 `order_id` 发给联调脚本即可。

## 7. 拿到订单号后的脚本

```bash
./shell/payment_fullchain_drill.sh --order-no <ORDER_ID>
./shell/payment_go_nogo_decision.sh --date $(date -d 'yesterday' +%F) --order-no <ORDER_ID>
./shell/payment_cutover_gate.sh --date $(date -d 'yesterday' +%F) --order-no <ORDER_ID>
```

