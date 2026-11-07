#!/usr/bin/env bash
set -euo pipefail

image="${1:?用法: scripts/deploy-k8s.sh <image:tag>}"
namespace="${NAMESPACE:-spring-boot4-demo}"
deployment="${DEPLOYMENT:-spring-boot4-demo}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl 不存在，无法部署" >&2
  exit 2
fi

# 先应用 Service、探针、资源限制等稳定配置，再替换具体镜像版本。
kubectl apply -k deploy/k8s
kubectl -n "$namespace" set image \
  "deployment/$deployment" \
  "app=$image"

# 新版本没有在时限内就绪时自动回到上一代 ReplicaSet。
if ! kubectl -n "$namespace" rollout status \
  "deployment/$deployment" \
  --timeout=180s; then
  echo "新版本发布失败，开始自动回滚" >&2
  kubectl -n "$namespace" rollout undo "deployment/$deployment"
  kubectl -n "$namespace" rollout status \
    "deployment/$deployment" \
    --timeout=180s
  exit 1
fi
