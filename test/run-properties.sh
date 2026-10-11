#!/usr/bin/env bash
# 测量 .properties（addressCodeMap）加载后的 Java 常驻内存增量 + 条目数。
# 用法：./test/run-properties.sh
set -euo pipefail

cd "$(dirname "$0")/.."

echo ">>> 构建 Docker 镜像..."
docker build -f test/Dockerfile.probe -t cidv-probe . >/dev/null

echo ">>> 测量 .properties 加载内存"
echo
docker run --rm cidv-probe properties
