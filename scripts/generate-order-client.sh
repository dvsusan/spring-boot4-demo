#!/usr/bin/env bash
set -euo pipefail

# 脚本可以从任意目录执行，统一以仓库根目录作为相对路径基准。
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
spec_file="$repo_root/openapi/order-api.yaml"
output_dir="$repo_root/target/generated-order-client"

# npm 缓存放到临时目录，避免污染仓库，也绕开用户级缓存权限问题。
export npm_config_cache="${TMPDIR:-/tmp}/openapi-generator-npm-cache"

# 在仓库根目录运行，让 openapitools.json 的生成器版本固定生效。
cd "$repo_root"

npx --yes @openapitools/openapi-generator-cli@2.25.2 generate \
  --input-spec "$spec_file" \
  --generator-name typescript-fetch \
  --output "$output_dir" \
  --additional-properties=npmName=order-api-client,npmVersion=1.0.0,supportsES6=true

echo "Generated TypeScript client: $output_dir"
