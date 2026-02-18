# database 目录说明

数据库脚本基线。

## 脚本清单

1. `database_migration_v1.0.sql`（基础表）
2. `database_migration_v2.0.sql`（扩展表）
3. `database_architecture_fix_v2.sql`（生产补丁）
4. `database_compliance_guardrails_v1.sql`（授权/审计/删除/标签上线拦截规则补丁）

## 推荐执行顺序

```bash
mysql -u root -p crmeb_java < /root/crmeb-java/hxy/database/database_migration_v1.0.sql
mysql -u root -p crmeb_java < /root/crmeb-java/hxy/database/database_migration_v2.0.sql
mysql -u root -p crmeb_java < /root/crmeb-java/hxy/database/database_architecture_fix_v2.sql
mysql -u root -p crmeb_java < /root/crmeb-java/hxy/database/database_compliance_guardrails_v1.sql
```
