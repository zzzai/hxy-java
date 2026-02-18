# FRP + Nginx 接入说明（hexiaoyue.com）

适用场景：
- 公网服务器：`115.190.245.14`，运行 `nginx + frps`
- 内网开发机：运行 `crmeb-admin(20500)`、`crmeb-front(20510)`、`frpc`
- DNS：`api.hexiaoyue.com` / `admin.hexiaoyue.com` 指向公网 IP

## 1. 公网机配置 frps

参考文件：`deploy/frp/frps.toml.example`

建议：
- `auth.token` 使用高强度随机串
- 开启 `transport.tls.force = true`
- 防火墙只放行：`80/443/7000`

## 2. 内网机配置 frpc

参考文件：`deploy/frp/frpc.toml.example`

端口映射约定：
- `remotePort 32000` -> 内网 `20500`（admin）
- `remotePort 32010` -> 内网 `20510`（front）

## 3. 公网 Nginx 反向代理

参考文件：`deploy/docker/nginx.hexiaoyue.conf`

域名回源：
- `admin.hexiaoyue.com` -> `127.0.0.1:32000`
- `api.hexiaoyue.com` -> `127.0.0.1:32010`

说明：
- 公网 Nginx 不直连内网机器，只连本机 frps 暴露端口
- 证书需覆盖 `api.hexiaoyue.com` 与 `admin.hexiaoyue.com`

## 4. 小程序支付必做项

1. 小程序后台“服务器域名”加入 `https://api.hexiaoyue.com`
2. 后端 `eb_system_config.api_url` 设为 `https://api.hexiaoyue.com`
3. `crmeb_java/app/config/app.js` 的 `domain` 设为 `https://api.hexiaoyue.com`

## 5. 联通性检查

公网机执行：

```bash
curl -I https://api.hexiaoyue.com
curl -I https://admin.hexiaoyue.com
```

内网机执行：

```bash
ss -lntp | rg '20500|20510'
```

若 `curl` 不是 CRMEB 服务而是主站页面，说明 Nginx 的 `server_name` 或证书/站点优先级未配置正确。
