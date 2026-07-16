# Container integration test script for Windows/PowerShell
Write-Host "Starting integration test for containers..." -ForegroundColor Cyan

# Bring up the stack
Write-Host "Building and starting containers..." -ForegroundColor Green
docker compose down -v
docker compose up -d --build

# Wait and poll backend health
$retries = 20
$backendReady = $false
Write-Host "Polling backend health endpoint (http://localhost:8080/actuator/health)..." -ForegroundColor Yellow

for ($i = 1; $i -le $retries; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 3
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
    Write-Host "Polling frontend service (http://localhost/)..." -ForegroundColor Yellow
    for ($i = 1; $i -le 5; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost/" -Method Get -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                Write-Host "Frontend is responding with 200 OK!" -ForegroundColor Green
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

# Tear down the stack
Write-Host "Tearing down containers..." -ForegroundColor Green
docker compose down -v

# Evaluate success
if ($backendReady -and $frontendReady) {
    Write-Host "Integration tests PASSED successfully!" -ForegroundColor Green
    exit 0
} else {
    Write-Error "Integration tests FAILED!"
    exit 1
}
