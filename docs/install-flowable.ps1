<#
 .SYNOPSIS
   把 Flowable 8.1.0 各模块的 jar + pom 连同父 POM 装进本地 .m2。
   .m2 中已存在对应制品时跳过（按 jar 文件名解析 artifactId/version 定位），可加 -Force 强制重装。
#>
param([switch]$Force)

$repo = "D:\workproject\springbladeandreact\flowable-engine"
$root = Join-Path $repo "pom.xml"
$localRepo = "E:\project\mavenLib"                       # Maven 本地仓库根
$m2        = Join-Path $localRepo "org\flowable"         # Flowable 制品所在目录

function Install-Artifact($jar) {
    $moduleDir = (Get-Item $jar).Directory.Parent.FullName
    $pomPath = Join-Path $moduleDir "pom.xml"
    if (-not (Test-Path $pomPath)) { Write-Host "SKIP(no pom): $jar"; return }

    # 由文件名推导 artifactId / version，定位 .m2 中已装制品
    $base = [System.IO.Path]::GetFileNameWithoutExtension($jar)  # flowable-foo-8.1.0-SNAPSHOT
    if ($base -notmatch '^(.*?)-(\d+\.\d+\.\d+(?:-SNAPSHOT)?)$') {
        Write-Host "SKIP(cannot parse): $jar"; return
    }
    $artifactId, $version = $Matches[1], $Matches[2]
    $target = Join-Path $m2 $artifactId $version "$artifactId-$version.jar"

    if (-not $Force -and (Test-Path $target)) { Write-Host "SKIP(already): $artifactId-$version"; return }

    & mvn install:install-file "-Dfile=$jar" "-DpomFile=$pomPath" "-DlocalRepositoryPath=$localRepo" 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { Write-Host "OK   : $artifactId-$version" } else { Write-Host "FAIL : $artifactId-$version (exit $LASTEXITCODE)" }
}

# 1) 父 POM（flowable-root）
$rootTarget = Join-Path $m2 "flowable-root" "8.1.0-SNAPSHOT" "flowable-root-8.1.0-SNAPSHOT.pom"
if (-not $Force -and (Test-Path $rootTarget)) {
    Write-Host "SKIP(already): flowable-root-8.1.0-SNAPSHOT"
} else {
    & mvn install:install-file "-Dfile=$root" "-DpomFile=$root" "-DlocalRepositoryPath=$localRepo" 2>&1 | Out-Null
    Write-Host ("PARENT exit={0}" -f $LASTEXITCODE)
}

# 2) 所有模块 jar + pom
Get-ChildItem -Path (Join-Path $repo "modules") -Recurse -Include "flowable-*.jar" |
    Where-Object { $_.Name -notmatch "sources|javadoc" } |
    ForEach-Object { Install-Artifact $_.FullName }

Write-Host "=== DONE ==="
