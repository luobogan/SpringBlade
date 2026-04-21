# 批量修改包路径的PowerShell脚本
$sourceDir = "D:\workproject\springbladeandreact\springBlade\src\main\java\org\springblade\modules\system"

# 递归获取所有Java文件
$javaFiles = Get-ChildItem -Path $sourceDir -Recurse -Include "*.java"

# 遍历所有Java文件
foreach ($file in $javaFiles) {
    Write-Host "处理文件: $($file.FullName)"

    # 读取文件内容
    $content = Get-Content -Path $file.FullName -Encoding UTF8 -Raw

    # 替换包路径
    $newContent = $content -replace "package org\.springblade\.system\.([a-z]+);", "package org.springblade.modules.system.$1;"

    # 替换导入路径
    $newContent = $newContent -replace "import org\.springblade\.system\.([a-z]+)\.([a-zA-Z0-9]+);", "import org.springblade.modules.system.$1.$2;"

    # 写入修改后的内容
    Set-Content -Path $file.FullName -Value $newContent -Encoding UTF8
}

Write-Host "包路径修改完成！"
