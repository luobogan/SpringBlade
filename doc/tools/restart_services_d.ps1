# 把运行中的 SpringBlade 服务从 E:\project\... 切到 D:\project\springbladeandreact\...
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File restart_services_d.ps1 [-IncludeWorkflow]
param(
    [switch]$IncludeWorkflow
)

$ErrorActionPreference = 'Continue'
$base = 'D:\project\springbladeandreact\SpringBlade'
$sysJava = 'D:\project\weaver\jdk\bin\java.exe'

Write-Host '== 1. 停止仍在 E 盘运行的旧服务进程 =='
Get-CimInstance Win32_Process -Filter "Name like '%java%'" |
    Where-Object { $_.CommandLine -like '*E:\project\springbladeandreact*' } |
    ForEach-Object {
        Write-Host "  STOP pid=$($_.ProcessId)"
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

Start-Sleep -Seconds 3

function Start-Svc {
    param($name, $jarRel, $java)
    $jar = Join-Path $base $jarRel
    if (-not (Test-Path $jar)) {
        Write-Host "  SKIP $name : jar 不存在 -> $jar"
        return
    }
    $out = Join-Path $base "$name-d.log"
    $err = Join-Path $base "$name-d.err.log"
    Start-Process -FilePath $java `
        -ArgumentList @('-Xms512m', '-Xmx1024m', '-jar', $jar, '--spring.profiles.active=dev') `
        -WorkingDirectory $base `
        -RedirectStandardOutput $out `
        -RedirectStandardError $err `
        -WindowStyle Hidden
    Write-Host "  STARTED $name -> $out"
}

Write-Host '== 2. 从 D 盘 jar 启动 =='
Start-Svc 'blade-auth'    'blade-auth\target\blade-auth.jar'                       'java'
Start-Svc 'blade-gateway' 'blade-gateway\target\blade-gateway.jar'                 'java'
Start-Svc 'blade-system'  'blade-service\blade-system\target\blade-system.jar'     $sysJava
if ($IncludeWorkflow) {
    Start-Svc 'blade-workflow' 'blade-service\blade-workflow\target\blade-workflow.jar' $sysJava
}

Write-Host '== 3. 等待端口就绪（最多 90s） =='
$ports = @{81 = 'gateway'; 8100 = 'auth'; 8106 = 'system'}
if ($IncludeWorkflow) { $ports[8107] = 'workflow' }
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    $all = $true
    foreach ($p in $ports.Keys) {
        $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
        if (-not $c) { $all = $false }
    }
    if ($all) { Write-Host 'ALL PORTS UP'; break }
}
foreach ($p in ($ports.Keys | Sort-Object)) {
    $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if ($c) { Write-Host ("  {0,-6} {1} : UP" -f $ports[$p], $p) }
    else { Write-Host ("  {0,-6} {1} : DOWN" -f $ports[$p], $p) }
}
Write-Host 'DONE'
