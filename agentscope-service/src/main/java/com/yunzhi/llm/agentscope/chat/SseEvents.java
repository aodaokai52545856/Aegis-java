package com.yunzhi.llm.agentscope.chat;

import com.alibaba.fastjson2.JSON;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SseEvents {

    public static final String TYPE_TEXT_DELTA = "text_delta";
    public static final String TYPE_THINKING_DELTA = "thinking_delta";
    public static final String TYPE_TOOL_START = "tool_start";
    public static final String TYPE_TOOL_RESULT = "tool_result";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    public static String textDelta(String content) {
        return encode(TYPE_TEXT_DELTA, Map.of("content", content));
    }

    public static String thinkingDelta(String content) {
        return encode(TYPE_THINKING_DELTA, Map.of("content", content));
    }

    public static String toolStart(String tool, Object args) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", tool);
        payload.put("args", args);
        return encode(TYPE_TOOL_START, payload);
    }

    public static String toolResult(String tool, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", tool);
        payload.put("content", content);
        return encode(TYPE_TOOL_RESULT, payload);
    }

    public static String done(String requestId, Long keyId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("keyId", keyId);
        return encode(TYPE_DONE, payload);
    }

    public static String error(String message) {
        return encode(TYPE_ERROR, Map.of("message", message));
    }

    public static String encode(String type, Map<String, Object> fields) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.putAll(fields);
        return JSON.toJSONString(event);
    }
}
