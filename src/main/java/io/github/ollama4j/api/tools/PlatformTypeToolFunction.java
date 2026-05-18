package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool function for identifying the platform type (linux, mac, windows).
 */
public class PlatformTypeToolFunction implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> arguments) {
        Map<String, Object> response = new HashMap<>();
        String osName = System.getProperty("os.name").toLowerCase();
        
        String platform;
        if (osName.contains("win")) {
            platform = "windows";
        } else if (osName.contains("mac")) {
            platform = "mac";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            platform = "linux";
        } else {
            platform = "unknown";
        }
        
        response.put("platform", platform);
        response.put("osName", System.getProperty("os.name"));
        response.put("osVersion", System.getProperty("os.version"));
        response.put("osArch", System.getProperty("os.arch"));
        return response;
    }
}
