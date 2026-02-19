# Builds the Kestra Docker image with all open-source plugins pre-installed.
# Requires: Docker, Gradle. Optional: curl (or PowerShell will use Invoke-RestMethod).
# Usage: .\build-docker-with-plugins.ps1 [image:tag]

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# Version from gradle.properties (strip -SNAPSHOT for API)
$versionLine = Get-Content gradle.properties | Where-Object { $_ -match "^version=" }
$version = ($versionLine -replace "version=", "" -replace "-SNAPSHOT", "").Trim()
if (-not $version) {
    Write-Error "Could not read version from gradle.properties"
    exit 1
}

$imageTag = $args[0]
if (-not $imageTag) { $imageTag = "kestra/kestra:${version}-with-plugins" }

Write-Host "Building Kestra with plugins (version=$version, image=$imageTag)"

Write-Host "Fetching plugin list from Kestra API..."
try {
    $response = Invoke-RestMethod -Uri "https://api.kestra.io/v1/plugins/artifacts/core-compatibility/$version/latest" -Method Get
} catch {
    Write-Warning "No plugin list from API for $version; building image without plugins."
    $response = $null
}

$kestraPlugins = ""
if ($response) {
    $list = $response | Where-Object {
        $_.license -eq "OPEN_SOURCE" -and
        $_.groupId -ne "io.kestra.plugin.ee" -and
        $_.groupId -ne "io.kestra.ee.secret"
    } | ForEach-Object { "$($_.groupId):$($_.artifactId):$($_.version)" }
    $kestraPlugins = ($list -join " ")
}
if (-not $kestraPlugins) {
    Write-Warning "No open-source plugins found for $version; building without plugins."
} else {
    Write-Host "Plugins will be installed into image."
}

Write-Host "Building executable JAR..."
& .\gradlew.bat -q executableJar --no-daemon

Write-Host "Preparing docker/app..."
New-Item -ItemType Directory -Force -Path docker\app | Out-Null
$jar = Get-ChildItem build\executable\kestra* -File | Select-Object -First 1
if ($jar) { Copy-Item $jar.FullName docker\app\kestra -Force }

Write-Host "Building Docker image..."
$env:DOCKER_BUILDKIT = "1"
& docker build `
    --build-arg "KESTRA_PLUGINS=$kestraPlugins" `
    --build-arg "APT_PACKAGES=python3 python-is-python3 python3-pip curl jattach" `
    --build-arg "PYTHON_LIBRARIES=kestra" `
    -t $imageTag `
    -f Dockerfile .

Write-Host "Done. Run with: docker run -p 8080:8080 -v kestra_data:/app/storage $imageTag server local"
Write-Host "Image: $imageTag"
