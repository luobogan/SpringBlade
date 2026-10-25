param([string]$Pattern = 'Started WorkflowApplication|ERROR|Exception', [int]$Tail = 15)
$files = @('C:\wfobs\wf_esign.log', 'C:\wfobs\wf_esign.err.log')
foreach ($f in $files) {
    if (Test-Path $f) {
        Write-Output "===== $f ====="
        Select-String -Path $f -Pattern $Pattern | Select-Object -Last $Tail | ForEach-Object {
            Write-Output ("{0}  {1}" -f $_.LineNumber, $_.Line)
        }
    }
}
