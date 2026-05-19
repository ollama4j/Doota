package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.util.Map;

/**
 * Finds files whose filename contains the given name keyword (case-insensitive).
 * Use this when the user wants to search for files by name or partial name.
 */
public class FindFilesByNameToolFunction implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> arguments) {
        String searchDir = getStr(arguments, "searchDir", System.getProperty("user.home"));
        String nameQuery = getStr(arguments, "nameQuery", null);

        if (nameQuery == null || nameQuery.isBlank()) {
            return Map.of("error", "nameQuery is required for this tool.");
        }

        String queryLower = nameQuery.toLowerCase();

        boolean recursive = getBool(arguments, "recursive", true);

        return FileSearchHelper.search(
                searchDir,
                (filename, size) -> filename.toLowerCase().contains(queryLower),
                recursive
        );
    }

    private boolean getBool(Map<String, Object> args, String key, boolean defaultVal) {
        Object val = args.get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultVal;
    }

    private String getStr(Map<String, Object> args, String key, String defaultVal) {
        Object val = args.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : defaultVal;
    }
}
