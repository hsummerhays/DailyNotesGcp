# Container integration test script for Windows/PowerShell
Write-Host "Starting integration test for containers..." -ForegroundColor Cyan

# Bring up the stack
Write-Host "Building and starting containers..." -ForegroundColor Green
docker compose down -v
docker compose up -d --build

# Wait and poll backend health
$retries = 20
$backendReady = $false
Write-Host "Polling backend health endpoint (http://127.0.0.1:8085/actuator/health)..." -ForegroundColor Yellow

for ($i = 1; $i -le $retries; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "http://127.0.0.1:8085/actuator/health" -Method Get -TimeoutSec 3
        if ($response.status -eq "UP") {
            Write-Host "Backend is UP and healthy!" -ForegroundColor Green
            $backendReady = $true
            break
        }
    }
    catch {
        # Keep waiting
    }
    Write-Host "Backend not ready yet, retrying in 5 seconds ($i/$retries)..."
    Start-Sleep -Seconds 5
}

# Wait and poll frontend health
$frontendReady = $false
if ($backendReady) {
    Write-Host "Polling frontend service (http://127.0.0.1:8086/health)..." -ForegroundColor Yellow
    for ($i = 1; $i -le 5; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:8086/health" -Method Get -TimeoutSec 3 -UseBasicParsing
            if ($response.StatusCode -eq 200) {
                Write-Host "Frontend is responding with 200 OK at /health!" -ForegroundColor Green
                $frontendReady = $true
                break
            }
        }
        catch {
            # Keep waiting
        }
        Write-Host "Frontend not ready yet, retrying in 2 seconds ($i/5)..."
        Start-Sleep -Seconds 2
    }
}

# Wait and poll worker health
$workerReady = $false
if ($backendReady) {
    Write-Host "Polling import-worker service (http://127.0.0.1:8087/actuator/health)..." -ForegroundColor Yellow
    for ($i = 1; $i -le 10; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "http://127.0.0.1:8087/actuator/health" -Method Get -TimeoutSec 3
            if ($response.status -eq "UP") {
                Write-Host "Import worker is UP and healthy!" -ForegroundColor Green
                $workerReady = $true
                break
            }
        }
        catch {
            # Keep waiting
        }
        Write-Host "Worker not ready yet, retrying in 3 seconds ($i/10)..."
        Start-Sleep -Seconds 3
    }
}

# Tear down the stack
Write-Host "Tearing down containers..." -ForegroundColor Green
docker compose down -v

# Evaluate success
if ($backendReady -and $frontendReady -and $workerReady) {
    Write-Host "Integration tests PASSED successfully!" -ForegroundColor Green
    exit 0
} else {
    Write-Error "Integration tests FAILED!"
    exit 1
}
