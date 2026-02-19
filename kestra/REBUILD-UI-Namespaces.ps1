# Reconstruir a UI para incluir as funcionalidades Enterprise de Namespaces
# (Criar namespace + Acesso por utilizador). A UI é copiada para o webserver.
# Depois de executar, reinicie a aplicação (runLocal ou o servidor que usa).

Set-Location $PSScriptRoot

Write-Host "A construir a UI (override com Namespaces Enterprise)..." -ForegroundColor Cyan
& ./gradlew.bat :ui:assembleFrontend --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host "UI construida com sucesso. A UI foi copiada para webserver/src/main/resources/ui" -ForegroundColor Green
    Write-Host "Reinicie a aplicacao (ex: .\gradlew.bat runLocal) para ver o botao 'Criar namespace' e a tab Acesso." -ForegroundColor Yellow
} else {
    Write-Host "Erro na construcao da UI." -ForegroundColor Red
    exit 1
}
