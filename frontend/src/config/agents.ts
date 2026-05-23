import type { AgentProfile } from "@/types/chat";

export const agents: AgentProfile[] = [
  {
    id: "operation",
    name: "主题业务 Agent",
    code: "OperationQaAgent",
    status: "active"
  },
  {
    id: "ticket",
    name: "工单 Agent",
    code: "规划中",
    status: "planned"
  },
  {
    id: "ops",
    name: "运维排障 Agent",
    code: "规划中",
    status: "planned"
  }
];

export const quickPrompts = [
  "主题为什么上架失败？",
  "帮我查一下这个主题的审核状态",
  "主题审核通过了为什么前台看不到？",
  "这个主题当前是什么状态？"
];
