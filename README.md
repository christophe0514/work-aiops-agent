# 主题平台 AIOps Agent

本项目用于构建主题平台的智能运维 Agent，面向运营、客服、开发支持人员，提供主题业务查询、工单辅助流转、运维排障等能力。

当前代码优先实现了「主题业务 Agent」的基础对话链路：用户通过 HTTP 接口提交问题，服务端使用 Spring AI 接入 DashScope 大模型，并基于数据库中配置的系统提示词生成流式回答。工单 Agent 和运维排障 Agent 已在代码结构中预留，后续继续扩展。

## 项目目标

- 为运营、客服、开发支持人员提供自然语言问答入口。
- 支持主题状态、审核状态、上架异常等业务问题查询与解释。
- 后续扩展工单分类、优先级判断、处理建议生成等能力。
- 后续扩展日志、告警、流水线、接口、服务、数据库、中间件等运维排障能力。
- 将 Agent 的核心提示词维护在数据库中，便于版本管理、灰度调整和后续 A/B 测试。

## Agent 规划

### 1. 主题业务 Agent

负责主题业务相关问答，是当前优先建设的 Agent。

典型能力包括：

- 主题状态查询
- 主题审核状态查询
- 主题上架失败原因分析
- 主题业务流程解释
- 常见运营问题答疑

当前代码中对应的 ChatClient Bean 为 `operationQaAgentClient`，系统提示词从 `ai_prompt_config` 表中按 `OperationQaAgent:system` 读取。

### 2. 工单 Agent

负责辅助工单流转，目前代码中已预留 `TicketAgent` Bean，尚未完成具体实现。

规划能力包括：

- 识别用户问题类型
- 判断工单优先级
- 推荐处理部门或开发组
- 生成工单摘要
- 给出处理建议

### 3. 运维排障 Agent

负责系统异常分析与排障建议生成，目前代码中已预留 `OpsAgent` Bean，尚未完成具体实现。

规划能力包括：

- 分析系统异常
- 查询日志、告警、流水线状态
- 判断接口、服务、数据库、中间件等异常方向
- 生成排障建议

## 当前已实现链路

用户可以通过对话输入问题，例如：

- “主题为什么上架失败？”
- “帮我查一下这个主题的审核状态”
- “主题审核通过了为什么前台看不到？”
- “这个主题当前是什么状态？”

当前代码链路如下：

```text
用户提问
   ↓
POST /chat
   ↓
ChatController 接收 userMessage
   ↓
ChatService 调用 operationQaAgentClient
   ↓
ChatClient 使用 OperationQaAgent 的 system prompt
   ↓
DashScope qwen-plus 生成回答
   ↓
Flux<String> 以 text/event-stream 形式流式返回
```

主题业务 Tool 调用真实业务数据的能力属于下一阶段建设内容，当前仓库尚未看到对应 Tool、主题业务查询服务或外部系统适配代码。

## 技术栈

- Java 17
- Spring Boot 3.2.6
- Spring AI 1.0.0
- Spring AI Alibaba DashScope
- WebFlux 流式输出
- MySQL
- MyBatis-Plus
- Maven

## 核心目录

```text
src/main/java/com/example
+-- WorkAiOpsAgentApplication.java          # Spring Boot 启动类
+-- agent/core
    +-- config
    |   +-- AiConfig.java                   # AI 公共配置，如日志 Advisor
    |   +-- ChatClientConfig.java           # Agent ChatClient Bean 配置
    |   +-- PromptManager.java              # 启动时加载并缓存 Prompt
    +-- controller
    |   +-- ChatController.java             # /chat 对话接口
    +-- dto
    |   +-- ChatMessageDTO.java             # 对话请求 DTO
    +-- entity
    |   +-- AiPromptConfig.java             # Prompt 配置实体
    +-- mapper
    |   +-- AiPromptConfigMapper.java       # MyBatis-Plus Mapper
    +-- service
        +-- AiPromptService.java
        +-- ChatService.java
        +-- impl
            +-- AiPromptServiceImpl.java
            +-- ChatServiceImpl.java
```

## 配置说明

默认配置文件位于 `src/main/resources/application.yml`。

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
```

启动前需要准备：

- 设置环境变量 `DASHSCOPE_API_KEY`
- 创建 MySQL 数据库 `work_aiops_agent`
- 初始化 `ai_prompt_config` 表
- 插入启用状态的 `OperationQaAgent` 系统提示词

## Prompt 配置表

项目通过 `ai_prompt_config` 表维护 Agent 提示词。服务启动时，`PromptManager` 会加载 `is_enabled = 1` 的 Prompt，并按 `agentName:promptType` 缓存在内存中。

当前主题业务 Agent 需要至少存在一条：

```text
agent_name = OperationQaAgent
prompt_type = system
is_enabled = 1
```

参考建表语句：

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

示例初始化数据：

```sql
INSERT INTO ai_prompt_config (
    prompt_code,
    prompt_name,
    prompt_type,
    prompt_content,
    model_name,
    agent_name,
    version_num,
    is_enabled,
    remark
) VALUES (
    'operation_qa_agent_system',
    '主题业务Agent系统提示词',
    'system',
    '你是主题平台的主题业务 Agent，面向运营、客服、开发支持人员，负责解释主题状态、审核状态、上架失败原因和常见业务流程。回答要准确、简洁，遇到缺少主题ID、商户ID、渠道等必要信息时，应先追问。',
    'qwen-plus',
    'OperationQaAgent',
    1,
    1,
    '基础版本'
);
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
  "userMessage": "主题审核通过了为什么前台看不到？"
}
```

响应：

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
  -d "{\"userMessage\":\"主题审核通过了为什么前台看不到？\"}"
```

## 本地启动

1. 准备 MySQL 数据库和 Prompt 数据。
2. 设置 DashScope API Key。
3. 启动服务。

```bash
mvn spring-boot:run
```

服务默认监听：

```text
http://localhost:18080
```

## 前端联调台

项目内置了一个 Vue 3 + TypeScript 前端联调工程，位于 `frontend/`，用于调试 `/chat` 流式对话接口。

```bash
cd frontend
npm install
npm run dev
```

前端默认启动在：

```text
http://localhost:5173
```

开发环境通过 Vite proxy 将 `/api/chat` 转发到后端 `http://localhost:18080/chat`，因此联调前需要先启动后端服务。

## 后续建设建议

- 增加主题业务 Tool，接入主题状态、审核状态、上架记录等真实业务数据。
- 为 Tool 调用定义统一入参、出参和错误码，方便大模型稳定解释查询结果。
- 增加 Agent 路由层，根据用户意图分发到主题业务 Agent、工单 Agent 或运维排障 Agent。
- 完成 `TicketAgent` 和 `OpsAgent` 的 ChatClient 配置和专属 Prompt。
- 增加会话上下文管理，支持多轮追问主题 ID、渠道、商户等关键信息。
- 增加接口测试和 Prompt 初始化脚本，降低本地启动门槛。
