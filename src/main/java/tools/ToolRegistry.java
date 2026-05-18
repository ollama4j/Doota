package tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ToolRegistry {
    private final Map<String, Tool> tools = new HashMap<>();

    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
