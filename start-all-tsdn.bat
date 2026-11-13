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
REM     3) 后端微服务   —— blade-auth / gateway / system / workflow / message / formmode / portal / resource(8010,对象存储)
REM     4) 前端(8000)   —— 重启（先杀掉占用 8000 的旧进程再拉起）
REM
REM  行为：启动前先探端口 —— 已在监听的组件直接跳过，不会重复启动/端口冲突。
REM       所有日志（后端各服务 + 前端 + Nacos）统一落到 D:\workproject\springbladeandreact\log。
REM ============================================================================
setlocal EnableDelayedExpansion

set "JAVA=D:\project\weaver\jdk21\bin\java.exe"
REM Maven 构建统一固定 JDK21（系统全局 JAVA_HOME=jdk25 对 Maven 无效，避免双击运行时环境差异导致构建失败）
set "JAVA_HOME=D:\project\weaver\jdk21"
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
set "BUILD_MODULES=blade-auth,blade-gateway,blade-service/blade-system,blade-service/blade-workflow,blade-service/blade-message,blade-service/blade-formmode,blade-service/blade-portal,blade-service/blade-doc,blade-ops/blade-resource"
REM ---- core/starter 源头仓（org.springblade.core.* 构件来源，含 blade-core-boot）----
REM   blade-tool 是独立的 Maven 工程，必须先 clean install 进本地仓库再构建本工程：
REM   否则 .m2 里的 blade-core-boot 若是旧构建，会缺 org.springblade.core.boot.file.service/dto/vo
REM   等新类，blade-portal/blade-workflow 编译即报「程序包 org.springblade.core.boot.file.service
REM   不存在 / 找不到符号 IFileStorageService」。
set "TOOL_ROOT=D:\workproject\blade-tool"

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
  if !errorlevel! neq 0 (
    echo [ERROR] 构建中止：服务进程未能停止，jar 仍被占用。请手动结束后重试。
    pause
    exit /b 1
  )
  call :checkjars
  if !errorlevel! neq 0 (
    echo [ERROR] 构建中止：仍有进程持有服务 jar。请结束后重试。
    pause
    exit /b 1
  )
  call :buildtool
  if !errorlevel! neq 0 (
    echo [ERROR] 构建中止：blade-tool^(core/starter^) 构建失败，.m2 中的核心构件不是最新，不启动服务。
    pause
    exit /b 1
  )
  call :build
  if !errorlevel! neq 0 (
    echo [ERROR] 构建中止：Maven 构建失败，不启动服务（避免旧 jar 冒充成功）。
    pause
    exit /b 1
  )
)
REM           名称            端口   jar 路径                                    日志路径(统一到 %LOG_DIR%)
call :start blade-auth      8100  "%ROOT%\blade-auth\target\blade-auth.jar"                    "%LOG_DIR%\blade-auth.log"
call :start blade-gateway    81   "%ROOT%\blade-gateway\target\blade-gateway.jar"              "%LOG_DIR%\blade-gateway.log"
call :start blade-system    8106  "%ROOT%\blade-service\blade-system\target\blade-system.jar" "%LOG_DIR%\blade-system.log"
call :start blade-workflow  8111  "%ROOT%\blade-service\blade-workflow\target\blade-workflow.jar" "%LOG_DIR%\blade-workflow.log"
call :start blade-message   8130  "%ROOT%\blade-service\blade-message\target\blade-message.jar"   "%LOG_DIR%\blade-message.log"
call :start blade-formmode  8400  "%ROOT%\blade-service\blade-formmode\target\blade-formmode.jar" "%LOG_DIR%\blade-formmode.log"
call :start blade-portal    8140  "%ROOT%\blade-service\blade-portal\target\blade-portal.jar"     "%LOG_DIR%\blade-portal.log"
call :start blade-resource  8010  "%ROOT%\blade-ops\blade-resource\target\blade-resource.jar"      "%LOG_DIR%\blade-resource.log"
call :start blade-doc       8120  "%ROOT%\blade-service\blade-doc\target\blade-doc.jar"          "%LOG_DIR%\blade-doc.log"

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
netstat -ano | findstr "LISTENING" | findstr ":8848 :6379 :81 :8000 :8100 :8106 :8111 :8130 :8140 :8400 :8010 :8120"
echo.
echo done.
pause
exit /b 0

REM ---------------------------------------------------------------------------
:stop
echo [stop] stopping running backend services to release jar locks before build ...
REM 1) 按端口杀（快速路径）
for %%p in (8100 81 8106 8111 8130 8140 8400 8010 8120) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr "LISTENING" ^| findstr /C:":%%p "') do (
    echo [stop] killing PID %%a on port %%p
    taskkill /F /T /PID %%a >nul 2>&1
  )
)
REM 2) 兜底 + 等待：杀掉所有仍持有服务 jar 的 java 进程，并等到它们真正退出。
REM    只发 taskkill 不够 —— JVM 被强杀后文件句柄释放有延迟，maven repackage 紧接着
REM    就会撞上 "Unable to rename ...jar to ...jar.original"，导致整个构建失败。
REM    这里限定 springBlade 路径下的 blade-*.jar，不会误杀 Nacos/其它 java。
powershell -NoProfile -Command "$m={ $_.Name -eq 'java.exe' -and $_.CommandLine -match 'springBlade' -and $_.CommandLine -match 'blade-[a-zA-Z-]+\.jar' }; $p=Get-CimInstance Win32_Process | Where-Object $m; if($p){ $p | ForEach-Object { Write-Host ('[stop] killing stale java PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue } }; $dl=(Get-Date).AddSeconds(30); while((Get-Date) -lt $dl){ $p=Get-CimInstance Win32_Process | Where-Object $m; if(-not $p){break}; Start-Sleep -Milliseconds 1500 }; if($p){ $p | ForEach-Object { Write-Host ('[stop] STILL RUNNING PID ' + $_.ProcessId) }; exit 1 }; Write-Host '[stop] all service java processes exited, jar handles released.'"
exit /b %errorlevel%

REM ---------------------------------------------------------------------------
:checkjars
REM 预检：是否仍有进程持有服务 jar（存在则 maven repackage 必然锁死）。
echo [check] verifying no process holds service jars ...
powershell -NoProfile -Command "$m={ $_.Name -eq 'java.exe' -and $_.CommandLine -match 'springBlade' -and $_.CommandLine -match 'blade-[a-zA-Z-]+\.jar' }; $p=Get-CimInstance Win32_Process | Where-Object $m; if($p){ $p | ForEach-Object { Write-Host ('[check] LOCKED: PID ' + $_.ProcessId + ' -> ' + $_.CommandLine) }; exit 1 }; Write-Host '[check] OK: no process holds service jars.'"
exit /b %errorlevel%

REM ---------------------------------------------------------------------------
:buildtool
REM core/starter 源头仓（blade-core-boot / blade-core-tool / blade-starter-* 等）：
REM 必须 clean install 进本地仓库，本工程才能解析到最新的 org.springblade.core.* 构件。
REM 典型故障：.m2 的 blade-core-boot 是旧 jar，缺 org.springblade.core.boot.file.service 等包，
REM blade-portal 编译报「程序包不存在 / 找不到符号 IFileStorageService」。
if not exist "%TOOL_ROOT%\pom.xml" (
  echo [buildtool] ERROR: blade-tool pom not found: %TOOL_ROOT%\pom.xml
  exit /b 1
)
set "MAVEN="
for /f "delims=" %%i in ('where mvn 2^>nul') do set "MAVEN=%%i"
if not defined MAVEN (
  echo [buildtool] ERROR: maven not found on PATH
  exit /b 1
)
echo [buildtool] installing blade-tool ^(core/starter^) into local repo ...
pushd "%TOOL_ROOT%"
echo [buildtool] cmd: %MAVEN% %MVN_OPTS% clean install
echo [buildtool] maven output: %LOG_DIR%\maven-tool-build.log
REM 同样必须用 call：mvn 是 mvn.cmd（批处理），直接调用会破坏 call 栈。
call "%MAVEN%" %MVN_OPTS% clean install > "%LOG_DIR%\maven-tool-build.log" 2>&1
set "TOOL_RC=%errorlevel%"
popd
if not "%TOOL_RC%"=="0" (
  echo [buildtool] ERROR: blade-tool build failed ^(rc=%TOOL_RC%^). See %LOG_DIR%\maven-tool-build.log
  exit /b 1
)
echo [buildtool] done. core/starter jars refreshed in local repo.
exit /b 0

REM ---------------------------------------------------------------------------
:build
echo [build] rebuilding backend jars via Maven ^(DO_BUILD=1^) ...
REM 必须取 where mvn 返回的完整路径：若直接 set MAVEN=mvn，下面 call "%MAVEN%" 会变成
REM call "mvn"，而 cmd 对带引号的名字不做 PATHEXT 扩展，找不到 mvn.cmd，
REM 导致构建静默失败（rc=1、无任何输出、报错信息绕过重定向），旧 jar 被继续使用。
set "MAVEN="
for /f "delims=" %%i in ('where mvn 2^>nul') do set "MAVEN=%%i"
if not defined MAVEN (
  if exist "%ROOT%\mvnw.cmd" (set "MAVEN=%ROOT%\mvnw.cmd")
)
if not defined MAVEN (
  echo [build] ERROR: maven not found on PATH and no %ROOT%\mvnw.cmd
  exit /b 1
)
pushd "%ROOT%"
echo [build] cmd: %MAVEN% -pl %BUILD_MODULES% -am %MVN_OPTS% clean package
echo [build] maven output: %LOG_DIR%\maven-build.log
REM IMPORTANT: must use call here. mvn is mvn.cmd (a batch file). Calling it directly
REM inside a "call :build" context corrupts the call stack and cmd reports
REM "cannot find batch label - build" when mvn.cmd exits. call ensures return.
call "%MAVEN%" -pl %BUILD_MODULES% -am %MVN_OPTS% clean package > "%LOG_DIR%\maven-build.log" 2>&1
set "BUILD_RC=%errorlevel%"
popd
if not "%BUILD_RC%"=="0" (
  echo [build] ERROR: Maven build failed ^(rc=%BUILD_RC%^). Jars NOT updated. See %LOG_DIR%\maven-build.log
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
