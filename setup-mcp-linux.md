# MCP Server 配置指南 (Linux 环境)

## 1. Claude Code (Linux) 配置

### Linux 配置路径
在 Debian/Ubuntu 系统中，Claude Code 配置文件位于：
```bash
~/.config/claude/claude_desktop_config.json
```

### 配置步骤

1. **创建配置目录**
```bash
mkdir -p ~/.config/claude
```

2. **创建配置文件**
```bash
cat > ~/.config/claude/claude_desktop_config.json << 'EOF'
{
  "mcpServers": {
    "mcp-java-server": {
      "command": "java",
      "args": [
        "-jar",
        "/mnt/c/Users/admin/worker/Mcp_learn/target/mcp-java-server-1.0.0.jar"
      ],
      "env": {
        "JAVA_OPTS": "-Xmx512m -Xms256m"
      }
    }
  }
}
EOF
```

3. **验证配置**
```bash
cat ~/.config/claude/claude_desktop_config.json
```

4. **重启 Claude Code**
如果 Claude Code 正在运行，请重启它以加载新配置。

## 2. 测试 MCP 连接

配置完成后，在 Claude Code 中：
- 运行 `/mcp` 命令应该显示可用的服务器
- 可以使用我们实现的工具

## 3. 启动服务器进行测试

在项目目录中：
```bash
cd /mnt/c/Users/admin/worker/Mcp_learn
./scripts/start-server.sh
```

## 4. 验证服务器状态
```bash
./scripts/stop-server.sh status
```

## 故障排除

1. **权限问题**：确保配置文件有正确的权限
```bash
chmod 644 ~/.config/claude/claude_desktop_config.json
```

2. **路径问题**：确保 JAR 文件路径在 Linux 环境下可访问
```bash
ls -la /mnt/c/Users/admin/worker/Mcp_learn/target/mcp-java-server-1.0.0.jar
```

3. **Java 环境**：确认 Linux 环境下 Java 可用
```bash
java -version
```