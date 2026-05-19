package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DateTimeTool implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> args) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}
