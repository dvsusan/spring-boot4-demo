#!/usr/bin/env bash
set -euo pipefail

namespace="${NAMESPACE:-spring-boot4-demo}"
deployment="${DEPLOYMENT:-spring-boot4-demo}"
revision="${1:-}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl 不存在，无法回滚" >&2
  exit 2
fi

if [[ -n "$revision" ]]; then
  kubectl -n "$namespace" rollout undo \
    "deployment/$deployment" \
    "--to-revision=$revision"
else
  kubectl -n "$namespace" rollout undo "deployment/$deployment"
fi

kubectl -n "$namespace" rollout status \
  "deployment/$deployment" \
  --timeout=180s
