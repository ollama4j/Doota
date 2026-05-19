package io.github.ollama4j.api;

/**
 * Data Transfer Object containing information about registered AI agent tools.
 */
public class ToolInfo {
    private String name;
    private String displayName;
    private String description;
    private boolean enabled;

    public ToolInfo() {}

    public ToolInfo(String name, String displayName, String description, boolean enabled) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
