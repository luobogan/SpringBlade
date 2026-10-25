$ErrorActionPreference = 'Continue'
$root = 'D:\workproject\springbladeandreact\springBlade'
$logDir = 'C:\wfobs'
$jar = "$root\blade-service\blade-workflow\target\blade-workflow.jar"

# 1) 停止旧实例
$old = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*blade-workflow.jar*' }
foreach ($p in $old) {
    Write-Output "stopping PID=$($p.ProcessId)"
    Stop-Process -Id $p.ProcessId -Force
}
Start-Sleep -Seconds 3

# 2) 重新打包
Set-Location $root
& mvn -o "-Dmaven.legacyLocalRepo=true" -pl blade-service/blade-workflow package -DskipTests *> "$logDir\wf_esign_build.log"
if ($LASTEXITCODE -ne 0) {
    Write-Output "BUILD FAILED, exit=$LASTEXITCODE"
    Get-Content "$logDir\wf_esign_build.log" -Tail 40
    exit 1
}
Write-Output "BUILD OK"

# 3) 启动
$proc = Start-Process -FilePath 'D:\project\weaver\jdk21\bin\java.exe' `
    -ArgumentList '-Xms512m', '-Xmx2048m', '-jar', $jar, '--spring.profiles.active=dev' `
    -RedirectStandardOutput "$logDir\wf_esign.log" `
    -RedirectStandardError "$logDir\wf_esign.err.log" `
    -WindowStyle Hidden -PassThru
Write-Output "started PID=$($proc.Id)"
