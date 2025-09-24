package com.example.mcp.resources;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileResourceProvider extends AbstractResourceProvider {

    @Value("${mcp.resources.file.basePath:./data}")
    private String basePath;

    @Override
    public String getUri() {
        return "file://data";
    }

    @Override
    public String getName() {
        return "File System Resources";
    }

    @Override
    public String getDescription() {
        return "Access to files and directories in the data directory";
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    protected Object doRead() throws Exception {
        validateResource();

        Path dataPath = Paths.get(basePath).toAbsolutePath().normalize();

        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }

        logger.info("Reading file resources from: {}", dataPath);

        List<Map<String, Object>> files = Files.list(dataPath)
                .map(this::createFileInfo)
                .collect(Collectors.toList());

        return Map.of(
                "uri", getUri(),
                "name", getName(),
                "description", getDescription(),
                "basePath", dataPath.toString(),
                "files", files,
                "totalFiles", files.size(),
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    private Map<String, Object> createFileInfo(Path path) {
        try {
            return Map.of(
                    "name", path.getFileName().toString(),
                    "path", path.toString(),
                    "type", Files.isDirectory(path) ? "directory" : "file",
                    "size", Files.isDirectory(path) ? 0 : Files.size(path),
                    "lastModified", Files.getLastModifiedTime(path).toString(),
                    "readable", Files.isReadable(path),
                    "writable", Files.isWritable(path)
            );
        } catch (IOException e) {
            logger.warn("Error reading file info for {}: {}", path, e.getMessage());
            return Map.of(
                    "name", path.getFileName().toString(),
                    "path", path.toString(),
                    "error", e.getMessage()
            );
        }
    }

    @Override
    public Object getMetadata() {
        return Map.of(
                "basePath", basePath,
                "supportedOperations", List.of("list", "read", "stat"),
                "maxFileSize", "1MB",
                "encoding", "UTF-8"
        );
    }
}