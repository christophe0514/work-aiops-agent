# 主题业务知识库目录

适用 Agent：OperationQaAgent  
适用对象：主题平台运营、客服、开发支持  
知识库版本：v1.0  
更新日期：2026-05-22

## 使用说明

本目录用于主题业务 Agent 的 RAG 检索验证，内容模拟主题平台真实运营知识库。文档覆盖主题生命周期、审核规则、上架可见性、失败原因、渠道差异、素材规范、价格结算、客服口径和典型案例。

RAG 检索时建议优先根据用户问题识别以下信息：

- 主题 ID、商品 ID、资源包 ID
- 商户 ID、开发者 ID、CP 名称
- 渠道，如主站、会员中心、应用商店、运营活动页
- 终端，如 Android、iOS、HarmonyOS、Web
- 状态，如待审核、审核通过、已上架、上架失败、已下架
- 时间范围，如今天、昨天、近 7 天、某次提交时间

## 文档清单

- `01_theme_lifecycle.md`：主题从创建、提交、审核到上架、下架的完整生命周期。
- `02_theme_status_dictionary.md`：主题状态、审核状态、上架状态、售卖状态字段解释。
- `03_audit_rules.md`：主题审核规则、驳回原因和复审策略。
- `04_publish_visibility.md`：审核通过但前台不可见的排查路径。
- `05_publish_failure_reasons.md`：上架失败、同步失败、渠道失败的常见原因。
- `06_channel_and_terminal_rules.md`：不同渠道和终端的上架差异。
- `07_price_and_settlement.md`：主题定价、限免、会员权益和结算相关规则。
- `08_asset_package_specs.md`：主题素材包、预览图、适配资源规范。
- `09_operation_faq.md`：运营高频问题和标准回答。
- `10_customer_service_scripts.md`：客服对外沟通口径。
- `11_typical_incident_cases.md`：典型异常案例和处理过程。
- `12_query_fields_and_tool_contract.md`：后续主题业务 Tool 查询字段建议。

## 多格式样本

`data/` 目录提供多种格式的 RAG 测试样本，用于验证结构化、半结构化和非结构化内容的混合检索效果。

- `data/theme_status_samples.csv`：主题状态样本，可模拟业务表导出。
- `data/publish_failure_matrix.csv`：上架失败节点和处理归属矩阵。
- `data/audit_reject_codes.json`：审核驳回码、运营口径和处理建议。
- `data/publish_incident_logs.jsonl`：上架异常事件日志样本。
- `data/search_test_queries.tsv`：检索测试问题和期望命中文档。
- `data/operation_runbook.yaml`：运营排查手册，适合测试层级结构检索。
- `data/customer_dialogue_samples.txt`：客服和用户对话样本。
- `data/mock_theme_records.sql`：模拟主题业务记录，适合后续 Tool 或数据库样例。

## 推荐检索策略

当用户问“为什么上架失败”，优先检索：

1. `05_publish_failure_reasons.md`
2. `03_audit_rules.md`
3. `06_channel_and_terminal_rules.md`

当用户问“审核通过了为什么看不到”，优先检索：

1. `04_publish_visibility.md`
2. `02_theme_status_dictionary.md`
3. `06_channel_and_terminal_rules.md`

当用户问“这个主题现在是什么状态”，优先检索：

1. `02_theme_status_dictionary.md`
2. `01_theme_lifecycle.md`
3. `12_query_fields_and_tool_contract.md`
