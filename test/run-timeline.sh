#!/usr/bin/env bash
# 测量 .bin（timelineMap）加载后的 Java 常驻内存增量 + 条目数。
# 用法：./test/run-timeline.sh
#
# 注：.bin 是懒加载，触发需要 DataProvider 实例（会先加载 .properties），
#     因此本脚本同时输出 .properties 和 .bin 两阶段的内存增量。
set -euo pipefail

cd "$(dirname "$0")/.."

echo ">>> 构建 Docker 镜像..."
docker build -f test/Dockerfile.probe -t cidv-probe . >/dev/null

echo ">>> 测量 .properties + .bin 加载内存"
echo
docker run --rm cidv-probe all
