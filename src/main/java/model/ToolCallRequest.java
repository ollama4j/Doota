package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCallRequest {
    private String tool;
    private Map<String, Object> arguments;

    public ToolCallRequest() {}

    public ToolCallRequest(String tool, Map<String, Object> arguments) {
        this.tool = tool;
        this.arguments = arguments;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
