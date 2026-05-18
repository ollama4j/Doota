package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.util.Map;

/**
 * Tool function for fetching the user's home directory.
 */
public class GetHomeDirectoryToolFunction implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> arguments) {
        return Map.of("userHome", System.getProperty("user.home"));
    }
}
