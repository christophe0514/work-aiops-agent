# 主题业务 Tool 查询字段建议

适用场景：为后续 OperationQaAgent 接入主题业务查询 Tool 提供字段参考  
关键词：Tool、查询字段、主题状态、审核状态、上架状态、RAG 结合工具

## 设计目标

主题业务 Agent 使用 RAG 回答流程类、规则类问题，但对于具体主题状态、审核状态、上架失败原因，必须调用业务 Tool 查询实时数据。

RAG 适合回答：

- 状态含义。
- 流程解释。
- 规则说明。
- 排查路径。
- 客服口径。

Tool 适合回答：

- 某主题当前状态。
- 某次审核驳回原因。
- 某渠道是否同步成功。
- 某主题是否可售。
- 某个用户为什么看不到或买不了。

## 查询主题基础信息

建议接口：`getThemeBasicInfo`

入参：

- themeId
- themeName，可选
- merchantId，可选

出参：

- themeId
- themeName
- merchantId
- merchantName
- developerId
- themeVersion
- terminalTypes
- category
- tags
- createdTime
- updatedTime

## 查询主题状态

建议接口：`getThemeStatus`

入参：

- themeId
- terminalType，可选
- channel，可选

出参：

- themeStatus
- auditStatus
- publishStatus
- saleStatus
- riskStatus
- currentVersion
- approvedVersion
- lastUpdatedTime

## 查询审核记录

建议接口：`getThemeAuditRecords`

入参：

- themeId
- version，可选

出参：

- auditId
- auditType
- auditStatus
- auditStage
- submitTime
- auditTime
- reviewer
- rejectCode
- rejectReason
- reviewerComment
- requiredMaterials

## 查询上架记录

建议接口：`getThemePublishRecords`

入参：

- themeId
- channel，可选
- terminalType，可选

出参：

- publishTaskId
- channel
- terminalType
- publishStatus
- publishStartTime
- publishEndTime
- scheduledTime
- publishedTime
- failedNode
- failedReason
- retryable
- lastRetryTime

## 查询渠道可见性

建议接口：`checkThemeVisibility`

入参：

- themeId
- channel
- terminalType
- clientVersion，可选
- userId，可选
- cityCode，可选

出参：

- visible
- reasonCode
- reasonMessage
- matchedChannel
- matchedTerminal
- matchedUserGroup
- minClientVersion
- cacheStatus
- indexStatus

## 查询售卖状态

建议接口：`getThemeSaleStatus`

入参：

- themeId
- channel，可选
- userId，可选

出参：

- saleStatus
- saleType
- originalPrice
- currentPrice
- currency
- priceStartTime
- priceEndTime
- memberOnly
- limitedFree
- stockStatus
- purchaseEnabled
- purchaseDisabledReason

## Agent 回答建议

当用户没有提供主题 ID 时：

> 请提供主题 ID 或主题名称，我才能查询具体状态。如果你只是想了解通用原因，我可以先说明常见排查方向。

当 Tool 查询到具体失败原因时，回答应结合 RAG 规则解释：

> 该主题上架失败节点是商品中心同步，失败原因是价格配置为空。根据主题上架规则，付费主题必须先配置有效价格，商品中心才能生成可售商品。建议补充价格配置后重新触发上架。

当 Tool 查询不到主题时：

> 当前没有查询到该主题记录。请确认主题 ID 是否正确，或补充商户 ID、主题名称后重新查询。

## 推荐 reasonCode

- `NOT_APPROVED`：当前版本未审核通过。
- `NO_CHANNEL_CONFIG`：未配置目标渠道。
- `NOT_IN_PUBLISH_TIME`：不在上架时间范围内。
- `TERMINAL_NOT_SUPPORTED`：终端不支持。
- `CLIENT_VERSION_TOO_LOW`：客户端版本过低。
- `USER_GROUP_NOT_MATCHED`：用户不在定向人群。
- `REGION_NOT_MATCHED`：地域不匹配。
- `SEARCH_INDEX_PENDING`：搜索索引待同步。
- `CHANNEL_SYNC_FAILED`：渠道同步失败。
- `CDN_DISTRIBUTION_FAILED`：资源分发失败。
- `PRICE_NOT_EFFECTIVE`：价格未生效。
- `MERCHANT_DISABLED`：商户状态异常。
- `RISK_BLOCKED`：风控拦截。

