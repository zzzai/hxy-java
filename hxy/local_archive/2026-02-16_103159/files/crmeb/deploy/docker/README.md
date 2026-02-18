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
    ├── logs.sh
    ├── ps.sh
    └── up.sh
```

## 快速开始

1. 复制环境变量模板并修改密码

```bash
cd /path/to/crmeb/deploy/docker
cp .env.example .env
```

2. 启动全部服务（含构建镜像）

```bash
./scripts/up.sh
```

3. 查看状态与日志

```bash
./scripts/ps.sh
./scripts/logs.sh admin
./scripts/logs.sh front
```

4. 停止服务

```bash
./scripts/down.sh
```

## 重要说明

1. 镜像构建上下文是 `crmeb/` 根目录，已通过 `.dockerignore` 排除 `target/runtime/log` 等内容。
2. 默认 profile 使用 `dev`，数据库与 Redis 通过环境变量覆盖。
3. 首次启动后需要手动导入基础 SQL（如 `sql/Crmeb_v1.4.sql`）与业务补丁 SQL。
4. 日志与数据落盘目录：
   - `runtime/docker/mysql-data`
   - `runtime/docker/redis-data`
   - `runtime/docker/admin-log`
   - `runtime/docker/front-log`
   - `runtime/docker/images`

## 建议的下一步

1. 增加镜像 tag/version 规范（按日期+git短 SHA）。
2. 增加 `docker compose config` 与 `mvn -DskipTests package` 的 CI 门禁。
3. 将敏感配置迁移到密钥管理系统（而非 `.env` 明文）。

