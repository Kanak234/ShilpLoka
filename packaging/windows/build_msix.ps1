# Builds ShilpLoka-<ver>.msix on Windows:  pwsh packaging\windows\build_msix.ps1
# FLOW: npm ci + vite build → cargo build --release (Tauri host) → smoke test (exe
#       starts and stays alive 10 s) → layout + manifest → makeappx → portable zip.
# Failures become GitHub annotations (readable on the run page without sign-in).
$ErrorActionPreference = "Stop"
$global:Tail = New-Object System.Collections.Generic.List[string]
function Run($step, [scriptblock]$cmd) {
  Write-Host "::group::$step"
  & $cmd 2>&1 | ForEach-Object { $l = "$_"; Write-Host $l; $global:Tail.Add($l); if ($global:Tail.Count -gt 25) { $global:Tail.RemoveAt(0) } }
  $code = $LASTEXITCODE; Write-Host "::endgroup::"
  if ($code -ne 0) { throw "STEP FAILED: $step (exit $code)" }
}
trap { Write-Host ("::error title=MSIX build failed::" + ((($_ | Out-String) + "`n--- last output ---`n" + ($global:Tail -join "`n")) -replace "`r?`n", "%0A")); exit 1 }
$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root
$ver = (node -p "require('./package.json').version").Trim()
Run "npm ci" { npm ci }
Run "web build" { npm run build }
Push-Location src-tauri
Run "cargo build (Tauri host)" { cargo build --release --features custom-protocol }
Pop-Location
$exe = "src-tauri\target\release\shilploka.exe"
if (-not (Test-Path $exe)) { throw "STEP FAILED: $exe not produced" }
# smoke test: the game window process must start and keep running
$p = Start-Process -FilePath $exe -PassThru
Start-Sleep -Seconds 10
if ($p.HasExited) { throw "STEP FAILED: ShilpLoka.exe exited after start (code $($p.ExitCode))" }
Stop-Process -Id $p.Id -Force
Write-Host "smoke test: ShilpLoka.exe ran 10 s"
$layout = Join-Path $root "dist-msix\layout"
Remove-Item -Recurse -Force (Join-Path $root "dist-msix") -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$layout\ShilpLoka" | Out-Null
Copy-Item $exe "$layout\ShilpLoka\ShilpLoka.exe"
Run "icons" { python packaging\windows\make_icons.py snap\gui\shilploka.png "$layout\Assets" }
(Get-Content packaging\windows\AppxManifest.xml -Raw).Replace("__VERSION__", "$ver.0") | Set-Content "$layout\AppxManifest.xml" -Encoding utf8
$makeappx = Get-ChildItem "${env:ProgramFiles(x86)}\Windows Kits\10\bin\*\x64\makeappx.exe" | Sort-Object FullName -Descending | Select-Object -First 1
Run "makeappx pack" { & $makeappx.FullName pack /d $layout /p "dist-msix\ShilpLoka-$ver.msix" /o }
Compress-Archive -Path "$layout\ShilpLoka" -DestinationPath "dist-msix\ShilpLoka-$ver-portable.zip" -Force
Get-ChildItem dist-msix\*.msix, dist-msix\*.zip | ForEach-Object { "{0}  {1:N1} MB" -f $_.Name, ($_.Length / 1MB) }
