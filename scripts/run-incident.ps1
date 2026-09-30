param(
    [int]$Port = 8088,
    [int]$MemoryIterations = 15,
    [int]$MemoryMb = 10,
    [int]$WaitingThreads = 40,
    [int]$DbRequests = 20,
    [int]$DbSleepMs = 30000,
    [int]$DeadloadPayloadMb = 10,
    [int]$ThreadLocalMb = 10,
    [int]$QueueTasks = 15,
    [int]$QueueTaskMb = 5
)

$baseUrl = "http://localhost:$Port"

Write-Host "--- Starting Incident Generation on $baseUrl ---"

Write-Host "1. Generating static memory retention ($MemoryIterations x ${MemoryMb}MB)..."
1..$MemoryIterations | ForEach-Object {
    Invoke-RestMethod -Method Post -Uri "$baseUrl/incident/memory-leak?megabytes=$MemoryMb" | Out-Null
}

Write-Host "2. Generating $WaitingThreads waiting worker threads (5 min wait)..."
Invoke-RestMethod -Method Post -Uri "$baseUrl/incident/waiting-threads?count=$WaitingThreads&waitMs=300000" | Out-Null

Write-Host "3. Generating multithreaded Deadlock (IFE-DeadlockWorker-A <-> IFE-DeadlockWorker-B)..."
Invoke-RestMethod -Method Post -Uri "$baseUrl/incident/deadlock?payloadMb=$DeadloadPayloadMb" | Out-Null

Write-Host "4. Generating ThreadLocal memory retention across pool worker threads (8 x ${ThreadLocalMb}MB)..."
Invoke-RestMethod -Method Post -Uri "$baseUrl/incident/threadlocal-leak?megabytes=$ThreadLocalMb" | Out-Null

Write-Host "5. Generating ThreadPool queue backlog saturation with heavy tasks ($QueueTasks x ${QueueTaskMb}MB)..."
Invoke-RestMethod -Method Post -Uri "$baseUrl/incident/queue-saturation?count=$QueueTasks&megabytes=$QueueTaskMb" | Out-Null

Write-Host "6. Generating $DbRequests concurrent DB contention requests..."
1..$DbRequests | ForEach-Object {
    Start-Job -ScriptBlock {
        param($url)
        try { Invoke-RestMethod -Method Post -Uri $url } catch {}
    } -ArgumentList "$baseUrl/incident/db-contention?sleepMs=$DbSleepMs" | Out-Null
}

$status = Invoke-RestMethod "$baseUrl/incident/status"
Write-Host "Incident workload started successfully."
Write-Host "Current status: $($status | ConvertTo-Json -Compress)"
Write-Host "Now run scripts/capture-dumps.ps1 to generate ThreadDump and HeapDump!"
