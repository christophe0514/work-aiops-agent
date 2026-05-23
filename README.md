# 主题平台 AIOps Agent

## LLM AgentRouter 多智能体路由

当前对话入口已经升级为多智能体路由链路：

```text
用户提问
  -> POST /chat
  -> ChatService 调用 AgentRouter
  -> agentRouterClient 使用大模型输出结构化路由结果
  -> AgentRegistry 获取目标业务 Agent
  -> OperationQaAgent / TicketAgent / OpsAgent 处理问题
  -> ChatEventVO 通过 SSE 返回路由事件和回答内容
```

`AgentRouter` 只负责意图识别和任务分流，不直接回答业务问题。当前业务 Agent 包括：

- `OperationQaAgent`：主题业务运营答疑、RAG 知识库检索、主题业务 Tool 查询。
- `TicketAgent`：工单辅助流转，占位能力已接入，后续扩展工单摘要、优先级和分派建议。
- `OpsAgent`：运维排障，已接入 Mock 诊断 Tool，支持服务健康、告警、流水线和 Trace 摘要联调。

SSE 事件体仍使用 `ChatEventVO`，事件类型包括：

- `001`：回答内容
- `002`：流式结束
- `003`：错误信息
- `004`：路由信息

前端对话页已支持展示当前回答来源 Agent，便于联调 LLM 路由结果。

## 运维排障 Agent

`OpsAgent` 已接入独立 `opsAgentClient` 和运维诊断 Tool。当前底层使用 Mock 数据，后续可以替换为真实监控、日志、Trace 或流水线平台的 HTTP 客户端。

当前 Tool 包括：

- `queryServiceHealth(serviceName, env)`：查询服务健康状态、错误率、P95 耗时、CPU、内存和实例状态。
- `queryRecentAlerts(serviceName, timeRange)`：查询近期告警摘要。
- `queryPipelineStatus(appName, env)`：查询最近一次发布流水线状态。
- `queryTraceSummary(traceId)`：查询 Trace 调用链摘要。

Mock 调试接口：

```http
GET /mock/ops/service-health?serviceName=theme-publish-service&env=prod
GET /mock/ops/alerts?serviceName=theme-publish-service&timeRange=最近30分钟
GET /mock/ops/pipeline?appName=theme-publish-service&env=prod
GET /mock/ops/trace?traceId=trace-demo-001
```

适合联调的问题示例：

- `theme-publish-service 在 prod 环境接口超时，帮我排查`
- `最近30分钟 theme-publish-service 有哪些告警？`
- `theme-publish-service 发布流水线失败了，看下原因`
- `traceId 为 trace-demo-001 的接口为什么报错？`

## Redis Stack RAG 落地说明

当前项目已接入 Redis Stack 作为主题业务知识库的向量存储，核心链路如下：

```text
docs/rag/theme-business/*
  -> KnowledgeBaseService 扫描多格式文件
  -> 按 chunk-size / chunk-overlap 切片
  -> DashScope text-embedding-v3 生成向量
  -> Spring AI Redis VectorStore 写入 Redis Stack
  -> ChatService 提问前先检索知识库
  -> 命中则基于知识片段回答，未命中则转业务接口人
```

### 相关配置

```yaml
spring:
  ai:
    dashscope:
      embedding:
        options:
          model: text-embedding-v3
          dimensions: 1024
    vectorstore:
      redis:
        initialize-schema: true
        index-name: spring-ai-index
        prefix: "embedding:"
  data:
    redis:
      host: 192.168.150.101
      password: 123456
      port: 16379
      client-type: jedis

aiops:
  rag:
    knowledge-base-path: docs/rag/theme-business
    biz-domain: theme-business
    fallback-owner-employee-no: THEME_OPS_OWNER
    top-k: 5
    similarity-threshold: 0.65
    chunk-size: 900
    chunk-overlap: 120
```

### 导入知识库

启动后调用：

```bash
curl -X POST "http://localhost:18080/admin/kb/ingest/theme-business"
```

接口会扫描 `docs/rag/theme-business` 下的以下文件类型：

```text
.md .txt .csv .tsv .json .jsonl .yaml .yml .sql
```

每个文件会被拆成多个知识片段，并写入 Redis Stack 向量索引。

### 调试检索

```bash
curl "http://localhost:18080/admin/kb/search?query=主题审核通过了为什么前台看不到"
```

返回内容包含：

- `content`：命中的知识片段
- `score`：相似度分数
- `metadata.title`：文档标题
- `metadata.sourcePath`：来源路径
- `metadata.chunkIndex`：片段序号

### Chat 使用方式

`POST /chat` 现在会先检索 Redis 向量库：

- 如果命中知识片段：把片段注入本轮 prompt，让 `operationQaAgentClient` 只根据知识库回答。
- 如果没有命中：直接返回“当前知识库没有明确说明”，并提示联系 `aiops.rag.fallback-owner-employee-no` 配置的业务接口人。
- 如果 Redis Stack、向量索引或 Embedding 配置异常：返回 `ChatEventVO` 的 `003` 错误事件。

第一版采用“文档目录即知识源”的轻量方案，方便本地调试。后续如果要做后台管理，可以再补 `kb_document`、`kb_document_chunk` 等业务表，用来管理文档版本、启停状态、所属业务域和接口人。

### 文件级管理接口

为了避免每次都全量导入，后端提供了文件级调试接口：

```http
GET /admin/kb/theme-business/files
```

列出当前知识库目录下的文件、标题、类型、大小、修改时间和预计切片数。

```http
POST /admin/kb/ingest/theme-business/file?path=03_audit_rules.md
```

只导入或重建一个文件。重建前会按稳定 chunk id 删除该文件旧向量，再写入新向量。

```http
POST /admin/kb/delete/theme-business/file?path=03_audit_rules.md
```

删除某个文件对应的 Redis 向量数据，不删除本地知识库原文。

前端 `frontend/` 已增加“知识库管理”页面，可以直接完成文件刷新、单文件导入、单文件删除向量、全量导入和检索调试。

## Agent Trace 观测体系

项目已新增 Agent Trace 事件流水，用于记录一次对话从路由到 Agent 执行的关键链路。每次调用 `/chat` 时后端会生成一个 `traceId`，并通过路由 SSE 事件返回给前端或调试方。

当前已记录的事件包括：

- `trace_start`：一次对话链路开始，记录 userId、chatId、userMessage。
- `route_start` / `route_result`：记录 AgentRouter 的路由输入和结构化路由结果。
- `knowledge_search`：记录 RAG 检索命中的知识片段、score 和 metadata。
- `theme_data_query_detected`：记录是否识别为具体主题数据查询。
- `user_prompt_built`：记录注入 RAG / Tool 使用规则后的本轮 Prompt。
- `*_call` / `*_result`：记录主题业务 Tool、运维诊断 Tool 的调用参数、返回结果和耗时。
- `agent_complete` / `error`：记录 Agent 执行完成、异常原因和基础耗时。

建表脚本：

```bash
docs/sql/agent_trace_log.sql
```

查询接口：

```http
GET /admin/agent-traces/{traceId}
```

示例链路：

```text
POST /chat
  -> SSE eventType=004 route event 中获取 traceId
  -> GET /admin/agent-traces/{traceId}
  -> 查看 router / rag / prompt / tool / agent 全链路事件
```

Token 字段 `prompt_tokens`、`completion_tokens`、`total_tokens` 已在表结构中预留。后续如果接入模型 usage metadata 或自定义 Advisor，可以直接补齐 Token 消耗统计，并进一步接入 Prometheus / Grafana 做趋势监控。

## 主题业务 Tool

当前已为 `operationQaAgentClient` 接入主题业务 Tool。它用于处理带具体主题 ID 的业务查询，例如：

- 查询主题当前状态
- 查询审核状态
- 查询上架状态
- 查询前台不可见原因
- 查询最近驳回原因

### 代码结构

```text
ThemeBusinessTools
  -> ThemeBusinessClient
  -> MockThemeBusinessClient
```

- `ThemeBusinessTools`：暴露给大模型调用的 Spring AI Tool。
- `ThemeBusinessClient`：主题业务服务客户端抽象，后续真实 HTTP 对接只需要替换这个接口实现。
- `MockThemeBusinessClient`：当前模拟主题业务服务返回。

### 当前 Tool

```text
queryThemeBusinessSnapshot(themeId)
```

返回主题业务快照，包括：

- 主题主状态
- 审核状态
- 上架状态
- 可见渠道
- 当前原因说明
- 运营处理建议
- 最近审核记录

### Mock 调试接口

开发阶段可以不经过大模型，直接查看模拟业务数据：

```bash
curl "http://localhost:18080/mock/theme-business/snapshot?themeId=theme_10001"
```

Mock 规则：

- `themeId` 以 `2` 结尾：模拟审核驳回
- `themeId` 以 `3` 结尾：模拟上架失败
- `themeId` 以 `404` 结尾：模拟未查询到主题
- 其他：模拟已上架

后续对接真实主题业务服务时，建议新增 `HttpThemeBusinessClient`，通过配置切换 Mock/HTTP 实现，保持 `ThemeBusinessTools` 不变。
本项目用于构建主题平台的智能运维 Agent，面向运营、客服、审核和开发支持人员，提供主题业务答疑、工单辅助流转、运维排障等能力。

当前重点建设的是「主题业务 Agent」，对应 `operationQaAgentClient`。它负责基于系统提示词和会话上下文回答主题创作者平台的运营规则、平台功能、业务流程和常见问题。工单 Agent 和运维排障 Agent 已在代码结构中预留，后续继续扩展。

## 当前能力

- 通过 `/chat` 提供 SSE 流式对话接口。
- 对接 DashScope `qwen-plus` 模型。
- 从 MySQL 的 `ai_prompt_config` 表加载 Agent 系统提示词。
- 使用 Spring AI `MessageChatMemoryAdvisor` 接入会话记忆。
- 使用 Redis 持久化聊天上下文。
- 使用 `ChatEventVO` 作为规整的 SSE 返回体。
- 内置 Vue 3 + TypeScript 前端联调台。
- 提供 `docs/rag/theme-business/` 主题业务 RAG 样本文档和多格式数据。

## Agent 规划

### 主题业务 Agent

当前优先实现的业务 Agent，对应实现类：`OperationQaAgent`，底层继续使用 `operationQaAgentClient`。

负责：

- 主题创作者平台功能使用答疑
- 主题上传、审核、修改重提、上架、下架、资源同步流程说明
- 审核规则、上架条件、主题状态含义、驳回原因说明
- 运营、审核、客服常见问题答疑

当前系统提示词从 `ai_prompt_config` 中按以下 key 读取：

```text
agent_name = OperationQaAgent
prompt_type = system
is_enabled = 1
```

### 工单 Agent

代码中已接入 `TicketAgent` 占位实现，当前用于验证 AgentRouter 分流链路。

规划能力：

- 识别用户问题类型
- 判断工单优先级
- 推荐处理部门或开发组
- 生成工单摘要
- 给出处理建议

### 运维排障 Agent

代码中已接入 `OpsAgent` 占位实现，当前用于验证 AgentRouter 分流链路。

规划能力：

- 分析系统异常
- 查询日志、告警、流水线状态
- 判断接口、服务、数据库、中间件等异常方向
- 生成排障建议

## 对话链路

```text
用户提问
  -> POST /chat
  -> ChatController 接收 userMessage、userId、chatId
  -> ChatService 调用 AgentRouter
  -> agentRouterClient 使用 LLM 输出结构化路由结果
  -> AgentRegistry 根据 agentCode 获取目标业务 Agent
  -> OperationQaAgent / TicketAgent / OpsAgent 处理问题
  -> ChatEventVO 通过 text/event-stream 返回路由事件和回答内容
  -> OperationQaAgent 通过 MessageChatMemoryAdvisor 复用 Redis 历史上下文
  -> RedisChatMemoryRepository 保存最近 20 条上下文
```

## 技术栈

- Java 17
- Spring Boot 3.2.6
- Spring AI 1.0.0
- Spring AI Alibaba DashScope
- Spring WebFlux SSE
- MySQL
- MyBatis-Plus
- Redis
- Hutool
- Maven
- Vue 3 + TypeScript + Vite

## 核心目录

```text
src/main/java/com/example
+-- WorkAiOpsAgentApplication.java
+-- agent/core
|   +-- agent
|   |   +-- Agent.java
|   |   +-- AgentCode.java
|   |   +-- AgentRegistry.java
|   |   +-- AbstractAgent.java
|   |   +-- impl/OperationQaAgent.java
|   |   +-- impl/TicketAgent.java
|   |   +-- impl/OpsAgent.java
|   +-- config
|   |   +-- AiConfig.java
|   |   +-- ChatClientConfig.java
|   |   +-- PromptManager.java
|   +-- controller
|   |   +-- ChatController.java
|   +-- domain
|   |   +-- dto/ChatMessageDTO.java
|   |   +-- entity/AiPromptConfig.java
|   |   +-- vo/ChatEventVO.java
|   +-- mapper
|   |   +-- AiPromptConfigMapper.java
|   +-- memory
|   |   +-- RedisChatMemoryRepository.java
|   |   +-- MessageUtil.java
|   |   +-- dto/MemoryMessageDTO.java
|   +-- router
|   |   +-- AgentRouter.java
|   |   +-- impl/LlmAgentRouter.java
|   +-- service
|       +-- AiPromptService.java
|       +-- ChatService.java
|       +-- impl
|           +-- AiPromptServiceImpl.java
|           +-- ChatServiceImpl.java
+-- agent/rag
|   +-- config/RagProperties.java
|   +-- controller/KnowledgeBaseController.java
|   +-- domain/vo
|   |   +-- KbDocumentFileVO.java
|   |   +-- KbIngestResultVO.java
|   |   +-- KbSearchResultVO.java
|   +-- service
|       +-- KnowledgeBaseService.java
|       +-- impl/KnowledgeBaseServiceImpl.java
+-- agent/tools
    +-- theme
        +-- ThemeBusinessTools.java
        +-- controller/ThemeBusinessMockController.java
        +-- client
        |   +-- ThemeBusinessClient.java
        |   +-- impl/MockThemeBusinessClient.java
        +-- domain/vo
            +-- ThemeAuditRecordVO.java
            +-- ThemeBusinessSnapshotVO.java
```

## 配置说明

默认配置文件：`src/main/resources/application.yml`。

```yaml
server:
  port: 18080

spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: 123456
    url: jdbc:mysql://localhost:3306/work_aiops_agent?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
  data:
    redis:
      host: 192.168.150.101
      password: 123321

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

启动前需要准备：

- 设置环境变量 `DASHSCOPE_API_KEY`
- 创建 MySQL 数据库 `work_aiops_agent`
- 初始化 `ai_prompt_config` 表
- 插入启用状态的 `OperationQaAgent` 系统提示词
- 启动 Redis，并确保 `spring.data.redis` 配置可连接

## Prompt 配置表

项目通过 `ai_prompt_config` 表维护 Agent 提示词。服务启动时，`PromptManager` 会加载 `is_enabled = 1` 的 Prompt，并按 `agentName:promptType` 缓存在内存中。

```sql
CREATE TABLE ai_prompt_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    prompt_code VARCHAR(100) NOT NULL COMMENT 'Prompt唯一编码',
    prompt_name VARCHAR(200) NOT NULL COMMENT 'Prompt名称',
    prompt_type VARCHAR(50) NOT NULL COMMENT 'Prompt类型(system/user/tool)',
    prompt_content TEXT NOT NULL COMMENT 'Prompt内容',
    model_name VARCHAR(100) DEFAULT NULL COMMENT '绑定模型',
    agent_name VARCHAR(100) DEFAULT NULL COMMENT '所属Agent',
    version_num INT NOT NULL DEFAULT 1 COMMENT '版本号',
    is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(100) DEFAULT NULL COMMENT '创建人',
    updated_by VARCHAR(100) DEFAULT NULL COMMENT '更新人',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prompt_code_version (prompt_code, version_num),
    KEY idx_agent_name (agent_name),
    KEY idx_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Prompt配置表';
```

## 接口说明

### 发送对话

```http
POST /chat
Content-Type: application/json
Accept: text/event-stream
```

请求体：

```json
{
  "userMessage": "主题审核通过了为什么前台看不到？",
  "userId": "operator-demo",
  "chatId": "chat-001"
}
```

字段说明：

- `userMessage`：用户问题。
- `userId`：用户标识，用于隔离不同用户的会话记忆。
- `chatId`：会话标识，同一用户下不同会话互不影响。

后端会使用：

```text
conversationId = userId + "_" + chatId
```

作为 Spring AI ChatMemory 的会话 ID，并使用 Redis key：

```text
aiops:chat:{conversationId}
```

保存上下文。

### SSE 返回体

响应类型：

```text
text/event-stream
```

每个 SSE `data` 事件返回 `ChatEventVO` JSON：

```json
{
  "eventType": "001",
  "eventData": "主题审核通过只代表内容合规，前台是否可见还需要确认上架渠道、上架时间和缓存同步状态。"
}
```

事件类型：

- `001`：数据事件，`eventData` 为本次模型输出片段。
- `002`：结束事件，`eventData` 为 `[DONE]`。
- `003`：错误事件，`eventData` 为错误提示。

示例：

```bash
curl -N -X POST "http://localhost:18080/chat" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d "{\"userMessage\":\"主题审核通过了为什么前台看不到？\",\"userId\":\"operator-demo\",\"chatId\":\"chat-001\"}"
```

## 会话记忆

当前会话记忆由以下组件实现：

- `MessageChatMemoryAdvisor`：将历史消息注入 ChatClient。
- `MessageWindowChatMemory`：限制窗口大小，当前 `maxMessages = 20`。
- `RedisChatMemoryRepository`：将消息序列化后写入 Redis。
- `MessageUtil`：负责 Spring AI `Message` 与 Redis JSON 字符串互转。

注意事项：

- 同一个 `userId + chatId` 会复用历史上下文。
- 切换 `chatId` 等于开启新会话。
- 如果请求未传 `userId`，后端会使用 `anonymous`。
- 如果请求未传 `chatId`，后端会生成随机 UUID，但前端无法复用该临时会话。

## RAG 样本

项目提供主题业务 RAG 样本，目录：

```text
docs/rag/theme-business/
```

其中包含 Markdown 知识库文章，以及 CSV、JSON、JSONL、TSV、YAML、TXT、SQL 等多格式样本，便于验证结构化、半结构化和非结构化内容的混合检索效果。

## 本地启动

```bash
mvn spring-boot:run
```

服务默认监听：

```text
http://localhost:18080
```

## 前端联调台

前端工程位于 `frontend/`，基于 Vue 3 + TypeScript + Vite。

```bash
cd frontend
npm install
npm run dev
```

Windows PowerShell 如果拦截 `npm.ps1`，可以使用：

```bash
npm.cmd install
npm.cmd run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

开发环境通过 Vite proxy 将：

```text
/api/chat -> http://localhost:18080/chat
```

前端联调台支持：

- 配置 `userId`
- 配置 `chatId`
- 新建会话
- 查看 Redis 记忆键
- 解析 `ChatEventVO` SSE 返回体
- 展示流式回答

## 后续建设建议

- 接入主题业务 Tool，查询主题状态、审核状态、上架记录等实时业务数据。
- 接入 RAG 检索，将 `docs/rag/theme-business/` 知识库向量化。
- 为知识库无命中场景增加后端硬兜底。
- 完成 `TicketAgent` 和 `OpsAgent` 的 ChatClient 配置和专属 Prompt。
- 增加会话清理接口，支持按 `userId/chatId` 删除 Redis 历史上下文。

## Tool 调用稳定性治理

项目已针对主题状态、审核、上架、失败原因等确定性业务查询补充“规则识别 + Tool 强制调用 + 降级兜底”策略。

对于 `theme_10003 为什么上架失败`、`主题 10001 前台为什么不可见` 这类“包含主题 ID + 询问状态/审核/上架/失败原因”的问题，`OperationQaAgent` 不再完全依赖模型自主 Tool Calling，而是先在代码侧识别主题数据查询意图并提取主题 ID，然后强制调用 `ThemeBusinessClient#queryThemeBusinessSnapshot` 获取业务快照。

```text
用户问题
  -> OperationQaAgent 识别 themeDataQuery
  -> 提取 themeId
  -> 强制查询主题业务快照
  -> 将快照结果与 RAG 命中片段一起注入 Prompt
  -> 大模型基于实时业务数据 + 规则知识生成答复
```

如果主题业务系统异常，Agent 不会编造主题实时状态，而是记录 `forced_queryThemeBusinessSnapshot failed` Trace 事件，并将本轮回答降级为“基于知识库规则和通用处理流程回答”。这样可以同时降低模型漏调 Tool 的风险，以及业务系统不可用导致整条对话失败的风险。
