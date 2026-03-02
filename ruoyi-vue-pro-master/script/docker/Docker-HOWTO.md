# Docker Build & Up

目标: 快速部署体验系统，帮助了解系统之间的依赖关系。
依赖：docker compose v2，删除`name: yudao-system`，降低`version`版本为`3.3`以下，支持`docker-compose`。

## 功能文件列表

```text
.
├── Docker-HOWTO.md                 
├── docker-compose.yml              
├── docker.env                      <-- 提供docker-compose环境变量配置
├── yudao-server
│   └── Dockerfile
└── hxy-ui-admin
    ├── .dockerignore
    ├── Dockerfile
    └── nginx.conf                  <-- 提供基础配置，gzip压缩、api转发
```

## 构建 jar 包

```shell
# 创建maven缓存volume
docker volume create --name yudao-maven-repo

docker run -it --rm --name yudao-maven \
    -v yudao-maven-repo:/root/.m2 \
    -v $PWD:/usr/src/mymaven \
    -w /usr/src/mymaven \
    maven mvn clean install package '-Dmaven.test.skip=true'
```

## 构建启动服务

```shell
docker compose --env-file docker.env up -d
```

首次运行会自动构建容器。可以通过`docker compose build [service]`来手动构建所有或某个docker镜像

`--env-file docker.env`为可选参数，只是展示了通过`.env`文件配置容器启动的环境变量，`docker-compose.yml`本身已经提供足够的默认参数来正常运行系统。

## 服务器的宿主机端口映射

- admin ui: `http://localhost:${ADMIN_HOST_PORT}`（默认 `18080`）
- api server: `http://localhost:${SERVER_HOST_PORT}`（默认 `14880`）
- mysql: `root/123456`, host port `${MYSQL_HOST_PORT}`（默认 `13306`）
- redis: host port `${REDIS_HOST_PORT}`（默认 `16379`）

说明：默认值已避开常见的 `3306/6379/8080/48080` 占用，支持与现有 CRMEB 容器并行运行。

## CRMEB 兼容层灰度阶段切换

`docker.env` 已支持以下变量：

- `CRMEB_COMPAT_STAGE=full-compat|payment-core-only|disabled`
- `SPRING_PROFILES_ACTIVE=local,crmeb-gray|crmeb-fast-track|crmeb-disabled`

建议使用脚本一键切换并验收：

```bash
# 切到支付核心收敛阶段并重启 server，再执行阶段门禁
bash ../dev/switch_crmeb_stage.sh \
  --stage payment-core-only \
  --base-url https://api.hexiaoyue.com \
  --deploy-mode docker-compose \
  --apply \
  --restart-server
```

## 数据卷命名迁移（yudao-system_* -> hxy-platform_*）

首次切换新卷名时，先迁移旧卷数据，避免 MySQL/Redis 丢数据：

```bash
# 先看计划，不执行
bash migrate_volume_names_to_hxy.sh --dry-run

# 正式迁移（会先备份，再拷贝）
bash migrate_volume_names_to_hxy.sh
```

如旧卷仍被运行中容器占用，脚本会直接退出。先停服务后再执行，或明确使用 `--allow-live`。
