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
