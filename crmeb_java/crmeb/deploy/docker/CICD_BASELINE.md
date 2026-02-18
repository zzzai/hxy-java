# CI/CD 基线草案（容器化）

目标：建立可复用、可回滚、可审计的构建发布链路。

## 1. 流程建议

1. `payment-ops offline smoke`：支付值守离线回归（contract/ops_status/decision_chain/cutover_gate/mock_replay）
2. `lint/test`：静态检查 + 单测
3. `package`：`mvn -DskipTests package`
4. `image build`：构建 `admin/front` 镜像
5. `scan`：镜像安全扫描
6. `push`：推送私有镜像仓库（带 tag）
7. `deploy`：测试环境 -> 预发布 -> 生产
8. `verify`：健康检查 + 关键接口 smoke
9. `rollback`：按上一稳定 tag 回滚

本地可先用以下脚本做最小化发布链路演练：
- `deploy/docker/scripts/preflight.sh`
- `deploy/docker/scripts/release_smoke.sh`（build/up/health/smoke）
- `deploy/docker/scripts/ci_gate.sh`（上线拦截规则聚合脚本）
- `deploy/docker/scripts/db_init.sh --mode strict`（CI 严格模式）
- `deploy/docker/scripts/db_init.sh --mode replay`（运维重放模式）

## 2. 镜像 tag 规则

建议统一：
- `${date}-${git_short_sha}`
- 示例：`20260216-7e18030`

同时保留：
- `latest`（仅测试环境使用）
- `stable`（当前生产稳定版本）

## 3. 必要上线拦截规则

1. 构建失败禁止进入下一步。
2. `docker compose config` 必须通过。
3. `admin/front` 至少一组冒烟接口返回 `code=200`。
4. 安全扫描出现高危漏洞时阻断发布。
5. 预检脚本 `preflight.sh` 必须通过。

## 4. 发布参数外置

统一由环境变量或密钥系统提供：
- DB URL/USER/PASS
- REDIS 密码
- 支付密钥/证书路径
- 告警 webhook

禁止将生产密钥写入仓库。

## 5. 回滚策略

1. 每次发布记录：
- 镜像 tag
- 数据库变更脚本版本
- 发布人和发布时间

2. 回滚顺序：
- 先回滚应用镜像
- 再评估数据库是否需要回退（优先前向修复）

容器环境快速回滚命令：
- `deploy/docker/scripts/down.sh`
- 使用上一稳定 tag 重新 `up`（建议在 compose 中固定镜像 tag）

## 6. 自动触发建议

仓库已提供 GitHub Actions 示例：
- `.github/workflows/crmeb-ci-gate.yml`
- `.github/workflows/crmeb-k8s-render.yml`
- `.github/workflows/crmeb-payment-ops-smoke.yml`

说明：
- `crmeb-ci-gate` 会先调用 `crmeb-payment-ops-smoke`（离线值守回归），通过后再进入 Docker gate。
- `crmeb-ci-gate` 已启用同分支并发互斥（`concurrency`），新任务会自动取消旧任务，避免资源堆积。
- `crmeb-payment-ops-smoke` 会产出统一索引：`crmeb/runtime/payment_ops_smoke_artifact_index.md`，用于快速查看各 smoke 子任务最新 summary/report。
- `crmeb-ci-gate` 会产出统一入口：`crmeb/runtime/ci_gate_artifacts/artifact_index.md`，同页索引 `payment-ops-smoke`、`ci-gate` 日志、失败高亮片段与支付 runtime latest summary。

触发方式：
1. `push` 到 `main/master/develop` 且变更命中 `crmeb/**`
2. `pull_request` 变更命中 `crmeb/**`
3. `workflow_dispatch` 手动触发（支持是否全量构建）
