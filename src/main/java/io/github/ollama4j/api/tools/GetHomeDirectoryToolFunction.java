package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.util.Map;

/**
 * Tool function for fetching the user's home directory. Useful when the home
 * directory cannot be found otherwise in some other ways. If a model cannot
 * identify the home directory, it can use this tool to get it. This function
 * is particularly useful for tools that operate on files and directories.
 */
public class GetHomeDirectoryToolFunction implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> arguments) {
        return Map.of("userHome", System.getProperty("user.home"));
    }
}
