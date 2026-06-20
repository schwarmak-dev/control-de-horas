# Script para subir Control de Horas a F-Droid
# Ejecutar paso a paso manualmente

Write-Host "=== PASO 1: Ir a GitLab y forkear fdroiddata ===" -ForegroundColor Cyan
Write-Host "1. Abre https://gitlab.com/fdroid/fdroiddata en tu navegador"
Write-Host "2. Click en Fork -> Fork project (a tu cuenta)"
Write-Host "3. Copia la URL de tu fork (ej: https://gitlab.com/TU_USUARIO/fdroiddata)"
Write-Host ""

$gitlabUser = Read-Host "Ingresa tu usuario de GitLab"
$forkUrl = "https://gitlab.com/$gitlabUser/fdroiddata.git"

Write-Host ""
Write-Host "=== PASO 2: Agregar tu fork como remote y pushear ===" -ForegroundColor Cyan

cd "$env:TEMP\fdroiddata"

# Add user's fork as remote
git remote add fork $forkUrl
git push fork control-de-horas

Write-Host ""
Write-Host "=== PASO 3: Crear Merge Request en GitLab ===" -ForegroundColor Cyan
Write-Host "1. Anda a https://gitlab.com/$gitlabUser/fdroiddata"
Write-Host "2. Deberia aparecer: 'You pushed to control-de-horas. Create Merge Request'"
Write-Host "3. Click ahi"
Write-Host "4. Titulo: New App: Control de Horas"
Write-Host "5. Descripcion: App Android nativa de control de horas para trabajo y practica profesional. Licencia MIT. Sin Google Services."
Write-Host "6. Click en Create Merge Request"
Write-Host ""
Write-Host "Y LISTO! Los maintainers de F-Droid lo revisaran. En 24-48h aprox tu app aparece en el repositorio." -ForegroundColor Green
