$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path "artifacts" | Out-Null

$pidLine = jps -l | Select-String "ife-correlated-vulnerable-app"
if (-not $pidLine) {
    Write-Error "Application PID not found. Run: jps -l"
}

$appPid = ($pidLine.ToString().Trim() -split "\s+")[0]
Write-Host "Detected Target App PID: $appPid"

Write-Host "Capturing Thread Dump to artifacts/ThreadDump-correlated.jstack..."
jstack $appPid | Out-File -Encoding ascii "artifacts/ThreadDump-correlated.jstack"

Write-Host "Capturing Heap Dump to artifacts/heapdump-correlated.hprof..."
if (Test-Path "artifacts/heapdump-correlated.hprof") {
    Remove-Item -Force "artifacts/heapdump-correlated.hprof"
}
jmap -dump:live,format=b,file="artifacts/heapdump-correlated.hprof" $appPid

if (Test-Path "logs/ife-application.log") {
    Copy-Item "logs/ife-application.log" "artifacts/application-correlated.log" -Force
}

Write-Host "Successfully generated correlated artifacts under artifacts/:"
Get-ChildItem artifacts
