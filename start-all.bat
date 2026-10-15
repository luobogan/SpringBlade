@echo off
REM ============================================================================
REM  一键拉起 SpringBlade 全套环境 + 前端（双击运行，不依赖任何命令会话）
REM
REM  背景：此前用 PowerShell Start-Process 起的服务会随命令会话结束被回收，
REM       导致 blade-system / blade-workflow / blade-message 莫名全部消失
REM       （表现：页面 404、/param/public-value 404 等）。
REM       本脚本改为由各自独立的 cmd 窗口承载进程，双击运行后即常驻。
REM
REM  启动顺序（依赖次序，不可颠倒）：
REM     1) Nacos(8848)  —— 注册中心/配置中心，必须最先就绪
REM     2) Redis(6379)  —— 缓存
REM     3) 后端微服务   —— blade-auth / gateway / system / workflow / message / formmode
REM     4) 前端(8000)   —— 重启（先杀掉占用 8000 的旧进程再拉起）
REM
REM  行为：启动前先探端口 —— 已在监听的组件直接跳过，不会重复启动/端口冲突。
REM       所有日志（后端各服务 + 前端 + Nacos）统一落到 D:\workproject\springbladeandreact\log。
REM ============================================================================
setlocal EnableDelayedExpansion

set "JAVA=D:\project\weaver\jdk21\bin\java.exe"
set "ROOT=D:\workproject\springbladeandreact\springBlade"

REM ---- 中间件与前端路径 ----
set "NACOS_JAVA_HOME=D:\project\weaver\JDK17"
set "NACOS_BIN=D:\project\nacos\bin"
set "REDIS_DIR=D:\project\Redis-x64-3.2.100"
set "FRONTEND_DIR=D:\workproject\springbladeandreact\ant-design-pro"
REM ---- 统一日志目录（后端各服务 + 前端 + Nacos 全部落这里）----
set "LOG_DIR=D:\workproject\springbladeandreact\log"
set "FRONTEND_LOG=%LOG_DIR%\frontend.log"

REM ---- Maven build switch (rebuild jars before starting backend) ----
REM   DO_BUILD=1  rebuild via Maven before start (fresh jars); =0 skip and run old jars
REM   MVN_OPTS   build opts: -B batch mode, -DskipTests skip tests
REM   BUILD_MODULES  modules to build (nested blade-service modules use / )
set "DO_BUILD=1"
set "MVN_OPTS=-B -DskipTests"
set "BUILD_MODULES=blade-auth,blade-gateway,blade-service/blade-system,blade-service/blade-workflow,blade-service/blade-message,blade-service/blade-formmode"

if not exist "%JAVA%" (
  echo [ERROR] java not found: %JAVA%
  pause
  exit /b 1
)

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ===== SpringBlade full startup =====
echo.

REM ============================================================================
REM 1) Nacos（standalone）—— 需 JDK17，就绪后才继续（否则微服务注册/拉配置会失败）
REM ============================================================================
REM [临时禁用] Nacos 启动段 —— 恢复方法：去掉下面各行行首的 "REM " 即可
REM echo ---- [1/4] Nacos ----
REM netstat -ano | findstr "LISTENING" | findstr /C:":8848 " >nul
REM if %errorlevel%==0 (
REM   echo [nacos] already listening on 8848, skip.
REM ) else (
REM   if not exist "%NACOS_BIN%\startup.cmd" (
REM     echo [nacos] startup.cmd NOT found: %NACOS_BIN%\startup.cmd
REM     echo [nacos] WARNING: Nacos not started, later services may fail to register.
REM   ) else (
REM     echo [nacos] starting standalone with JAVA_HOME=%NACOS_JAVA_HOME% ...
REM     REM JAVA_HOME 必须设在「本脚本」环境里让子 cmd 继承：
REM     REM 若写成 start ... cmd /c "set JAVA_HOME=... && startup.cmd"，子进程的 %JAVA_HOME% 展开不到，
REM     REM startup.cmd 会直接报 "Please set the JAVA_HOME variable" 并退出。
REM     set "JAVA_HOME=%NACOS_JAVA_HOME%"
REM     start "nacos" /min cmd /c "cd /d %NACOS_BIN% && startup.cmd -m standalone > %LOG_DIR%\nacos_start.log 2>&1"
REM     echo [nacos] waiting for 8848 to be ready ^(max ~60s^) ...
REM     set "NACOS_READY=0"
REM     for /L %%i in (1,1,20) do (
REM       if !NACOS_READY!==0 (
REM         ping -n 4 127.0.0.1 >nul
REM         netstat -ano | findstr "LISTENING" | findstr /C:":8848 " >nul
REM         if !errorlevel!==0 (
REM           set "NACOS_READY=1"
REM           echo [nacos] ready on 8848.
REM         )
REM       )
REM     )
REM     if !NACOS_READY!==0 echo [nacos] WARNING: 8848 still not listening, continue anyway.
REM   )
REM )
echo.

REM ============================================================================
REM 2) Redis
REM ============================================================================
REM [临时禁用] Redis 启动段 —— 恢复方法：去掉下面各行行首的 "REM " 即可
REM echo ---- [2/4] Redis ----
REM netstat -ano | findstr "LISTENING" | findstr /C:":6379 " >nul
REM if %errorlevel%==0 (
REM   echo [redis] already listening on 6379, skip.
REM ) else (
REM   if not exist "%REDIS_DIR%\redis-server.exe" (
REM     echo [redis] redis-server.exe NOT found: %REDIS_DIR%
REM   ) else (
REM     echo [redis] starting ...
REM     start "redis" /min cmd /c "cd /d %REDIS_DIR% && redis-server.exe redis.windows.conf"
REM     ping -n 4 127.0.0.1 >nul
REM   )
REM )
echo.

REM ============================================================================
REM 3) 后端微服务
REM ============================================================================
echo ---- [3/4] backend services ----
REM rebuild before start if DO_BUILD=1 (see switch at top)
if "%DO_BUILD%"=="1" (
  call :stop
  call :build
)
REM           名称            端口   jar 路径                                    日志路径(统一到 %LOG_DIR%)
call :start blade-auth      8100  "%ROOT%\blade-auth\target\blade-auth.jar"                    "%LOG_DIR%\blade-auth.log"
call :start blade-gateway    81   "%ROOT%\blade-gateway\target\blade-gateway.jar"              "%LOG_DIR%\blade-gateway.log"
call :start blade-system    8106  "%ROOT%\blade-service\blade-system\target\blade-system.jar" "%LOG_DIR%\blade-system.log"
call :start blade-workflow  8111  "%ROOT%\blade-service\blade-workflow\target\blade-workflow.jar" "%LOG_DIR%\blade-workflow.log"
call :start blade-message   8130  "%ROOT%\blade-service\blade-message\target\blade-message.jar"   "%LOG_DIR%\blade-message.log"
call :start blade-formmode  8400  "%ROOT%\blade-service\blade-formmode\target\blade-formmode.jar" "%LOG_DIR%\blade-formmode.log"

echo.
echo ===== waiting for backend startup ^(45s^) =====
REM 用 ping 做等待而非 timeout：后者在 stdin 被重定向时会直接
REM "Input redirection is not supported" 退出，导致看不到末尾的端口核验结果。
ping -n 46 127.0.0.1 >nul

REM ============================================================================
REM 4) 前端（重启：先杀掉占用 8000 的旧进程）
REM ============================================================================
REM [临时禁用] 前端重启段 —— 恢复方法：去掉下面各行行首的 "REM " 即可
REM echo.
REM echo ---- [4/4] frontend restart ----
REM set "FE_PID="
REM for /f "tokens=5" %%a in ('netstat -ano ^| findstr "LISTENING" ^| findstr /C:":8000 "') do (
REM   if not defined FE_PID set "FE_PID=%%a"
REM )
REM if defined FE_PID (
REM   echo [frontend] killing old process PID=!FE_PID! ...
REM   taskkill /F /T /PID !FE_PID! >nul 2>&1
REM   ping -n 4 127.0.0.1 >nul
REM ) else (
REM   echo [frontend] no process on 8000.
REM )
REM where npm >nul 2>&1
REM if %errorlevel%==0 (
REM   echo [frontend] starting npm run dev ...
REM   start "frontend" /min cmd /c "cd /d %FRONTEND_DIR% && npm run dev > %FRONTEND_LOG% 2>&1"
REM ) else (
REM   echo [frontend] WARNING: npm not found on PATH, skip frontend.
REM )
echo.

echo.
echo ===== listening ports =====
netstat -ano | findstr "LISTENING" | findstr ":8848 :6379 :81 :8000 :8100 :8106 :8111 :8130 :8400"
echo.
echo done.
pause
exit /b 0

REM ---------------------------------------------------------------------------
:stop
echo [stop] stopping running backend services to release jar locks before build ...
for %%p in (8100 81 8106 8111 8130 8400) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr "LISTENING" ^| findstr /C:":%%p "') do (
    echo [stop] killing PID %%a on port %%p
    taskkill /F /T /PID %%a >nul 2>&1
  )
)
ping -n 4 127.0.0.1 >nul
exit /b 0

REM ---------------------------------------------------------------------------
:build
echo [build] rebuilding backend jars via Maven ^(DO_BUILD=1^) ...
where mvn >nul 2>&1
if %errorlevel%==0 (set "MAVEN=mvn") else (set "MAVEN=")
if not defined MAVEN (
  if exist "%ROOT%\mvnw.cmd" (set "MAVEN=%ROOT%\mvnw.cmd")
)
if not defined MAVEN (
  echo [build] ERROR: maven not found on PATH and no %ROOT%\mvnw.cmd
  pause
  exit /b 1
)
pushd "%ROOT%"
echo [build] cmd: %MAVEN% -pl %BUILD_MODULES% -am %MVN_OPTS% package
REM IMPORTANT: must use call here. mvn is mvn.cmd (a batch file). Calling it directly
REM inside a "call :build" context corrupts the call stack and cmd reports
REM "cannot find batch label - build" when mvn.cmd exits. call ensures return.
call "%MAVEN%" -pl %BUILD_MODULES% -am %MVN_OPTS% package
set "BUILD_RC=%errorlevel%"
popd
if not "%BUILD_RC%"=="0" (
  echo [build] ERROR: Maven build failed ^(rc=%BUILD_RC%^). Jars NOT updated.
  pause
  exit /b 1
)
echo [build] done. fresh jars regenerated under each module's target/.
exit /b 0

REM ---------------------------------------------------------------------------
:start
set "SVC=%~1"
set "PORT=%~2"
set "JAR=%~3"
set "LOG=%~4"

REM DO_BUILD=1: force restart - kill old process on this port first so the fresh jar
REM takes effect. Otherwise :start would skip (port already listening) and the old jar
REM (without new endpoints) keeps serving, e.g. 404 after code changes.
if "%DO_BUILD%"=="1" (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr "LISTENING" ^| findstr /C:":%PORT% "') do (
    echo [%SVC%] killing old process on port %PORT% ^(PID %%a^) ...
    taskkill /F /T /PID %%a >nul 2>&1
  )
  ping -n 3 127.0.0.1 >nul
) else (
  netstat -ano | findstr "LISTENING" | findstr /C:":%PORT% " >nul
  if %errorlevel%==0 (
    echo [%SVC%] already listening on %PORT%, skip.
    exit /b 0
  )
)

if not exist "%JAR%" (
  echo [%SVC%] jar NOT found, skip: %JAR%
  exit /b 0
)

echo [%SVC%] starting on port %PORT% ...
start "%SVC%" /min cmd /c ""%JAVA%" -Xms512m -Xmx2048m -jar "%JAR%" --spring.profiles.active=dev > "%LOG%" 2>&1"
exit /b 0
