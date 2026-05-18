package tools;

import java.util.Map;

public interface Tool {
    /**
     * Unique name of the tool, used by the LLM to call it.
     * Example: "calculator", "weather_tool"
     */
    String getName();

    /**
     * Description of what the tool does.
     * Crucial for the LLM to understand when and how to use it.
     */
    String getDescription();

    /**
     * Optional: Provide a JSON Schema string describing the parameters.
     * This will be dynamically injected into the system prompt.
     */
    String getParameterSchema();

    /**
     * Execute the tool with the given arguments from the LLM.
     * @param args The JSON arguments parsed into a map.
     * @return The result of the execution (usually a string, or an object to be serialized).
     */
    Object execute(Map<String, Object> args) throws Exception;
}
