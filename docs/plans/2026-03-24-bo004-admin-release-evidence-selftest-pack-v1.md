# BO-004 Admin Release Evidence Selftest Pack v1 (2026-03-24)

## 1. 目标
- 为 `BO-004 技师提成明细 / 计提管理` 建立一套仓内可执行的模拟样本包。
- 这套样本包只用于验证证据结构、门禁脚本和误写边界。
- 它不是真实 release evidence，不改变 `Can Develop / Cannot Release`。

## 2. 样本包目录
- `tests/fixtures/bo004-admin-release-evidence-simulated/`

## 3. 样本组成
- `menu-navigation.json`
- `query-list-by-technician.json`
- `write-settle-readback.json`
- `config-save-readback.json`
- `gray-stage.json`
- `rollback-drill.json`
- `signoff.json`

## 4. 固定边界
- 所有样本的 `sampleMode` 固定为 `SIMULATED_SELFTEST`。
- `signoff.json` 的 `decision` 固定为 `SELFTEST_ONLY_NO_RELEASE`。
- `gray-stage.json`、`rollback-drill.json`、`signoff.json` 只说明仓内流程跑通，不说明真实环境已拿到灰度、回滚、签发证据。

## 5. 校验脚本
- `ruoyi-vue-pro-master/script/dev/check_bo004_admin_release_evidence_gate.sh`
- 用法：
```bash
bash ruoyi-vue-pro-master/script/dev/check_bo004_admin_release_evidence_gate.sh \
  --repo-root . \
  --sample-pack-dir tests/fixtures/bo004-admin-release-evidence-simulated
```

## 6. 当前结论
- 该样本包解决的是“证据结构能否被仓内持续校验”。
- 它不解决“真实页面请求样本、真实菜单执行样本、真实灰度/回滚/sign-off 证据”缺口。
