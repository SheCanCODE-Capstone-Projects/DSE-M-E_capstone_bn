# PowerShell script to switch to Java 17 for this project
# Run this script before compiling/running the project

Write-Host "Switching to Java 17..." -ForegroundColor Yellow

$java17Path = "C:\Program Files\Java\jdk-17"

if (Test-Path $java17Path) {
    $env:JAVA_HOME = $java17Path
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    
    Write-Host "✅ Java 17 activated!" -ForegroundColor Green
    Write-Host "Java version:" -ForegroundColor Cyan
    java -version
    Write-Host "`nMaven Java version:" -ForegroundColor Cyan
    mvn -version | Select-String "Java version"
    
    Write-Host "`n⚠️  Note: This change is only for this PowerShell session." -ForegroundColor Yellow
    Write-Host "To make it permanent, set JAVA_HOME system environment variable." -ForegroundColor Yellow
} else {
    Write-Host "❌ Java 17 not found at: $java17Path" -ForegroundColor Red
    Write-Host "Please install Java 17 first." -ForegroundColor Red
    Write-Host "Download from: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Cyan
}
