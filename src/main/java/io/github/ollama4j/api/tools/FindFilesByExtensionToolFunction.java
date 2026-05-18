package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.util.Map;

/**
 * Finds files that match a given file extension (e.g. "pdf", "txt", "png").
 * Use this when the user wants to search for files by their type/extension.
 */
public class FindFilesByExtensionToolFunction implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> arguments) {
        String searchDir = getStr(arguments, "searchDir", System.getProperty("user.home"));
        String extension = getStr(arguments, "extension", null);

        if (extension == null || extension.isBlank()) {
            return Map.of("error", "extension is required for this tool.");
        }

        // Normalise: strip leading dot if user passed ".pdf" instead of "pdf"
        String ext = extension.startsWith(".") ? extension.toLowerCase() : "." + extension.toLowerCase();

        return FileSearchHelper.search(
                searchDir,
                (filename, size) -> filename.toLowerCase().endsWith(ext),
                true
        );
    }

    private String getStr(Map<String, Object> args, String key, String defaultVal) {
        Object val = args.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : defaultVal;
    }
}
