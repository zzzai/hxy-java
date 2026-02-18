# 容器化数据库初始化清单

目标：确保容器环境中的 MySQL 在第一次启动后可支持当前业务代码运行。

## 1. 连接参数

默认（来自 `.env`）：
- 主机：`127.0.0.1`
- 端口：`${MYSQL_HOST_PORT}`（默认 `33306`）
- 库名：`${MYSQL_DATABASE}`（默认 `crmeb_java`）
- 用户：`${MYSQL_USER}`（默认 `crmeb`）

## 2. 初始化顺序（建议）

1. 导入基础结构
```bash
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < /path/to/crmeb/sql/Crmeb_v1.4.sql
```

2. 导入业务补丁（按你的项目实际顺序）
```bash
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < /root/crmeb-java/hxy/database/database_architecture_fix_v2.sql
mysql -h127.0.0.1 -P33306 -ucrmeb -p'crmeb123' crmeb_java < /root/crmeb-java/hxy/database/database_compliance_guardrails_v1.sql
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

