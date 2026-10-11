# ===========================================================================
# Chinese ID Validator — 项目根 Makefile
#
# 数据文件生成：
#   make data              从 JSON 重新生成 .properties 和 .bin
#   make data-properties   address_code.json -> address_code.properties
#   make data-timeline     address_code_timeline.json -> address_code_timeline.bin
# ===========================================================================

DATA_GEN := cn.chineseid.validator.tools.DataFileGenerator
CP_FILE  := target/cp.txt

.DEFAULT_GOAL := help
.PHONY: data data-properties data-timeline data-prepare help

# ---- 数据生成 ------------------------------------------------------------

data: data-properties data-timeline ## 从 JSON 重新生成 .properties 和 .bin

data-properties: data-prepare ## address_code.json -> address_code.properties
	java -cp "target/test-classes:target/classes:$$(cat $(CP_FILE))" $(DATA_GEN) properties

data-timeline: data-prepare ## address_code_timeline.json -> address_code_timeline.bin
	java -cp "target/test-classes:target/classes:$$(cat $(CP_FILE))" $(DATA_GEN) timeline

# 编译生成器 + 导出 classpath（含 gson）
data-prepare:
	@mvn -q test-compile -DskipTests
	@mvn -q dependency:build-classpath -Dmdep.outputFile=$(CP_FILE)

# ---- 帮助 ----------------------------------------------------------------

help: ## 显示帮助
	@printf 'Chinese ID Validator\n\n'
	@printf '用法: make <target>\n\nTargets:\n'
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
