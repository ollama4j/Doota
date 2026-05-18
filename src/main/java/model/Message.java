package model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private Role role;
    private String content;
    
    // Using a list for potential parallel tool calls
    private List<ToolCallRequest> toolCalls;
    private String toolName;
    private String toolResult;

    public Message() {}

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content);
    }

    public static Message user(String content) {
        return new Message(Role.USER, content);
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content);
    }

    public static Message assistantWithToolCalls(String content, List<ToolCallRequest> toolCalls) {
        Message msg = new Message(Role.ASSISTANT, content);
        msg.setToolCalls(toolCalls);
        return msg;
    }

    public static Message toolResult(String toolName, String result) {
        Message msg = new Message(Role.TOOL, null); // Tool responses typically don't have standard 'content' in some formats, but we'll use toolResult field
        msg.setToolName(toolName);
        msg.setToolResult(result);
        return msg;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ToolCallRequest> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallRequest> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolResult() {
        return toolResult;
    }

    public void setToolResult(String toolResult) {
        this.toolResult = toolResult;
    }
}
