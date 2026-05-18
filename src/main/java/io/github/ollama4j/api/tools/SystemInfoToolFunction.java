package io.github.ollama4j.api.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tool function for fetching system information (OS, JVM memory, CPUs, Disk Space).
 */
public class SystemInfoToolFunction implements ToolFunction {

    @Override
    public Object apply(Map<String, Object> arguments) {
        Map<String, Object> info = new HashMap<>();

        try {
            // OS Info
            info.put("osName", System.getProperty("os.name"));
            info.put("osVersion", System.getProperty("os.version"));
            info.put("osArch", System.getProperty("os.arch"));

            // Java Runtime Info
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("javaVendor", System.getProperty("java.vendor"));

            // Hardware and CPU Info
            Runtime runtime = Runtime.getRuntime();
            info.put("cpuCoresAvailable", runtime.availableProcessors());

            // Memory Specs
            long freeMemory = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            long maxMemory = runtime.maxMemory();
            info.put("jvmFreeMemory", formatSize(freeMemory));
            info.put("jvmTotalMemory", formatSize(totalMemory));
            info.put("jvmMaxMemory", formatSize(maxMemory));

            // System Drive Info
            File[] roots = File.listRoots();
            Map<String, Object> disks = new HashMap<>();
            if (roots != null) {
                for (File root : roots) {
                    String path = root.getPath();
                    Map<String, Object> diskInfo = new HashMap<>();
                    diskInfo.put("totalSpace", formatSize(root.getTotalSpace()));
                    diskInfo.put("freeSpace", formatSize(root.getFreeSpace()));
                    diskInfo.put("usableSpace", formatSize(root.getUsableSpace()));
                    disks.put(path, diskInfo);
                }
            }
            info.put("diskDrives", disks);

            // User Space details
            info.put("username", System.getProperty("user.name"));
            info.put("userHome", System.getProperty("user.home"));
            info.put("workingDir", System.getProperty("user.dir"));

        } catch (Exception e) {
            info.put("error", "Failed to retrieve complete system information: " + e.getMessage());
        }

        return info;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
