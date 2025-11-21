# PowerShell script to generate self-signed certificates for Nginx
# For development/demo purposes only

$certDir = ".\nginx\certs"
New-Item -ItemType Directory -Force -Path $certDir | Out-Null

Write-Host "Generating self-signed certificate..." -ForegroundColor Cyan

# Use Docker with OpenSSL to generate certificates
docker run --rm -v "${PWD}/nginx/certs:/certs" alpine/openssl req -x509 -nodes -days 365 -newkey rsa:2048 `
  -keyout /certs/privkey.pem `
  -out /certs/fullchain.pem `
  -subj "/C=US/ST=State/L=City/O=DemoOrg/CN=*.keycloak-demo.local" `
  -addext "subjectAltName=DNS:auth.keycloak-demo.local,DNS:app.keycloak-demo.local,DNS:backend.keycloak-demo.local"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Self-signed certificates generated in $certDir" -ForegroundColor Green
    Write-Host ""
    Write-Host "Certificates:" -ForegroundColor Yellow
    Write-Host "  - $certDir\fullchain.pem (public certificate)"
    Write-Host "  - $certDir\privkey.pem (private key)"
    Write-Host ""
    Write-Host "⚠️  Browser will show security warning (self-signed cert)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "For production, use Let's Encrypt (free):" -ForegroundColor Cyan
    Write-Host '  certbot certonly --webroot -w /var/www/certbot \' -ForegroundColor White
    Write-Host '    -d auth.yourdomain.com \' -ForegroundColor White
    Write-Host '    -d app.yourdomain.com \' -ForegroundColor White
    Write-Host '    -d backend.yourdomain.com' -ForegroundColor White
} else {
    Write-Host "✗ Certificate generation failed" -ForegroundColor Red
    exit 1
}
