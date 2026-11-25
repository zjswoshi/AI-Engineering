$ErrorActionPreference = 'Stop'
$platform = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture
if ($env:OS -ne 'Windows_NT' -or $platform -ne 'X64') {
    throw 'The bundled OCR runtime targets Windows x64. See tools/opencodereview/README.md for building from source on other platforms.'
}

$distribution = Join-Path $PSScriptRoot 'opencodereview'
$binaryName = 'opencodereview-windows-amd64.exe'
$archive = Join-Path $distribution 'opencodereview-windows-amd64.zip'
$cache = Join-Path $distribution '.runtime-cache/1.14.3'
$binary = Join-Path $cache $binaryName
$sha256 = '5531c50080feec9bc2b966c83e1498a3b910ac23bd907651c0cfe4e7f7463631'

if (-not (Test-Path -LiteralPath $binary -PathType Leaf)) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Directory]::CreateDirectory($cache) | Out-Null
    $temporary = Join-Path $cache ([Guid]::NewGuid().ToString('N') + '.tmp')
    $package = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        $entry = $package.GetEntry($binaryName)
        if ($null -eq $entry) { throw "Missing $binaryName in $archive" }
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $temporary)
        if ((Get-FileHash -LiteralPath $temporary -Algorithm SHA256).Hash -ne $sha256) {
            throw 'Bundled OCR binary failed SHA-256 verification.'
        }
        try {
            [IO.File]::Move($temporary, $binary)
        } catch [IO.IOException] {
            if (-not (Test-Path -LiteralPath $binary -PathType Leaf)) { throw }
        }
    } finally {
        $package.Dispose()
        [IO.File]::Delete($temporary)
    }
}
if ((Get-FileHash -LiteralPath $binary -Algorithm SHA256).Hash -ne $sha256) {
    throw "Cached OCR binary failed SHA-256 verification: $binary"
}

Push-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
try {
    & $binary @args
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $exitCode
