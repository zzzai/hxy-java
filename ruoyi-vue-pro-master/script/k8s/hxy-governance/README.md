# HXY Kubernetes Governance Baseline

This directory provides a starting governance baseline for HXY multi-service deployments.

## Scope

- Namespace isolation
- Resource quota and limits
- Default deny network policy
- DNS egress allow policy
- PDB and HPA templates

## Apply order

1. `namespaces.yaml`
2. `resourcequota.yaml`
3. `limitrange.yaml`
4. `networkpolicy-default-deny.yaml`
5. `networkpolicy-allow-dns.yaml`
6. service-level `pdb` and `hpa` templates

## Example

```bash
kubectl apply -f script/k8s/hxy-governance/namespaces.yaml
kubectl apply -f script/k8s/hxy-governance/resourcequota.yaml
kubectl apply -f script/k8s/hxy-governance/limitrange.yaml
kubectl apply -f script/k8s/hxy-governance/networkpolicy-default-deny.yaml
kubectl apply -f script/k8s/hxy-governance/networkpolicy-allow-dns.yaml
```

Adjust quotas and labels before production use.
