package io.github.ollama4j.api;

import java.util.List;

public class AgentChatRequest {
    public String model;
    public String prompt;
    public List<Message> history;

    public static class Message {
        public String role;
        public String content;
    }
}
