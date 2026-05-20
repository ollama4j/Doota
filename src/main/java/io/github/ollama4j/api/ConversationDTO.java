package io.github.ollama4j.api;

import java.util.List;

/**
 * DTO representing a persisted conversation (chat session).
 * Stored as ~/doota/chats/<id>.json on the filesystem.
 */
public class ConversationDTO {
    public String id;
    public String title;
    public String model;
    public List<MessageDTO> messages;

    public static class MessageDTO {
        public String role;       // user | assistant | tool
        public String content;
        public Double tps;
        public List<Object> tool_calls;
    }
}
