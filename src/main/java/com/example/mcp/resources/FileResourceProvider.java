package com.example.mcp.resources;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileResourceProvider extends AbstractResourceProvider {

    private static final String SCHEME = "file://";
    private static final String SAFE_BASE_PATH = System.getProperty("user.home") + "/mcp-files";

    public FileResourceProvider() {
        // Create safe base directory
        try {
            Files.createDirectories(Paths.get(SAFE_BASE_PATH));

            // Create some sample files
            createSampleFiles();
        } catch (IOException e) {
            logger.warn("Could not create base directory: {}", SAFE_BASE_PATH, e);
        }
    }

    private void createSampleFiles() {
        try {
            Path sampleDir = Paths.get(SAFE_BASE_PATH, "samples");
            Files.createDirectories(sampleDir);

            // Create sample text file
            Path textFile = sampleDir.resolve("sample.txt");
            if (!Files.exists(textFile)) {
                Files.writeString(textFile, "This is a sample text file for MCP Server demonstration.\n" +
                        "You can read this file using the MCP resources API.\n" +
                        "Created at: " + java.time.LocalDateTime.now());
            }

            // Create sample JSON file
            Path jsonFile = sampleDir.resolve("config.json");
            if (!Files.exists(jsonFile)) {
                String jsonContent = """
                    {
                        "server": {
                            "name": "mcp-java-server",
                            "version": "1.0.0",
                            "description": "Sample configuration file"
                        },
                        "features": [
                            "tools",
                            "resources",
                            "prompts"
                        ],
                        "settings": {
                            "debug": false,
                            "maxConnections": 10
                        }
                    }
                    """;
                Files.writeString(jsonFile, jsonContent);
            }

        } catch (IOException e) {
            logger.warn("Could not create sample files", e);
        }
    }

    @Override
    public List<Map<String, Object>> listResources() {
        List<Map<String, Object>> resources = new ArrayList<>();

        try {
            Path basePath = Paths.get(SAFE_BASE_PATH);

            Files.walk(basePath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String relativePath = basePath.relativize(path).toString().replace("\\", "/");
                    String uri = SCHEME + relativePath;
                    String name = path.getFileName().toString();
                    String description = "File resource: " + relativePath;
                    String mimeType = guessMimeType(path);

                    resources.add(createResource(uri, name, description, mimeType));
                });

        } catch (IOException e) {
            logger.error("Error listing file resources", e);
        }

        return resources;
    }

    @Override
    public boolean canHandle(String uri) {
        return uri != null && uri.startsWith(SCHEME);
    }

    @Override
    public Mono<List<Map<String, Object>>> readResource(String uri) {
        return Mono.fromCallable(() -> {
            if (!canHandle(uri)) {
                throw new IllegalArgumentException("Cannot handle URI: " + uri);
            }

            String relativePath = uri.substring(SCHEME.length());
            Path safePath = getSafePath(relativePath);

            if (safePath == null) {
                throw new IllegalArgumentException("Invalid path: " + relativePath);
            }

            if (!Files.exists(safePath)) {
                throw new IllegalArgumentException("File does not exist: " + relativePath);
            }

            if (Files.isDirectory(safePath)) {
                return readDirectory(safePath);
            } else {
                return readFile(safePath);
            }
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

    private List<Map<String, Object>> readFile(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);

        Map<String, Object> resourceContent = Map.of(
            "uri", SCHEME + Paths.get(SAFE_BASE_PATH).relativize(path).toString().replace("\\", "/"),
            "mimeType", guessMimeType(path),
            "text", content
        );

        return List.of(resourceContent);
    }

    private List<Map<String, Object>> readDirectory(Path path) throws IOException {
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

        String listing = "Directory contents:\n" + String.join("\n", entries);

        Map<String, Object> resourceContent = Map.of(
            "uri", SCHEME + Paths.get(SAFE_BASE_PATH).relativize(path).toString().replace("\\", "/"),
            "mimeType", "text/plain",
            "text", listing
        );

        return List.of(resourceContent);
    }

    private String guessMimeType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".xml")) {
            return "application/xml";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".md")) {
            return "text/markdown";
        } else if (fileName.endsWith(".csv")) {
            return "text/csv";
        } else {
            return "application/octet-stream";
        }
    }
}