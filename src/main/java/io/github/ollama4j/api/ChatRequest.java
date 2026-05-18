package io.github.ollama4j.api;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    public String model;
    public List<Message> messages;

    public static class Message {
        public String role;
        public String content;
        public List<ToolCall> tool_calls;
    }

    public static class ToolCall {
        public String id;
        public String type;
        public FunctionCall function;
    }

    public static class FunctionCall {
        public String name;
        public Map<String, Object> arguments;
    }
}
