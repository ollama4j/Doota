package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FileReadTool implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> args) {
        try {
            String filePath = (String) args.get("filePath");
            if (filePath == null) return "Error: filePath is required";
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "Error: File does not exist at " + filePath;
            }
            if (!Files.isRegularFile(path)) {
                return "Error: " + filePath + " is not a regular file";
            }
            return Files.readString(path);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}
