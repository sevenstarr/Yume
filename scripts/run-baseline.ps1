#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$SdkRoot,
    [Parameter(Mandatory)][string]$GradleUserHome,
    [ValidateRange(1, 32)][int]$MaxWorkers = 2,
    [string[]]$Tasks = @(':app:assembleDebug', 'testDebugUnitTest', 'spotlessCheck', 'verifySqlDelightMigration', ':app:lintDebug'),
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repository = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'JDK not found' }
if (-not (Test-Path -LiteralPath $SdkRoot)) { throw 'SDK directory not found' }
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $repository ('build/baseline/' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$OutputDirectory = (Resolve-Path -LiteralPath $OutputDirectory).Path
$environmentNames = @('JAVA_HOME', 'ANDROID_HOME', 'ANDROID_SDK_ROOT', 'GRADLE_USER_HOME', 'ANDROID_USER_HOME')
$previous = @{}
foreach ($name in $environmentNames) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$results = [Collections.Generic.List[object]]::new()
$failed = $false
Push-Location $repository
try {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:ANDROID_HOME = (Resolve-Path -LiteralPath $SdkRoot).Path
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    $env:GRADLE_USER_HOME = [IO.Path]::GetFullPath($GradleUserHome)
    $env:ANDROID_USER_HOME = Join-Path $env:GRADLE_USER_HOME 'android-user'
    $revision = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read Git revision' }
    foreach ($task in $Tasks) {
        $label = $task -replace '[^A-Za-z0-9_-]', '_'
        $log = Join-Path $OutputDirectory ($label + '.log')
        $started = [DateTimeOffset]::UtcNow
        $timer = [Diagnostics.Stopwatch]::StartNew()
        & .\gradlew.bat $task --console=plain --stacktrace "--max-workers=$MaxWorkers" *> $log
        $code = $LASTEXITCODE
        $timer.Stop()
        $results.Add([pscustomobject]@{
            task = $task; revision = $revision; startedUtc = $started.ToString('o')
            seconds = [Math]::Round($timer.Elapsed.TotalSeconds, 3); exitCode = $code; maxWorkers = $MaxWorkers
            log = $log
        })
        $results | ConvertTo-Json -Depth 5 -AsArray | Set-Content -LiteralPath (Join-Path $OutputDirectory 'results.json') -Encoding utf8
        Write-Output "$task : exit $code ($([Math]::Round($timer.Elapsed.TotalSeconds, 1)) s)"
        Get-Content -LiteralPath $log -Tail 12
        if ($code -ne 0) { $failed = $true }
    }
} finally {
    foreach ($name in $environmentNames) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    Pop-Location
}
if ($failed) { exit 1 }
