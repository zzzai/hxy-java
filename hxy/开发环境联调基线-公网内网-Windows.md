# 开发环境联调基线（公网/内网 + Windows）

- 日期：2026-02-18
- 目标：让 Windows11（HBuilderX/微信开发者工具）与内网 CRMEB 环境稳定联调。

## 1. 当前拓扑（已确认）

1. 公网 Nginx：`https://hexiaoyue.com`（含 `api.hexiaoyue.com`、`admin.hexiaoyue.com` 证书与反代）。
2. 内网开发服务器：运行 CRMEB Java、Docker MySQL、支付脚本。
3. FRP：公网机 -> 内网机端口转发（HTTP/HTTPS 已通）。
4. Windows11：通过 FTP 同步小程序代码，HBuilderX 运行到微信开发者工具，已完成 0.01 元真实支付。

## 2. Windows 是否可直连内网 MySQL？

可以，推荐两种方式（优先顺序如下）：

1. 优先：SSH 隧道（最安全）
2. 次选：FRP TCP 暴露 MySQL（必须加白名单和强口令）

## 3. 方式A：SSH 隧道（推荐）

在 Windows PowerShell 执行：

```powershell
ssh -N -L 33306:127.0.0.1:33306 root@<你的公网机IP或域名>
```

连接参数（Navicat / DBeaver / Workbench）：

- Host: `127.0.0.1`
- Port: `33306`
- DB: `crmeb_java`
- User: `crmeb`
- Password: `crmeb123`

说明：本地 `33306` 通过隧道映射到内网 Docker MySQL。

## 4. 方式B：FRP 直通 MySQL（可用但风险更高）

`frpc.ini` 增加：

```ini
[crmeb_mysql]
type = tcp
local_ip = 127.0.0.1
local_port = 33306
remote_port = 33306
```

然后在公网机安全组/防火墙仅放行你的办公出口 IP，不要全网开放 33306。

## 5. 本项目支付脚本推荐数据库连接

统一使用 `MYSQL_DEFAULTS_FILE`，并指向 Docker MySQL：

```ini
# /root/.my.cnf
[client]
user=crmeb
password=crmeb123
host=127.0.0.1
port=33306
```

> 注意：脚本已经修正为优先尊重 `MYSQL_DEFAULTS_FILE`（不再强制覆盖到 `3306`）。

## 6. 联调最小检查命令

```bash
cd /root/crmeb-java/crmeb_java/crmeb
MYSQL_DEFAULTS_FILE=/root/.my.cnf ./shell/payment_order_locator.sh --order-no 4200003006202602174039197150
MYSQL_DEFAULTS_FILE=/root/.my.cnf ./shell/payment_fullchain_drill.sh --order-no 4200003006202602174039197150
```

