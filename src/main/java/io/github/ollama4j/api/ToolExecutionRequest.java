package io.github.ollama4j.api;

import java.util.Map;

public class ToolExecutionRequest {
    public String toolName;
    public Map<String, Object> arguments;
}
