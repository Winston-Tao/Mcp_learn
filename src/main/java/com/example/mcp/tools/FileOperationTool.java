package com.example.mcp.tools;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileOperationTool extends AbstractMcpTool {

    private static final String SAFE_BASE_PATH = System.getProperty("user.home") + "/mcp-files";

    public FileOperationTool() {
        super(
            "file_operations",
            "Performs file system operations like reading, writing, and listing files",
            createInputSchema()
        );

        // Create safe base directory
        try {
            Files.createDirectories(Paths.get(SAFE_BASE_PATH));
        } catch (IOException e) {
            logger.warn("Could not create base directory: {}", SAFE_BASE_PATH, e);
        }
    }

    private static Map<String, Object> createInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "action", Map.of(
                    "type", "string",
                    "enum", List.of("read", "write", "list", "delete"),
                    "description", "The file operation to perform"
                ),
                "path", Map.of(
                    "type", "string",
                    "description", "The file or directory path (relative to safe base directory)"
                ),
                "content", Map.of(
                    "type", "string",
                    "description", "Content to write (required for write action)"
                )
            ),
            "required", List.of("action", "path")
        );
    }

    @Override
    protected Mono<Void> validateArguments(Map<String, Object> arguments) {
        String action = (String) arguments.get("action");
        String path = (String) arguments.get("path");

        if (action == null) {
            return Mono.error(new IllegalArgumentException("Missing action"));
        }

        if (path == null) {
            return Mono.error(new IllegalArgumentException("Missing path"));
        }

        if (!List.of("read", "write", "list", "delete").contains(action)) {
            return Mono.error(new IllegalArgumentException("Invalid action: " + action));
        }

        if ("write".equals(action) && arguments.get("content") == null) {
            return Mono.error(new IllegalArgumentException("Content required for write action"));
        }

        // Security check: ensure path is within safe directory
        Path safePath = getSafePath(path);
        if (safePath == null) {
            return Mono.error(new IllegalArgumentException("Invalid path: " + path));
        }

        return Mono.empty();
    }

    @Override
    protected Mono<List<Map<String, Object>>> doExecute(Map<String, Object> arguments) {
        return Mono.fromCallable(() -> {
            String action = (String) arguments.get("action");
            String path = (String) arguments.get("path");
            Path safePath = getSafePath(path);

            return switch (action) {
                case "read" -> readFile(safePath);
                case "write" -> writeFile(safePath, (String) arguments.get("content"));
                case "list" -> listDirectory(safePath);
                case "delete" -> deleteFile(safePath);
                default -> throw new IllegalArgumentException("Unsupported action: " + action);
            };
        });
    }

    private Path getSafePath(String relativePath) {
        try {
            Path basePath = Paths.get(SAFE_BASE_PATH).toAbsolutePath().normalize();
            Path targetPath = basePath.resolve(relativePath).normalize();

            // Ensure the target path is within the safe base directory
            if (targetPath.startsWith(basePath)) {
                return targetPath;
            }
        } catch (Exception e) {
            logger.warn("Invalid path: {}", relativePath, e);
        }
        return null;
    }

    private List<Map<String, Object>> readFile(Path path) {
        try {
            if (!Files.exists(path)) {
                return List.of(createErrorContent("File does not exist: " + path.getFileName()));
            }

            if (Files.isDirectory(path)) {
                return List.of(createErrorContent("Path is a directory, not a file: " + path.getFileName()));
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            return List.of(createTextContent("File content:\n" + content));

        } catch (IOException e) {
            logger.error("Error reading file: {}", path, e);
            return List.of(createErrorContent("Failed to read file: " + e.getMessage()));
        }
    }

    private List<Map<String, Object>> writeFile(Path path, String content) {
        try {
            // Create parent directories if they don't exist
            Files.createDirectories(path.getParent());

            Files.writeString(path, content, StandardCharsets.UTF_8);
            return List.of(createTextContent("Successfully wrote " + content.length() + " characters to " + path.getFileName()));

        } catch (IOException e) {
            logger.error("Error writing file: {}", path, e);
            return List.of(createErrorContent("Failed to write file: " + e.getMessage()));
        }
    }

    private List<Map<String, Object>> listDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                return List.of(createErrorContent("Directory does not exist: " + path.getFileName()));
            }

            if (!Files.isDirectory(path)) {
                return List.of(createErrorContent("Path is not a directory: " + path.getFileName()));
            }

            List<String> entries = Files.list(path)
                .map(p -> {
                    String name = p.getFileName().toString();
                    if (Files.isDirectory(p)) {
                        return name + "/";
                    }
                    try {
                        long size = Files.size(p);
                        return name + " (" + size + " bytes)";
                    } catch (IOException e) {
                        return name;
                    }
                })
                .sorted()
                .collect(Collectors.toList());

            if (entries.isEmpty()) {
                return List.of(createTextContent("Directory is empty"));
            }

            String listing = "Directory contents:\n" + String.join("\n", entries);
            return List.of(createTextContent(listing));

        } catch (IOException e) {
            logger.error("Error listing directory: {}", path, e);
            return List.of(createErrorContent("Failed to list directory: " + e.getMessage()));
        }
    }

    private List<Map<String, Object>> deleteFile(Path path) {
        try {
            if (!Files.exists(path)) {
                return List.of(createErrorContent("File does not exist: " + path.getFileName()));
            }

            if (Files.isDirectory(path)) {
                // Only delete if directory is empty
                if (Files.list(path).findAny().isPresent()) {
                    return List.of(createErrorContent("Directory is not empty: " + path.getFileName()));
                }
            }

            Files.delete(path);
            return List.of(createTextContent("Successfully deleted: " + path.getFileName()));

        } catch (IOException e) {
            logger.error("Error deleting file: {}", path, e);
            return List.of(createErrorContent("Failed to delete file: " + e.getMessage()));
        }
    }
}