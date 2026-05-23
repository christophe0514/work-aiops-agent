import type { AgentProfile } from "@/types/chat";

export const agents: AgentProfile[] = [
  {
    id: "router",
    name: "路由 Agent",
    code: "AgentRouter",
    status: "active"
  },
  {
    id: "operation",
    name: "主题业务 Agent",
    code: "OperationQaAgent",
    status: "active"
  },
  {
    id: "ticket",
    name: "工单 Agent",
    code: "TicketAgent",
    status: "planned"
  },
  {
    id: "ops",
    name: "运维排障 Agent",
    code: "OpsAgent",
    status: "active"
  }
];

export const quickPrompts = [
  "主题审核通过了为什么前台看不到？",
  "theme_10003 为什么上架失败？",
  "帮我生成一个工单摘要并判断优先级",
  "接口超时、发布流水线失败怎么排查？",
  "这个为什么失败了？"
];
