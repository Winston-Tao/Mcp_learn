# MCP Java Server

一个完整的 Model Context Protocol (MCP) Server 实现示例，使用 Java 和 Spring Boot 构建。这个项目旨在帮助开发者理解 MCP 协议的工作原理和实现细节。

## 🚀 项目特性

### 核心功能
- **完整的 MCP 协议实现**：支持 Tools、Resources、Prompts 三大核心功能
- **STDIO 传输**：支持标准输入输出通信
- **响应式编程**：基于 Spring WebFlux 的异步处理
- **模块化设计**：易于扩展和自定义

### 内置工具 (Tools)
- **🧮 计算器**：执行基本数学运算
- **📁 文件操作**：安全的文件读写和目录操作
- **🌤️ 天气查询**：模拟天气数据获取

### 资源提供者 (Resources)
- **📄 文件资源**：访问本地文件系统
- **⚙️ 配置资源**：服务器配置和运行时信息

### 提示模板 (Prompts)
- **👩‍💻 代码审查**：生成代码审查提示
- **📚 文档生成**：创建技术文档模板
- **🐛 调试助手**：调试问题分析
- **📝 会议总结**：会议记录整理
- **✍️ 创意写作**：创意写作提示

## 📋 系统要求

- Java 17 或更高版本
- Maven 3.8 或更高版本
- Docker（可选，用于容器化部署）

## 🛠️ 快速开始

### 方式一：使用启动脚本（推荐）

#### Linux/macOS
```bash
# 克隆项目
git clone <repository-url>
cd mcp-java-server

# 启动服务器
./scripts/start-server.sh

# 查看状态
./scripts/stop-server.sh status

# 停止服务器
./scripts/stop-server.sh stop
```

#### Windows
```cmd
# 克隆项目
git clone <repository-url>
cd mcp-java-server

# 启动服务器
scripts\start-server.bat
```

### 方式二：手动启动

```bash
# 构建项目
mvn clean package

# 运行服务器
java -jar target/mcp-java-server-1.0.0.jar
```

### 方式三：Docker 部署

```bash
# 使用 Docker Compose
./scripts/deploy.sh deploy-compose

# 或者单独构建镜像
./scripts/deploy.sh build

# 查看部署状态
./scripts/deploy.sh status
```

## 📖 使用示例

### 工具调用示例

#### 计算器工具
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "calculator",
    "arguments": {
      "operation": "add",
      "a": 5,
      "b": 3
    }
  }
}
```

#### 文件操作工具
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "file_operations",
    "arguments": {
      "action": "read",
      "path": "samples/sample.txt"
    }
  }
}
```

### 资源访问示例

#### 读取文件资源
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "resources/read",
  "params": {
    "uri": "file://samples/sample.txt"
  }
}
```

#### 获取服务器配置
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "resources/read",
  "params": {
    "uri": "config://server"
  }
}
```

### 提示模板示例

#### 代码审查提示
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "prompts/get",
  "params": {
    "name": "code_review",
    "arguments": {
      "code": "function add(a, b) { return a + b; }",
      "language": "javascript",
      "focus": "security and best practices"
    }
  }
}
```

## 🏗️ 项目结构

```
mcp-java-server/
├── src/main/java/com/example/mcp/
│   ├── McpServerApplication.java     # 应用入口
│   ├── config/                       # 配置类
│   │   ├── ServerConfig.java
│   │   └── McpServerConfig.java
│   ├── server/                       # 服务器核心
│   │   ├── McpServerImpl.java
│   │   ├── McpMessage.java
│   │   └── McpError.java
│   ├── tools/                        # 工具实现
│   │   ├── McpTool.java
│   │   ├── CalculatorTool.java
│   │   ├── FileOperationTool.java
│   │   └── WeatherTool.java
│   ├── resources/                    # 资源提供者
│   │   ├── McpResourceProvider.java
│   │   ├── FileResourceProvider.java
│   │   └── ConfigResourceProvider.java
│   ├── prompts/                      # 提示模板
│   │   ├── McpPromptProvider.java
│   │   └── TemplatePromptProvider.java
│   └── transport/                    # 传输层
│       └── StdioTransportProvider.java
├── src/main/resources/
│   ├── application.yml               # Spring Boot 配置
│   ├── logback-spring.xml           # 日志配置
│   └── mcp-config.json              # MCP 配置
├── scripts/                          # 部署脚本
│   ├── start-server.sh              # 启动脚本 (Linux/macOS)
│   ├── start-server.bat             # 启动脚本 (Windows)
│   ├── stop-server.sh               # 停止脚本
│   └── deploy.sh                    # Docker 部署脚本
├── docker/                          # Docker 配置
│   ├── Dockerfile
│   └── docker-compose.yml
└── README.md                        # 项目文档
```

## ⚙️ 配置说明

### 应用配置 (application.yml)
- 服务器基本信息
- 传输方式配置
- 功能开关设置
- 日志级别配置

### MCP 配置 (mcp-config.json)
- MCP 协议特定配置
- 安全策略设置
- 使用示例

### 环境变量
- `JAVA_OPTS`: JVM 参数配置
- `SPRING_PROFILES_ACTIVE`: Spring 配置文件
- `MCP_SERVER_NAME`: 服务器名称
- `MCP_SERVER_VERSION`: 服务器版本

## 🔧 开发指南

### 添加新工具

1. 实现 `McpTool` 接口：
```java
@Component
public class MyCustomTool extends AbstractMcpTool {
    public MyCustomTool() {
        super("my_tool", "工具描述", createInputSchema());
    }

    @Override
    protected Mono<List<Map<String, Object>>> doExecute(Map<String, Object> arguments) {
        // 实现工具逻辑
        return Mono.just(List.of(createTextContent("结果")));
    }
}
```

2. Spring Boot 会自动发现并注册新工具

### 添加新资源提供者

1. 实现 `McpResourceProvider` 接口：
```java
@Component
public class MyResourceProvider extends AbstractResourceProvider {
    @Override
    public boolean canHandle(String uri) {
        return uri.startsWith("myscheme://");
    }

    @Override
    public Mono<List<Map<String, Object>>> readResource(String uri) {
        // 实现资源读取逻辑
    }
}
```

### 添加新提示模板

1. 在 `TemplatePromptProvider` 中添加新模板
2. 实现模板生成逻辑

## 🧪 测试

### 运行测试
```bash
mvn test
```

### 手动测试
使用任何支持 JSON-RPC 的客户端工具，如：
- curl
- Postman
- 自定义 MCP 客户端

### 示例测试命令
```bash
# 初始化连接
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}' | java -jar target/mcp-java-server-1.0.0.jar
```

## 📊 监控和日志

### 日志文件
- `logs/mcp-server.log`: 应用日志
- `logs/server.out`: 启动输出

### 日志级别
- `DEBUG`: 详细的调试信息
- `INFO`: 一般信息
- `WARN`: 警告信息
- `ERROR`: 错误信息

## 🔒 安全考虑

- 文件访问限制在安全目录内 (`~/mcp-files`)
- 输入参数验证
- 资源访问控制
- 进程隔离（Docker 部署）

## 🚀 部署

### 本地开发
```bash
./scripts/start-server.sh
```

### 生产环境
```bash
# Docker 部署
./scripts/deploy.sh deploy-compose

# 或者手动部署
mvn clean package
java -jar target/mcp-java-server-1.0.0.jar --spring.profiles.active=prod
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 🔗 相关链接

- [Model Context Protocol 官方文档](https://modelcontextprotocol.io/)
- [MCP Java SDK](https://modelcontextprotocol.io/sdk/java/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)

## ❓ 常见问题

### Q: 如何修改服务器端口？
A: 修改 `application.yml` 中的 `mcp.server.transport.port` 配置

### Q: 如何添加新的文件类型支持？
A: 在 `FileResourceProvider` 中的 `guessMimeType` 方法添加新的文件扩展名

### Q: 如何启用 HTTP 传输？
A: 修改配置文件中的 `mcp.server.transport.type` 为 `HTTP`

### Q: 如何查看详细的调试信息？
A: 设置日志级别为 `DEBUG`：`logging.level.com.example.mcp: DEBUG`