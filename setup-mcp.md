# MCP Server 配置指南

## 1. Claude Desktop 配置

### Windows 配置路径
Claude Desktop 的配置文件位于：
```
%APPDATA%\Claude\claude_desktop_config.json
```

### 配置步骤

1. **打开配置文件路径**
   - 按 `Win + R` 打开运行对话框
   - 输入 `%APPDATA%\Claude` 并按回车
   - 如果文件夹不存在，请创建它

2. **创建或编辑配置文件**
   将以下内容保存为 `claude_desktop_config.json`：

```json
{
  "mcpServers": {
    "mcp-java-server": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\admin\\worker\\Mcp_learn\\target\\mcp-java-server-1.0.0.jar"
      ],
      "env": {
        "JAVA_OPTS": "-Xmx512m -Xms256m"
      }
    }
  }
}
```

**重要**: 请将路径 `C:\\Users\\admin\\worker\\Mcp_learn\\target\\mcp-java-server-1.0.0.jar` 替换为您实际的项目路径。

3. **重启 Claude Desktop**
   配置完成后，完全关闭并重新启动 Claude Desktop。

## 2. 验证 MCP 连接

在 Claude Desktop 中，您应该能够：
- 看到 MCP 服务器状态
- 使用 `/mcp` 命令查看可用的工具和资源
- 调用我们实现的工具：
  - `calculator` - 数学计算
  - `file_operations` - 文件操作
  - `weather` - 天气查询

## 3. 测试示例

配置成功后，您可以在 Claude Desktop 中尝试：

```
请使用计算器工具计算 15 + 27
```

```
请读取文件 samples/sample.txt 的内容
```

```
请查询北京的天气情况
```

## 故障排除

如果遇到问题：

1. **检查路径**：确保 JAR 文件路径正确
2. **检查 Java**：确保系统已安装 Java 17+
3. **查看日志**：检查 `logs/mcp-server.log` 文件
4. **重启服务**：使用 `./scripts/start-server.sh` 重新启动服务器

## Claude Code (VS Code Extension) 配置

如果您使用的是 Claude Code VS Code 扩展，配置方法可能略有不同。请查看扩展的文档或使用命令面板中的 MCP 配置选项。