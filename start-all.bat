@echo off
REM ============================================================================
REM  一键拉起 SpringBlade 全部后端服务（双击运行，不依赖任何命令会话）
REM
REM  背景：此前用 PowerShell Start-Process 起的服务会随命令会话结束被回收，
REM       导致 blade-system / blade-workflow / blade-message 莫名全部消失
REM       （表现：页面 404、/param/public-value 404 等）。
REM       本脚本改为由各自独立的 cmd 窗口承载 java 进程，双击运行后即常驻。
REM
REM  行为：启动前先探端口 —— 已在监听的服务直接跳过，不会重复启动/端口冲突。
REM       日志统一落到各服务 target（或模块）目录下的 out.log。
REM ============================================================================
setlocal EnableDelayedExpansion

set "JAVA=D:\project\weaver\jdk\bin\java.exe"
set "ROOT=D:\project\springbladeandreact\SpringBlade"

if not exist "%JAVA%" (
  echo [ERROR] java not found: %JAVA%
  pause
  exit /b 1
)

echo ===== SpringBlade services startup =====
echo.

REM           名称            端口   jar 路径                                    日志路径
call :start blade-auth      8100  "%ROOT%\blade-auth\target\blade-auth.jar"           "%ROOT%\blade-auth\target\blade-auth.out.log"
call :start blade-gateway    81   "%ROOT%\blade-gateway\target\blade-gateway.jar"     "%ROOT%\blade-gateway\target\blade-gateway.out.log"
call :start blade-system    8106  "%ROOT%\blade-service\blade-system\target\blade-system.jar"       "%ROOT%\blade-service\blade-system\target\blade-system.out.log"
call :start blade-workflow  8111  "%ROOT%\blade-service\blade-workflow\target\blade-workflow.jar"   "%ROOT%\blade-service\blade-workflow\wf_stdout.log"
call :start blade-message   8130  "%ROOT%\blade-service\blade-message\target\blade-message.jar"     "%ROOT%\blade-service\blade-message\msg_stdout.log"

echo.
echo ===== waiting for startup ^(45s^) =====
REM 用 ping 做等待而非 timeout：后者在 stdin 被重定向时会直接
REM "Input redirection is not supported" 退出，导致看不到末尾的端口核验结果。
ping -n 46 127.0.0.1 >nul

echo.
echo ===== listening ports =====
netstat -ano | findstr "LISTENING" | findstr ":8100 :8106 :8111 :8130 :81 "
echo.
echo done.
pause
exit /b 0

REM ---------------------------------------------------------------------------
:start
set "SVC=%~1"
set "PORT=%~2"
set "JAR=%~3"
set "LOG=%~4"

netstat -ano | findstr "LISTENING" | findstr ":%PORT% " >nul
if %errorlevel%==0 (
  echo [%SVC%] already listening on %PORT%, skip.
  exit /b 0
)

if not exist "%JAR%" (
  echo [%SVC%] jar NOT found, skip: %JAR%
  exit /b 0
)

echo [%SVC%] starting on port %PORT% ...
start "%SVC%" /min cmd /c ""%JAVA%" -Xms512m -Xmx2048m -jar "%JAR%" --spring.profiles.active=dev > "%LOG%" 2>&1"
exit /b 0
