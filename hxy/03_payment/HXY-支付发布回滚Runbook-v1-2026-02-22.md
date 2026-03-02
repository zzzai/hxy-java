# HXY 支付发布回滚 Runbook V1（2026-02-22）

## 1. 适用范围

1. RuoYi 底座支付主链路（下单、回调、退款、对账、门禁）。
2. 阶段 A 的 `#19`（异常回放）与 `#20`（上线拦截）发布前门禁。

## 2. 发布前门禁（必须）

在 `ruoyi-vue-pro-master` 执行：

```bash
RUN_TESTS=1 \
RUN_NOTIFY_SMOKE=1 \
RUN_RETRY_POLICY_CHECK=1 \
RUN_PARTNER_READINESS_CHECK=1 \
RUN_RECONCILE_CHECK=0 \
REQUIRE_RECONCILE=1 \
bash script/dev/run_payment_stagea_p0_19_20.sh
```

发布准入标准：

1. `pipeline_exit_code=0`
2. `gate_result` 不是 `BLOCK`
3. `scenario_block_count=0`
4. `artifact_index.md`、`ci_gate_fail_logs.md` 已生成

## 3. 产物核查路径

1. `.tmp/payment_stagea_p0_19_20/<run_id>/artifact_index.md`
2. `.tmp/payment_stagea_p0_19_20/<run_id>/ci_gate_fail_logs.md`
3. `.tmp/payment_stagea_p0_19_20/<run_id>/release_gate_report.md`
4. `.tmp/payment_stagea_p0_19_20/<run_id>/logs/final_gate.log`

## 4. 回滚触发条件

触发任一条件立即回滚：

1. 支付成功率显著下降或连续异常告警。
2. 退款链路出现积压并超过 SLA。
3. 对账出现 BLOCK 级差异。
4. 门店核心交易无法完成且 5 分钟内无法恢复。

## 5. 回滚执行步骤

1. 立刻停止新版本灰度放量（冻结发布）。
2. 切回上一稳定镜像/版本（按当前环境部署方式执行）。
3. 运行一次 `#20` 门禁确认回退后状态：

```bash
RUN_TESTS=0 RUN_NOTIFY_SMOKE=1 RUN_RETRY_POLICY_CHECK=1 RUN_PARTNER_READINESS_CHECK=1 \
RUN_RECONCILE_CHECK=0 REQUIRE_RECONCILE=0 \
bash script/dev/run_payment_stagea_p0_19_20.sh
```

4. 记录回滚事件（时间、影响范围、根因、恢复时间）。
5. 进入工单复盘：代码修复、脚本补丁、二次发布窗口。

## 6. 值班沟通模板

1. 事件等级：P0/P1
2. 影响范围：门店数、订单数、退款数
3. 当前动作：冻结发布/回滚中/已恢复
4. 下一次通报时间：15 分钟内

## 7. 责任分工

1. 发布与回滚执行：技术负责人（你 + AI）。
2. 财务与风控确认：总部运营/财务接口人。
3. 门店沟通：运营负责人。
