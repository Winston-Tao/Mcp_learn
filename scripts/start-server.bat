@echo off
setlocal enabledelayedexpansion

REM MCP Java Server Startup Script for Windows

REM Configuration
set JAR_NAME=mcp-java-server-1.0.0.jar
set JAVA_OPTS=-Xmx512m -Xms256m
set LOG_DIR=logs
set PID_FILE=mcp-server.pid

echo [INFO] MCP Java Server Startup Script
echo [INFO] ==============================

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH
    echo [INFO] Please install Java 17 or later
    pause
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
)
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%
for /f "delims=. tokens=1" %%v in ("%JAVA_VERSION_STRING%") do set JAVA_VERSION=%%v

if %JAVA_VERSION% lss 17 (
    echo [ERROR] Java 17 or later is required. Found version: %JAVA_VERSION%
    pause
    exit /b 1
)

echo [INFO] Java version: %JAVA_VERSION%

REM Check if Maven is installed
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not installed or not in PATH
    pause
    exit /b 1
)

REM Check if pom.xml exists
if not exist "pom.xml" (
    echo [ERROR] pom.xml not found. Please run this script from the project root.
    pause
    exit /b 1
)

REM Check if server is already running
if exist "%PID_FILE%" (
    echo [WARN] Server might be running. PID file exists.
    echo [INFO] If you're sure the server is not running, delete %PID_FILE% and try again.
    pause
    exit /b 1
)

REM Build the project
echo [INFO] Building project...
call mvn clean package -DskipTests

if errorlevel 1 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

echo [INFO] Build completed successfully

REM Create necessary directories
echo [INFO] Setting up directories...
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%USERPROFILE%\mcp-files\samples" mkdir "%USERPROFILE%\mcp-files\samples"
echo [INFO] Directories created

REM Start the server
echo [INFO] Starting MCP Java Server...

set JAR_PATH=target\%JAR_NAME%

if not exist "%JAR_PATH%" (
    echo [ERROR] JAR file not found: %JAR_PATH%
    echo [INFO] Please build the project first
    pause
    exit /b 1
)

REM Start the server in a new window
start "MCP Java Server" /min java %JAVA_OPTS% -jar "%JAR_PATH%"

REM Wait a moment for the server to start
timeout /t 3 /nobreak >nul

echo [INFO] Server startup initiated
echo [INFO] Log files:
echo [INFO]   - Application log: %LOG_DIR%\mcp-server.log
echo [INFO]   - Check the server window for startup messages
echo [INFO]
echo [INFO] To stop the server, close the server window or use Ctrl+C in the server window
echo [INFO] To view logs, open: %LOG_DIR%\mcp-server.log

echo [INFO] Server startup script completed
pause