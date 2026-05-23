package com.example.agent.trace.context;

/**
 * 当前 Agent 调用链路的轻量上下文。
 *
 * <p>Tool Calling 由 Spring AI 在模型调用过程中触发，使用 ThreadLocal 可以让 Tool
 * 在不改变方法签名的情况下拿到当前 traceId。主流程仍然显式传递 traceId，避免只依赖线程上下文。</p>
 */
public final class AgentTraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private AgentTraceContext() {
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
