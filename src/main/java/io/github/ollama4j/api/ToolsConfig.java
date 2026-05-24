package io.github.ollama4j.api;

import java.util.List;

/**
 * POJO for deserializing tools.yaml.
 * Each entry maps to one registered tool in OllamaService.
 */
public class ToolsConfig {

    private List<ToolEntry> tools;

    public List<ToolEntry> getTools() { return tools; }
    public void setTools(List<ToolEntry> tools) { this.tools = tools; }

    public static class ToolEntry {
        private String name;
        private String displayName;
        private String description;
        private boolean enabled = true;
        private String functionClass;
        private List<ParameterEntry> parameters;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getFunctionClass() { return functionClass; }
        public void setFunctionClass(String functionClass) { this.functionClass = functionClass; }

        public List<ParameterEntry> getParameters() { return parameters; }
        public void setParameters(List<ParameterEntry> parameters) { this.parameters = parameters; }
    }

    public static class ParameterEntry {
        private String name;
        private String type;
        private String description;
        private boolean required;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }
}
