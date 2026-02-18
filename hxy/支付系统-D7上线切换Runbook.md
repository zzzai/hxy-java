# 支付系统-D7上线切换Runbook

- 日期: 2026-02-15
- 目标: 支付能力可灰度、可回滚、可审计

## 1. 上线前检查（T-1天）
1. 配置检查
   - `pay_weixin_open=1`
   - 渠道配置完整（appid/mchid/key/cert）
   - 门店映射文件已 dry-run 通过
2. 网络检查
   - 回调域名 HTTPS 可公网访问
   - 防火墙与网关已放通支付回调
3. 数据检查
   - 对账SQL模板可执行
   - 历史异常单已清理到可控范围

## 2. 灰度步骤（T日）
1. 第1批（5店）切换
   - 导入 `storeId -> sub_mchid`
   - 仅放量 5 家高可控门店
2. 观察 30-60 分钟
   - 下单成功率、回调成功率、差错单数
3. 第2批（20店）切换
   - 无P1/P2异常再扩容
4. 全量切换
   - 完成后执行一次专项对账

## 3. 回滚策略
1. 配置回滚
   - 回退到上一个稳定配置快照
2. 门店回滚
   - 仅回退异常门店映射，不影响其他门店
3. 开关回滚
   - 必要时临时关闭微信支付入口，保留余额/其他可用渠道

## 3.1 回滚演练SOP（新增，建议每周一次）
1. 演练前快照
   - `MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_config_snapshot.sh --tag rollback-drill-pre`
2. 门店映射上线拦截规则预检（不改库）
   - `MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_cutover_gate.sh --date 2026-02-15 --no-alert`
3. 触发并验证回滚路径（dry-run）
   - `MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_store_mapping_placeholder_cleanup.sh`
   - `MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_store_mapping_cross_channel_audit.sh --no-alert`
4. 执行回滚动作（按故障类型二选一）
   - 配置回滚：`MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_config_restore.sh --latest --apply`
   - 映射回滚：`MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_store_mapping_rollback.sh --latest --apply`
5. 回滚后复核
   - `MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_cutover_gate.sh --date 2026-02-15 --no-alert`
   - `./shell/payment_summary_contract_check.sh --date 2026-02-15 --no-alert`
6. 演练验收标准
   - 30 分钟内可完成回滚 + 复核
   - `payment_cutover_gate` 无 BLOCK
   - 对账差异与工单数无异常放大

## 4. 上线后2小时守护
1. 重点看板
   - 统一下单成功率
   - 回调失败率
   - 查单补偿量
   - 退款异常量
2. 必做动作
   - 每30分钟出一次简报
   - 发现 P1 立即止损并升级

## 5. 产出物归档
1. 本次上线配置快照
2. 门店映射版本
3. 监控曲线截图
4. 差错单处理记录
5. 复盘文档（问题、原因、改进）

## 6. 自动化脚本（已落地）
1. 日对账
   - `crmeb_java/crmeb/shell/payment_reconcile_daily.sh`
   - 输出分层：`main_diff_raw.tsv`、`main_diff_cleared_refund.tsv`、`main_diff.tsv`
2. 监控巡检 + 告警
   - `crmeb_java/crmeb/shell/payment_monitor_quickcheck.sh`
   - `crmeb_java/crmeb/shell/payment_monitor_alert.sh`
   - `crmeb_java/crmeb/shell/payment_alert_notify.sh`
3. 全链路演练
   - `crmeb_java/crmeb/shell/payment_fullchain_drill.sh`
4. 定时任务安装器
   - `crmeb_java/crmeb/shell/payment_ops_cron.sh`
5. 一键日常巡检
   - `crmeb_java/crmeb/shell/payment_ops_daily.sh`
6. 日报生成
   - `crmeb_java/crmeb/shell/payment_daily_report.sh`
7. 切换预演（无订单号）
   - `crmeb_java/crmeb/shell/payment_cutover_rehearsal.sh`
8. 配置快照与回滚
   - `crmeb_java/crmeb/shell/payment_config_snapshot.sh`
   - `crmeb_java/crmeb/shell/payment_config_restore.sh`
9. 切换编排（dry-run/apply）
   - `crmeb_java/crmeb/shell/payment_cutover_apply.sh`
10. go/no-go一页判定
   - `crmeb_java/crmeb/shell/payment_go_nogo_decision.sh`
11. 早班一键包
   - `crmeb_java/crmeb/shell/payment_ops_morning_bundle.sh`
12. 值守总览
   - `crmeb_java/crmeb/shell/payment_ops_status.sh`
13. 值守总览离线自测
   - `crmeb_java/crmeb/shell/payment_ops_status_smoke.sh`
14. summary 协议回归检查
   - `crmeb_java/crmeb/shell/payment_summary_contract_check.sh`
15. summary 协议离线自测
   - `crmeb_java/crmeb/shell/payment_summary_contract_smoke.sh`
16. 门店映射模板导出
   - `crmeb_java/crmeb/shell/payment_store_mapping_template_export.sh`
17. 对账SLA守卫
   - `crmeb_java/crmeb/shell/payment_reconcile_sla_guard.sh`
18. 对账工单自动分级
   - `crmeb_java/crmeb/shell/payment_reconcile_ticketize.sh`
19. 占位号清理工具
   - `crmeb_java/crmeb/shell/payment_store_mapping_placeholder_cleanup.sh`
20. 真实映射CSV生成器
   - `crmeb_java/crmeb/shell/payment_store_mapping_csv_generate.sh`
21. 跨渠道唯一性审计
   - `crmeb_java/crmeb/shell/payment_store_mapping_cross_channel_audit.sh`

## 7. 推荐执行命令
1. 对账执行（T+1）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_reconcile_daily.sh --date 2026-02-15
```
2. 演练执行（指定真实订单）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_fullchain_drill.sh --order-no <真实订单号>
```
3. 安装 cron（先 dry-run，再 apply）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
./shell/payment_ops_cron.sh --mysql-defaults-file "$HOME/.my.cnf" --report-notify 1
ALERT_WEBHOOK_URL='https://你的机器人webhook' ./shell/payment_ops_cron.sh --apply --mysql-defaults-file "$HOME/.my.cnf" --report-notify 1 --ticket-notify 1 --ticket-max-rows 200 --ticket-amount-p1-threshold-cent 100000 --warroom-notify 1 --go-nogo-notify 1 --go-nogo-require-booking-repair-pass 0 --morning-bundle-notify 1 --morning-bundle-require-booking-repair-pass 0 --status-notify 1 --status-refresh 1 --status-require-booking-repair-pass 0 --status-max-summary-age-minutes 240 --status-max-recon-age-days 2 --status-max-daily-report-age-days 2 --contract-notify 1 --sla-notify 1 --sla-lookback-days 14 --sla-days 1 --retention-notify 1 --booking-repair-notify 1 --booking-repair-window-hours 72 --booking-repair-apply 0
```
4. 飞书机器人安装示例
```bash
cd /root/crmeb-java/crmeb_java/crmeb
ALERT_WEBHOOK_TYPE='feishu' \
ALERT_WEBHOOK_URL='https://open.feishu.cn/open-apis/bot/v2/hook/你的token' \
./shell/payment_ops_cron.sh --apply --mysql-defaults-file "$HOME/.my.cnf" --report-notify 1 --ticket-notify 1 --ticket-max-rows 200 --ticket-amount-p1-threshold-cent 100000 --warroom-notify 1 --go-nogo-notify 1 --go-nogo-require-booking-repair-pass 0 --morning-bundle-notify 1 --morning-bundle-require-booking-repair-pass 0 --status-notify 1 --status-refresh 1 --status-require-booking-repair-pass 0 --status-max-summary-age-minutes 240 --status-max-recon-age-days 2 --status-max-daily-report-age-days 2 --contract-notify 1 --sla-notify 1 --sla-lookback-days 14 --sla-days 1 --retention-notify 1 --booking-repair-notify 1 --booking-repair-window-hours 72 --booking-repair-apply 0
```
5. 一键巡检（无订单号阶段）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_ops_daily.sh --date 2026-02-15
```
6. 生成支付日报
```bash
cd /root/crmeb-java/crmeb_java/crmeb
./shell/payment_daily_report.sh --date 2026-02-15
```
7. 上线前切换预演（无订单号）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_cutover_rehearsal.sh --date 2026-02-15 --window-hours 72 --no-alert
```
> 说明：默认会先做配置快照；如仅排查流程可用性可加 `--skip-snapshot`。
8. 配置快照（切换前）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_config_snapshot.sh --tag before-sp-cutover
```
9. 配置回滚（仅异常时）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
./shell/payment_config_restore.sh --latest
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_config_restore.sh --latest --apply
```
10. 切换编排 dry-run（推荐先跑）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" \
./shell/payment_cutover_apply.sh \
  --appid wx97fb30aed3983c2c \
  --sp-mchid 190000xxxx \
  --sp-key '你的APIv2Key' \
  --sub-mchid 190000yyyy \
  --non-strict-preflight --no-alert
```
11. 切换编排 apply（失败自动回滚）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" \
./shell/payment_cutover_apply.sh \
  --apply \
  --appid wx97fb30aed3983c2c \
  --sp-mchid 190000xxxx \
  --sp-key '你的APIv2Key' \
  --sub-mchid 190000yyyy
```
12. 一页式 go/no-go 判定（上线前最后一步）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_go_nogo_decision.sh --date 2026-02-15 --order-no <真实订单号>
```
> 若要求“必须有一次 apply 模式切换成功”再放行，可加：`--require-apply-ready 1`
> 已内置工单硬门槛：`ticket_p1>0` 会直接 NO_GO。
13. 早班一键包（无订单号阶段）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_ops_morning_bundle.sh --date 2026-02-15 --no-alert
```
> 该一键包已内置预约核销一致性回归（`payment_booking_verify_regression.sh`），会在 summary 中输出 `booking_verify_rc/severity`。
14. 值守总览（无订单号阶段）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_ops_status.sh --date 2026-02-15 --no-alert
```
> 建议加新鲜度阈值：`--max-summary-age-minutes 240 --max-recon-age-days 2 --max-daily-report-age-days 2`
15. 值守总览离线自测（上线前建议执行）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
./shell/payment_ops_status_smoke.sh --date 2026-02-15
```
16. 预约核销一致性回归（上线前建议执行）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_booking_verify_regression.sh --window-hours 72 --no-alert
```
17. 预约核销异常自动修复（先 dry-run，再按需 apply）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_booking_verify_repair.sh --window-hours 72 --no-alert
```
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_booking_verify_repair.sh --window-hours 72 --apply --no-alert
```
> 仅修复非资金字段：`check_in_time` 和 `locked_expire`。
18. summary 协议回归检查（上线前建议执行）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
./shell/payment_summary_contract_check.sh --date 2026-02-15 --no-alert
```
19. summary 协议离线自测（上线前建议执行）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
./shell/payment_summary_contract_smoke.sh --date 2026-02-15
```
20. 导出门店映射模板（给运营补全子商户号）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_store_mapping_template_export.sh
```
21. 对账SLA守卫（上线前建议执行）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
./shell/payment_reconcile_sla_guard.sh --lookback-days 14 --sla-days 1 --no-alert
```
22. 对账工单自动分级（上线前建议执行）
```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE="$HOME/.my.cnf" ./shell/payment_reconcile_ticketize.sh --date 2026-02-15 --amount-p1-threshold-cent 100000 --no-alert
```
