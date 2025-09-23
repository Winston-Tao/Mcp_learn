# MCP Java Server 快速开始指南

## 🎯 目标
这个项目实现了一个完整的 MCP (Model Context Protocol) Server，帮助您理解 MCP 协议的工作原理。

## 🚀 立即开始

### 1. 环境准备
确保您的系统已安装：
- Java 17+
- Maven 3.8+

### 2. 构建项目
```bash
cd mcp-java-server
mvn clean package
```

### 3. 启动服务器

#### Linux/macOS
```bash
./scripts/start-server.sh
```

#### Windows
```cmd
scripts\start-server.bat
```

#### 手动启动
```bash
java -jar target/mcp-java-server-1.0.0.jar
```

## 🧪 测试 MCP 协议

### 测试工具调用

创建测试文件 `test-calculator.json`：
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "calculator",
    "arguments": {
      "operation": "add",
      "a": 10,
      "b": 5
    }
  }
}
```

### 测试资源访问

创建测试文件 `test-resource.json`：
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "resources/read",
  "params": {
    "uri": "config://server"
  }
}
```

### 测试提示模板

创建测试文件 `test-prompt.json`：
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "prompts/get",
  "params": {
    "name": "code_review",
    "arguments": {
      "code": "function add(a, b) { return a + b; }",
      "language": "javascript"
    }
  }
}
```

## 📋 核心功能验证清单

- [ ] ✅ 服务器启动成功
- [ ] ✅ 工具列表获取 (`tools/list`)
- [ ] ✅ 计算器工具调用
- [ ] ✅ 文件操作工具
- [ ] ✅ 天气查询工具
- [ ] ✅ 资源列表获取 (`resources/list`)
- [ ] ✅ 文件资源读取
- [ ] ✅ 配置资源访问
- [ ] ✅ 提示模板列表 (`prompts/list`)
- [ ] ✅ 代码审查提示生成

## 🔍 学习要点

### MCP 协议核心概念
1. **JSON-RPC 2.0**：基于 JSON-RPC 的通信协议
2. **Tools**：可执行的功能模块
3. **Resources**：可访问的数据源
4. **Prompts**：可参数化的提示模板

### 架构设计模式
1. **模块化设计**：Tools、Resources、Prompts 独立模块
2. **接口抽象**：统一的接口定义
3. **响应式编程**：使用 Reactor 进行异步处理
4. **依赖注入**：Spring Boot 自动装配

### 扩展开发
1. **添加新工具**：实现 `McpTool` 接口
2. **添加资源提供者**：实现 `McpResourceProvider` 接口
3. **添加提示模板**：扩展 `McpPromptProvider`

## 📁 关键文件说明

- `McpServerImpl.java`：MCP 协议处理核心
- `StdioTransportProvider.java`：STDIO 传输实现
- `CalculatorTool.java`：工具实现示例
- `FileResourceProvider.java`：资源提供者示例
- `TemplatePromptProvider.java`：提示模板示例

## 🎓 下一步学习

1. **理解协议流程**：分析请求-响应处理
2. **扩展功能**：添加自定义工具和资源
3. **优化性能**：理解响应式编程优势
4. **部署实践**：掌握 Docker 容器化部署

## 🐛 常见问题

### Q: 端口已被占用
A: 修改 `application.yml` 中的端口配置

### Q: 文件访问权限问题
A: 检查 `~/mcp-files` 目录权限

### Q: 日志级别调整
A: 修改 `logback-spring.xml` 配置

## 📞 获取帮助

查看详细文档：`README.md`
查看项目结构：使用 `tree` 命令
查看日志：`tail -f logs/mcp-server.log`