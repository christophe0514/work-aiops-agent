# AIOps Agent 前端联调台

这是主题平台 AIOps Agent 的前端调试工程，基于 Vue 3 + TypeScript + Vite 构建，用于联调后端 `/chat` SSE 流式对话接口。

## 工程结构

```text
frontend
+-- src
    +-- components      # 页面组件
    +-- composables     # 会话状态和业务逻辑
    +-- config          # Agent 和快捷问题配置
    +-- services        # 后端接口封装
    +-- styles          # 全局样式
    +-- types           # TypeScript 类型
    +-- App.vue
    +-- main.ts
```

## 启动

先启动后端服务，确保后端监听在：

```text
http://localhost:18080
```

再启动前端：

```bash
npm install
npm run dev
```

Windows PowerShell 如果提示 `npm.ps1` 被执行策略拦截，可以使用：

```bash
npm.cmd install
npm.cmd run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

## 联调说明

开发环境通过 Vite proxy 转发接口：

```text
/api/chat -> http://localhost:18080/chat
```

页面会以 `POST /api/chat` 调用后端，并按 `text/event-stream` 读取 `ChatEventVO` 事件。

请求体：

```json
{
  "userMessage": "主题审核通过了为什么前台看不到？",
  "userId": "operator-demo",
  "chatId": "chat-001"
}
```

当前页面支持配置 `userId` 和 `chatId`。后端会使用 `userId_chatId` 作为 Redis 会话记忆 key 的业务部分，同一组合会复用上下文。

## 知识库管理

前端已内置“知识库管理”标签页，用于调试 Redis Stack RAG：

- 刷新 `docs/rag/theme-business` 文件列表
- 单文件导入或重建向量
- 删除单文件对应向量
- 全量导入
- 通过问题检索命中的知识片段

对应后端接口：

```text
GET  /api/admin/kb/theme-business/files
POST /api/admin/kb/ingest/theme-business
POST /api/admin/kb/ingest/theme-business/file?path=...
POST /api/admin/kb/delete/theme-business/file?path=...
GET  /api/admin/kb/search?query=...
```

## 构建检查

```bash
npm run typecheck
npm run build
```
