# CI/CD 基线草案（容器化）

目标：建立可复用、可回滚、可审计的构建发布链路。

## 1. 流程建议

1. `lint/test`：静态检查 + 单测
2. `package`：`mvn -DskipTests package`
3. `image build`：构建 `admin/front` 镜像
4. `scan`：镜像安全扫描
5. `push`：推送私有镜像仓库（带 tag）
6. `deploy`：测试环境 -> 预发布 -> 生产
7. `verify`：健康检查 + 关键接口 smoke
8. `rollback`：按上一稳定 tag 回滚

## 2. 镜像 tag 规则

建议统一：
- `${date}-${git_short_sha}`
- 示例：`20260216-7e18030`

同时保留：
- `latest`（仅测试环境使用）
- `stable`（当前生产稳定版本）

## 3. 必要门禁

1. 构建失败禁止进入下一步。
2. `docker compose config` 必须通过。
3. `admin/front` 至少一组冒烟接口返回 `code=200`。
4. 安全扫描出现高危漏洞时阻断发布。

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

