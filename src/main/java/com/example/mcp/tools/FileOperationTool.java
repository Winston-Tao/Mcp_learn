package com.example.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileOperationTool extends AbstractMcpTool {

    private static final int MAX_FILE_SIZE = 1024 * 1024; // 1MB
    private static final String BASE_PATH = System.getProperty("user.dir") + "/data";

    @Override
    public String getName() {
        return "file_operation";
    }

    @Override
    public String getDescription() {
        return "Perform file operations: read, write, list directory contents (restricted to data directory)";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode operation = objectMapper.createObjectNode();
        operation.put("type", "string");
        operation.put("description", "File operation to perform");
        operation.set("enum", objectMapper.valueToTree(new String[]{"read", "write", "list", "exists", "delete"}));
        properties.set("operation", operation);

        ObjectNode path = objectMapper.createObjectNode();
        path.put("type", "string");
        path.put("description", "File or directory path relative to data directory");
        properties.set("path", path);

        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", "string");
        content.put("description", "Content to write (for write operation)");
        properties.set("content", content);

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(new String[]{"operation", "path"}));

        return schema;
    }

    @Override
    protected Object doExecute(JsonNode parameters) throws Exception {
        requireParameter(parameters, "operation");
        requireParameter(parameters, "path");

        String operation = getStringParameter(parameters, "operation");
        String relativePath = getStringParameter(parameters, "path");
        String content = getStringParameter(parameters, "content", "");

        Path safePath = validateAndResolvePath(relativePath);

        logger.info("File operation: {} on path: {}", operation, safePath);

        return switch (operation.toLowerCase()) {
            case "read" -> handleRead(safePath);
            case "write" -> handleWrite(safePath, content);
            case "list" -> handleList(safePath);
            case "exists" -> handleExists(safePath);
            case "delete" -> handleDelete(safePath);
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    private Path validateAndResolvePath(String relativePath) throws Exception {
        Path basePath = Paths.get(BASE_PATH).normalize().toAbsolutePath();
        Path targetPath = basePath.resolve(relativePath).normalize();

        if (!targetPath.startsWith(basePath)) {
            throw new SecurityException("Path traversal attempt detected: " + relativePath);
        }

        Files.createDirectories(basePath);
        return targetPath;
    }

    private Object handleRead(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new IOException("Path is not a regular file: " + path);
        }

        long fileSize = Files.size(path);
        if (fileSize > MAX_FILE_SIZE) {
            throw new IOException("File is too large (max " + MAX_FILE_SIZE + " bytes): " + fileSize);
        }

        String content = Files.readString(path, StandardCharsets.UTF_8);

        return Map.of(
                "operation", "read",
                "path", path.toString(),
                "size", fileSize,
                "content", content
        );
    }

    private Object handleWrite(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);

        return Map.of(
                "operation", "write",
                "path", path.toString(),
                "size", content.getBytes(StandardCharsets.UTF_8).length,
                "success", true
        );
    }

    private Object handleList(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Directory does not exist: " + path);
        }

        if (!Files.isDirectory(path)) {
            throw new IOException("Path is not a directory: " + path);
        }

        List<Map<String, Object>> entries = Files.list(path)
                .map(p -> Map.<String, Object>of(
                        "name", p.getFileName().toString(),
                        "type", Files.isDirectory(p) ? "directory" : "file",
                        "size", getFileSize(p),
                        "lastModified", getLastModified(p)
                ))
                .collect(Collectors.toList());

        return Map.of(
                "operation", "list",
                "path", path.toString(),
                "entries", entries
        );
    }

    private Object handleExists(Path path) {
        boolean exists = Files.exists(path);
        boolean isDirectory = Files.isDirectory(path);
        boolean isFile = Files.isRegularFile(path);

        return Map.of(
                "operation", "exists",
                "path", path.toString(),
                "exists", exists,
                "isDirectory", isDirectory,
                "isFile", isFile
        );
    }

    private Object handleDelete(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        boolean deleted = Files.deleteIfExists(path);

        return Map.of(
                "operation", "delete",
                "path", path.toString(),
                "deleted", deleted
        );
    }

    private long getFileSize(Path path) {
        try {
            return Files.isDirectory(path) ? 0 : Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    private String getLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toString();
        } catch (IOException e) {
            return "unknown";
        }
    }
}