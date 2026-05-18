package io.github.ollama4j.api.tools;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Shared utility for walking the local filesystem safely and collecting matched files.
 */
public class FileSearchHelper {

    public static final int MAX_VISITED = 3000;
    public static final int MAX_RESULTS = 50;

    public interface FileMatcher {
        boolean matches(String filename, long sizeBytes);
    }

    public static Map<String, Object> search(String searchDir, FileMatcher matcher, boolean recursive) {
        Path startPath = Paths.get(searchDir);
        if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
            return Map.of("error", "The search path '" + searchDir + "' is not a valid directory.");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int[] visited = {0};
        boolean[] limitReached = {false};

        try {
            int maxDepth = recursive ? Integer.MAX_VALUE : 1;
            Files.walkFileTree(startPath, EnumSet.noneOf(FileVisitOption.class), maxDepth,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            visited[0]++;
                            if (visited[0] > MAX_VISITED) {
                                limitReached[0] = true;
                                return FileVisitResult.TERMINATE;
                            }
                            if (attrs.isRegularFile()) {
                                String name = file.getFileName().toString();
                                long size = attrs.size();
                                if (matcher.matches(name, size)) {
                                    results.add(Map.of(
                                            "path", file.toAbsolutePath().toString(),
                                            "name", name,
                                            "sizeBytes", size,
                                            "sizeFormatted", formatSize(size),
                                            "lastModified", attrs.lastModifiedTime().toString()
                                    ));
                                    if (results.size() >= MAX_RESULTS) {
                                        return FileVisitResult.TERMINATE;
                                    }
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE; // skip permission-denied files silently
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            if (visited[0] > MAX_VISITED) {
                                limitReached[0] = true;
                                return FileVisitResult.TERMINATE;
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (Exception e) {
            return Map.of("error", "File search failed: " + e.getMessage());
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("searchDirectory", startPath.toAbsolutePath().toString());
        output.put("matchCount", results.size());
        output.put("visitLimitReached", limitReached[0]);
        output.put("results", results);
        return output;
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
