# Run this script as Administrator to add hostname entries
# Right-click PowerShell and select "Run as Administrator"

$hostsFile = "C:\Windows\System32\drivers\etc\hosts"

$entries = @"

# Keycloak Demo Hostnames
127.0.0.1 auth.keycloak-demo.local
127.0.0.1 app.keycloak-demo.local
127.0.0.1 backend.keycloak-demo.local
"@

$currentContent = Get-Content $hostsFile -Raw -ErrorAction SilentlyContinue

if ($currentContent -notmatch "auth.keycloak-demo.local") {
    Add-Content -Path $hostsFile -Value $entries
    Write-Host "✓ Added hostname entries to hosts file" -ForegroundColor Green
    Write-Host "`nYou can now access:" -ForegroundColor Cyan
    Write-Host "  Frontend:  http://app.keycloak-demo.local:8080" -ForegroundColor Yellow
    Write-Host "  Keycloak:  http://auth.keycloak-demo.local:8082" -ForegroundColor Yellow
    Write-Host "  Backend:   http://backend.keycloak-demo.local:8081" -ForegroundColor Yellow
} else {
    Write-Host "✓ Hostname entries already exist in hosts file" -ForegroundColor Green
}

Write-Host "`nPress any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
