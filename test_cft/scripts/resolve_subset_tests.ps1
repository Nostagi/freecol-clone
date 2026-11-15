param(
    [Parameter(Mandatory = $true)][string]$Here,
    [Parameter(Mandatory = $true)][string]$RepoRoot,
    [Parameter(Mandatory = $true)][string]$OutFile,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$UserArgs
)

$ErrorActionPreference = 'Stop'

function Resolve-CsvPath([string]$RawPath) {
    $cleaned = $RawPath.Trim().Trim('"')
    if ([string]::IsNullOrWhiteSpace($cleaned)) { return $null }
    if ([System.IO.Path]::IsPathRooted($cleaned)) {
        if (Test-Path -LiteralPath $cleaned -PathType Leaf) {
            return (Resolve-Path -LiteralPath $cleaned).Path
        }
        return $null
    }
    $candidates = @(
        (Join-Path (Get-Location).Path $cleaned),
        (Join-Path $RepoRoot $cleaned),
        (Join-Path $Here $cleaned),
        (Join-Path (Join-Path $Here 'config') $cleaned)
    )
    foreach ($cand in $candidates) {
        if (Test-Path -LiteralPath $cand -PathType Leaf) {
            return (Resolve-Path -LiteralPath $cand).Path
        }
    }
    return $null
}

$rawCsv = $null
$extraArgs = New-Object System.Collections.Generic.List[string]

for ($i = 0; $i -lt $UserArgs.Count; $i++) {
    $arg = $UserArgs[$i]
    if ($arg -eq '-csv' -or $arg -eq '--csv') {
        if ($i + 1 -ge $UserArgs.Count) {
            [Console]::Error.WriteLine("[JaCoCo] ERROR: Missing file path after $arg")
            exit 1
        }
        $i++
        $rawCsv = $UserArgs[$i]
    }
    elseif ($arg -match '^(?:-csv=|--csv=|-Dcsv=)(.+)$') {
        $rawCsv = $Matches[1]
    }
    elseif (-not $rawCsv -and $arg -match '\.csv$' -and -not $arg.StartsWith('-')) {
        $rawCsv = $arg
    }
    else {
        $extraArgs.Add($arg)
    }
}

if (-not $rawCsv) {
    [Console]::Error.WriteLine("[JaCoCo] ERROR: No CSV path detected in arguments.")
    exit 1
}

$csvFullPath = Resolve-CsvPath $rawCsv
if (-not $csvFullPath) {
    [Console]::Error.WriteLine("[JaCoCo] ERROR: CSV file not found: $rawCsv")
    [Console]::Error.WriteLine("[JaCoCo] Searched: cwd, $RepoRoot, $Here, $(Join-Path $Here 'config')")
    exit 1
}

$nonEmptyLines = Get-Content -LiteralPath $csvFullPath -Encoding UTF8 | Where-Object {
    $t = $_.Trim()
    $t -ne '' -and -not $t.StartsWith('#')
}

if (-not $nonEmptyLines -or $nonEmptyLines.Count -eq 0) {
    [Console]::Error.WriteLine("[JaCoCo] ERROR: CSV file is empty: $csvFullPath")
    exit 1
}

# Parse CSV entries into (ClassName, TargetPkg)
$units = New-Object System.Collections.Generic.List[object]
$firstLineCols = ($nonEmptyLines[0] -split ',') | ForEach-Object { $_.Trim().Trim('"').ToLowerInvariant() }
$hasHeader = ($firstLineCols -contains 'class_name') -or
             ($firstLineCols -contains 'file') -or
             ($firstLineCols -contains 'class') -or
             ($firstLineCols -contains 'unit')

if ($hasHeader) {
    $rows = $nonEmptyLines | ConvertFrom-Csv
    foreach ($row in $rows) {
        $fileCol = ''
        if ($row.PSObject.Properties['file']) { $fileCol = [string]$row.file }
        elseif ($row.PSObject.Properties['path']) { $fileCol = [string]$row.path }
        $fileCol = $fileCol.Trim().Replace('\', '/')

        $clsCol = ''
        if ($row.PSObject.Properties['class_name']) { $clsCol = [string]$row.class_name }
        elseif ($row.PSObject.Properties['class']) { $clsCol = [string]$row.class }
        elseif ($row.PSObject.Properties['unit']) { $clsCol = [string]$row.unit }
        $clsCol = $clsCol.Trim() -replace '\.(java|class)$', ''

        if (-not $clsCol -and $fileCol) {
            $clsCol = [System.IO.Path]::GetFileNameWithoutExtension($fileCol)
        }
        if (-not $clsCol) { continue }

        $targetPkg = ''
        if ($fileCol -match '^(?:.*/)?(net/sf/freecol(?:/[^/]+)*)/[^/]+\.(?:java|class)$') {
            $targetPkg = $Matches[1].Replace('/', '.')
        }
        elseif ($row.PSObject.Properties['package']) {
            $pkgCol = ([string]$row.package).Trim().Replace('\', '/').Trim('/')
            if ($pkgCol -eq '(root)' -or $pkgCol -eq 'root') {
                $targetPkg = 'net.sf.freecol'
            }
            elseif ($pkgCol) {
                $targetPkg = if ($pkgCol.StartsWith('net.sf.freecol') -or $pkgCol.StartsWith('net/sf/freecol')) {
                    $pkgCol.Replace('/', '.')
                } else {
                    'net.sf.freecol.' + $pkgCol.Replace('/', '.')
                }
            }
        }

        $units.Add([PSCustomObject]@{ ClassName = $clsCol; TargetPkg = $targetPkg })
    }
}
else {
    foreach ($line in $nonEmptyLines) {
        $token = (($line -split ',')[0]).Trim().Trim('"').Replace('\', '/')
        if (-not $token) { continue }
        $targetPkg = ''
        $clsCol = $token
        if ($token -match '^(?:.*/)?(net/sf/freecol(?:/[^/]+)*)/([^/]+)\.(?:java|class)$') {
            $targetPkg = $Matches[1].Replace('/', '.')
            $clsCol = $Matches[2]
        }
        elseif ($token.Contains('/')) {
            $clsCol = [System.IO.Path]::GetFileNameWithoutExtension($token)
        }
        elseif ($token -match '^(net\.sf\.freecol(?:\.[^.]+)*)\.([^.]+)$') {
            $targetPkg = $Matches[1]
            $clsCol = $Matches[2]
        }
        else {
            $clsCol = $token -replace '\.(java|class)$', ''
        }
        $units.Add([PSCustomObject]@{ ClassName = $clsCol; TargetPkg = $targetPkg })
    }
}

if ($units.Count -eq 0) {
    [Console]::Error.WriteLine("[JaCoCo] ERROR: No valid units found in CSV: $csvFullPath")
    exit 1
}

# Index all test classes under test_cft/src/test/java
$testSrcRoot = Join-Path $Here 'src\test\java'
$testFiles = Get-ChildItem -LiteralPath $testSrcRoot -Recurse -File -Filter '*.java'
$testIndex = New-Object System.Collections.Generic.List[object]

foreach ($tf in $testFiles) {
    $base = $tf.BaseName
    if ($base -notmatch '(CfgTest|CFTTest|Test)$') { continue }
    $content = Get-Content -LiteralPath $tf.FullName -Raw -Encoding UTF8
    if ($base -notmatch '(CfgTest|CFTTest)$') {
        # For plain *Test.java, verify it looks like a JUnit test class
        if ($content -notmatch '@Test\b|extends\s+\w*TestCase\b') { continue }
    }
    $pkg = ''
    if ($content -match '(?m)^\s*package\s+([^;\s]+)\s*;') {
        $pkg = $Matches[1]
    }
    $fqn = if ($pkg) { "$pkg.$base" } else { $base }
    $testIndex.Add([PSCustomObject]@{
        BaseName = $base
        Package  = $pkg
        Fqn      = $fqn
    })
}

function Is-CompatibleTestPackage([string]$TestPkg, [string]$TargetPkg) {
    if (-not $TargetPkg) { return $true }
    if ($TestPkg -eq $TargetPkg) { return $true }
    # Allow test-only packages (e.g. net.sf.freecol.util.test, net.sf.freecol.model.test),
    # while preventing cross-package collisions between distinct production packages
    # (e.g. client.control vs server.control).
    if ($TestPkg -match '^net\.sf\.freecol\.(client|server|metaserver|tools|common)(?:\.|$)') {
        return $false
    }
    return $true
}

$matchedFqns = New-Object System.Collections.Generic.List[string]
$matchedSimple = New-Object System.Collections.Generic.List[string]
$seenFqns = New-Object System.Collections.Generic.HashSet[string]
$seenSimple = New-Object System.Collections.Generic.HashSet[string]
$matchedUnits = New-Object System.Collections.Generic.List[string]
$unmatchedUnits = New-Object System.Collections.Generic.List[string]

foreach ($u in $units) {
    $candidates = if ($u.ClassName -match 'Test$') {
        @($u.ClassName)
    } else {
        @("$($u.ClassName)CfgTest", "$($u.ClassName)CFTTest", "$($u.ClassName)Test")
    }

    $hits = $testIndex | Where-Object {
        ($candidates -contains $_.BaseName) -and (Is-CompatibleTestPackage $_.Package $u.TargetPkg)
    }

    if (-not $hits -or @($hits).Count -eq 0) {
        $unmatchedUnits.Add($u.ClassName)
        continue
    }

    $matchedUnits.Add($u.ClassName)
    foreach ($h in $hits) {
        if ($seenFqns.Add($h.Fqn)) {
            $matchedFqns.Add($h.Fqn)
        }
        if ($seenSimple.Add($h.BaseName)) {
            $matchedSimple.Add($h.BaseName)
        }
    }
}

$csvStem = [System.IO.Path]::GetFileNameWithoutExtension($csvFullPath)
Write-Host "[JaCoCo] Subset CSV:           $csvFullPath"
Write-Host "[JaCoCo] Units in CSV:         $($units.Count) (matched: $($matchedUnits.Count), no test found: $($unmatchedUnits.Count))"
if ($unmatchedUnits.Count -gt 0) {
    Write-Host "[JaCoCo] Missing test classes: $($unmatchedUnits -join ', ')"
}

if ($matchedFqns.Count -eq 0) {
    [Console]::Error.WriteLine("[JaCoCo] ERROR: No matching test classes found for any unit in $csvFullPath.")
    exit 2
}

Write-Host "[JaCoCo] Test classes to run ($($matchedFqns.Count)):"
foreach ($fqn in $matchedFqns) {
    Write-Host "           - $fqn"
}

$testList = $matchedFqns -join ','
if ($testList.Length -gt 6000) {
    $testList = $matchedSimple -join ','
}

$quotedExtra = ($extraArgs | ForEach-Object {
    if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
}) -join ' '

$outLines = @(
    "SUBSET_NAME=$csvStem",
    "TEST_LIST=$testList",
    "EXTRA_ARGS=$quotedExtra"
)
[System.IO.File]::WriteAllLines($OutFile, $outLines, (New-Object System.Text.UTF8Encoding($false)))
exit 0
