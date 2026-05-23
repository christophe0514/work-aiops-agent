export type AgentStatus = "active" | "planned";

export type MessageRole = "system" | "user" | "assistant";

export interface AgentProfile {
  id: string;
  name: string;
  code: string;
  status: AgentStatus;
}

export interface ChatMessage {
  id: number;
  role: MessageRole;
  content: string;
  pending?: boolean;
  agentCode?: string;
  agentName?: string;
}

export interface ChatRequest {
  userMessage: string;
  userId: string;
  chatId: string;
}

export interface StreamResult {
  content: string;
  rest: string;
}

export interface AgentRouteResult {
  agentCode?: string;
  agentName?: string;
  reason?: string;
  confidence?: number;
  needClarify?: boolean;
  clarifyQuestion?: string | null;
}
