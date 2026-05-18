package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.util.Map;

public class WeatherTool implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> args) {
        String location = (String) args.get("location");
        if (location == null) return "Error: location is required";
        // Mock implementation
        return "The weather in " + location + " is sunny and 72 degrees Fahrenheit.";
    }
}
