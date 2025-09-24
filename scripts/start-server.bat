@echo off
rem MCP Server Start Script for Windows
rem This script starts the MCP Server with appropriate Java settings

rem Check if Java is available
java -version > nul 2>&1
if errorlevel 1 (
    echo Error: Java is not installed or not in PATH
    exit /b 1
)

rem Set JVM options for optimal performance
set JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC -XX:+EnablePreview

rem Set application properties
if "%SERVER_PORT%"=="" set SERVER_PORT=8080
if "%MCP_LOG_LEVEL%"=="" set MCP_LOG_LEVEL=INFO
if "%SPRING_PROFILES_ACTIVE%"=="" set SPRING_PROFILES_ACTIVE=default

rem Create logs directory
if not exist logs mkdir logs

echo Starting MCP Server...
java -version 2>&1 | findstr /R "java version"
echo Server Port: %SERVER_PORT%
echo Log Level: %MCP_LOG_LEVEL%
echo Profile: %SPRING_PROFILES_ACTIVE%

rem Start the application
java %JAVA_OPTS% -jar target\mcp-server-1.0.0-SNAPSHOT.jar ^
    --server.port=%SERVER_PORT% ^
    --logging.level.com.example.mcp=%MCP_LOG_LEVEL% ^
    --spring.profiles.active=%SPRING_PROFILES_ACTIVE% ^
    %*