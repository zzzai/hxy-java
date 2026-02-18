# CRMEB K8s Baseline

本目录提供 `admin/front` 的 Kubernetes 基线清单，目标是先完成：
1. 可部署（Deployment/Service/Ingress/HPA）
2. 可配置（ConfigMap/Secret）
3. 可演进（base + overlays）

## 目录

```text
deploy/k8s/
├── base/
│   ├── admin-deployment.yaml
│   ├── admin-hpa.yaml
│   ├── admin-service.yaml
│   ├── configmap.yaml
│   ├── front-deployment.yaml
│   ├── front-hpa.yaml
│   ├── front-service.yaml
│   ├── ingress.yaml
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   └── secret.example.yaml
├── overlays/
│   ├── dev/
│   │   ├── kustomization.yaml
│   │   ├── patch-admin-deployment.yaml
│   │   ├── patch-front-deployment.yaml
│   │   └── patch-ingress.yaml
│   └── prod/
│       ├── kustomization.yaml
│       ├── patch-admin-deployment.yaml
│       ├── patch-front-deployment.yaml
│       └── patch-ingress.yaml
└── scripts/
    ├── apply.sh
    ├── delete.sh
    ├── render.sh
    └── validate.sh
```

## 快速开始

1. 准备 Secret（先复制模板）

```bash
cp deploy/k8s/base/secret.example.yaml deploy/k8s/base/secret.yaml
```

2. 填充 `deploy/k8s/base/secret.yaml` 中的真实配置（DB/Redis）

3. 预检与渲染

```bash
bash deploy/k8s/scripts/validate.sh dev
bash deploy/k8s/scripts/render.sh dev
```

4. 部署

```bash
bash deploy/k8s/scripts/apply.sh dev
```

5. 回滚/删除

```bash
bash deploy/k8s/scripts/delete.sh dev
```

## 生产建议

1. 不要在生产集群使用 `secret.example.yaml`，请改成 External Secrets/密钥系统。
2. 图片存储建议迁移到 OSS/S3，不要依赖容器本地路径。
3. DB/Redis 建议使用托管服务（RDS/托管 Redis），减少状态服务运维成本。
4. Ingress 域名、TLS 证书、镜像 tag 在 `overlays/prod` 管理。
