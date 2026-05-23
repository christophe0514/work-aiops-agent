import type { AgentRouteResult, ChatRequest, StreamResult } from "@/types/chat";

interface ChatEvent {
  eventType?: string;
  eventData?: unknown;
  agentCode?: string;
  agentName?: string;
}

export async function streamChat(
  request: ChatRequest,
  onDelta: (content: string) => void,
  onRoute?: (route: AgentRouteResult) => void
) {
  const response = await fetch("/api/chat", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(`后端返回 ${response.status} ${response.statusText}`);
  }

  if (!response.body) {
    throw new Error("浏览器未收到可读取的流式响应");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();

    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const parsed = consumeEventStream(buffer);
    buffer = parsed.rest;

    if (parsed.content) {
      handleEventPayload(parsed.content, onDelta, onRoute);
    }
  }

  if (buffer) {
    const parsed = consumeEventStream(buffer, true);
    if (parsed.content) {
      handleEventPayload(parsed.content, onDelta, onRoute);
    }
  }
}

function handleEventPayload(
  content: string,
  onDelta: (content: string) => void,
  onRoute?: (route: AgentRouteResult) => void
) {
  const frames = content
    .split("\n")
    .map((item) => item.trim())
    .filter(Boolean);

  for (const frame of frames) {
    const event = parseChatEvent(frame);

    if (!event) {
      onDelta(frame);
      continue;
    }

    if (event.eventType === "001") {
      onDelta(String(event.eventData ?? ""));
      continue;
    }

    if (event.eventType === "004") {
      onRoute?.(normalizeRouteEvent(event));
      continue;
    }

    if (event.eventType === "003") {
      throw new Error(String(event.eventData ?? "对话生成失败"));
    }
  }
}

function normalizeRouteEvent(event: ChatEvent): AgentRouteResult {
  const route = typeof event.eventData === "object" && event.eventData
    ? event.eventData as AgentRouteResult
    : {};

  return {
    ...route,
    agentCode: route.agentCode ?? event.agentCode,
    agentName: route.agentName ?? event.agentName
  };
}

function parseChatEvent(frame: string): ChatEvent | null {
  try {
    return JSON.parse(frame) as ChatEvent;
  } catch {
    return null;
  }
}

function consumeEventStream(buffer: string, flush = false): StreamResult {
  const separator = buffer.includes("\r\n\r\n") ? "\r\n\r\n" : "\n\n";
  const frames = buffer.split(separator);
  const rest = flush ? "" : frames.pop() ?? "";

  const content = frames
    .map((frame) => {
      const dataLines = frame
        .split(/\r?\n/)
        .filter((line) => line.startsWith("data:"))
        .map((line) => line.replace(/^data:\s?/, ""));

      if (dataLines.length > 0) {
        const data = dataLines.join("\n");
        return data === "[DONE]" ? "" : data;
      }

      return frame;
    })
    .join("\n");

  return { content, rest };
}
