# 容器化数据库初始化清单

目标：确保容器环境中的 MySQL 在第一次启动后可支持当前业务代码运行。

## 1. 连接参数

默认（来自 `.env`）：
- 主机：`127.0.0.1`
- 端口：`${MYSQL_HOST_PORT}`（默认 `33306`）
- 库名：`${MYSQL_DATABASE}`（默认 `crmeb_java`）
- 用户：`${MYSQL_USER}`（默认 `crmeb`）

## 2. 初始化顺序（建议）

推荐先跑自动脚本（按场景选择模式）：

```bash
cd crmeb/deploy/docker
# CI / 新库：严格模式（遇错即失败）
bash ./scripts/db_init.sh --mode strict

# 运维 / 历史库：重放模式（允许幂等重放）
bash ./scripts/db_init.sh --mode replay

# 如历史环境存在 qrtz_* 小写残留，可执行：
bash ./scripts/db_init.sh --quartz-fix-only --drop-lowercase-duplicates
```

1. 导入基础结构
```bash
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < ../../sql/Crmeb_v1.4.sql
```

2. 导入业务补丁（按你的项目实际顺序）
```bash
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < ../../sql/database_migration_v1.0.sql
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < ../../sql/database_migration_v2.0.sql
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < ../../sql/database_architecture_fix_v2.sql
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < ../../sql/database_compliance_guardrails_v1.sql
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < ../../sql/data_governance_compliance_v1.sql
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < ../../sql/data_governance_rbac_v1.sql
```

3. 核验关键表
```sql
show tables like 'eb_user_consent_record';
show tables like 'eb_data_access_ticket';
show tables like 'eb_data_deletion_ticket';
show tables like 'QRTZ_%';
```

## 3. 常见问题

1. Quartz 报表不存在：
- 检查 `QRTZ_` 前缀表是否存在。

2. 权限不足：
- 确认使用的是 `.env` 中的账号密码；
- 或在 mysql 容器里给业务用户补 `ALL PRIVILEGES`。

3. 字符集问题：
- 确认连接串使用 `characterEncoding=utf-8` 且 MySQL 服务为 `utf8mb4`。
