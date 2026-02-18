# CRMEB Java 后端容器化基线

本目录提供 `crmeb-admin` + `crmeb-front` 的容器化部署基础设施，目标是先统一运行方式，再逐步接入 CI/CD。

## 目录结构

```text
deploy/docker/
├── .env.example
├── docker-compose.backend.yml
├── Dockerfile.admin
├── Dockerfile.front
└── scripts/
    ├── build.sh
    ├── down.sh
    ├── db_init.sh
    ├── ci_gate.sh
    ├── logs.sh
    ├── ps.sh
    ├── preflight.sh
    ├── release_smoke.sh
    └── up.sh
```

## 快速开始

1. 复制环境变量模板并修改密码

```bash
cd crmeb/deploy/docker
cp .env.example .env
```

2. 启动全部服务（含构建镜像）

```bash
bash ./scripts/up.sh
```

3. 查看状态与日志

```bash
bash ./scripts/ps.sh
bash ./scripts/logs.sh admin
bash ./scripts/logs.sh front
```

4. 停止服务

```bash
bash ./scripts/down.sh
```

## 一键发布联调（推荐）

```bash
bash ./scripts/release_smoke.sh
```

该脚本会按顺序执行：
1. `build`（可用 `--skip-build` 跳过）
2. `up`
3. admin/front 健康检查
4. 自动登录并运行 `shell/data_governance_smoke.sh`（可用 `--skip-smoke` 跳过）

## 初始化数据库（推荐）

```bash
# CI / 新库：严格模式（遇错即失败）
bash ./scripts/db_init.sh --mode strict

# 运维 / 历史库：重放模式（允许幂等重放）
bash ./scripts/db_init.sh --mode replay
```

常用参数：
1. `--dry-run`：只打印将执行的 SQL，不实际导入
2. `--mode strict`：严格模式，遇错即失败（推荐 CI）
3. `--mode replay`：重放模式，允许幂等重放（推荐运维）
4. `--force`：兼容旧参数，等价于 `--mode replay`
5. `--skip-quartz-fix`：跳过 `qrtz_* -> QRTZ_*` 自动修复
6. `--quartz-fix-only`：仅执行 Quartz 小写表修复，不导入 SQL
7. `--drop-lowercase-duplicates`：当大写表已存在时，删除同名小写 Quartz 表

## CI 上线拦截规则（本地模拟）

```bash
# 快速上线拦截规则（跳过构建）
bash ./scripts/ci_gate.sh

# 全量上线拦截规则（含构建）
bash ./scripts/ci_gate.sh --full

# 全量上线拦截规则 + 初始化数据库（严格模式，推荐 CI）
bash ./scripts/ci_gate.sh --full --init-db --db-mode strict

# 运维重放模式（允许幂等重放）
bash ./scripts/ci_gate.sh --init-db --db-mode replay
```

## K8s 渲染校验 CI

仓库已提供 K8s 仅渲染校验工作流（不部署集群）：
- `.github/workflows/crmeb-k8s-render.yml`

## 重要说明

1. 镜像构建上下文是 `crmeb/` 根目录，已通过 `.dockerignore` 排除 `target/runtime/log` 等内容。
2. 默认 profile 使用 `dev`，数据库与 Redis 通过环境变量覆盖。
3. 首次启动后优先使用 `db_init.sh` 导入仓库内版本化 SQL，避免手工执行顺序错误。
4. 日志与数据落盘目录：
   - `runtime/docker/mysql-data`
   - `runtime/docker/redis-data`
   - `runtime/docker/admin-log`
   - `runtime/docker/front-log`
   - `runtime/docker/images`

## 建议的下一步

1. 增加镜像 tag/version 规范（按日期+git短 SHA）。
2. 增加 `docker compose config` 与 `mvn -DskipTests package` 的 CI 上线拦截规则。
3. 将敏感配置迁移到密钥管理系统（而非 `.env` 明文）。
