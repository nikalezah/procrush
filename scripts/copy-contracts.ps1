$root = "C:\Documents\projects\procrush"
$src = Join-Path $root "backend\shared\src\main\kotlin\jobs\procrush"
$dst = Join-Path $root "backend\contracts\src\main\kotlin\jobs\procrush"

function Copy-RelPath($rel) {
    $s = Join-Path $src $rel
    $d = Join-Path $dst $rel
    if (-not (Test-Path $s)) {
        Write-Host "MISSING $s"
        return
    }
    $parent = Split-Path $d -Parent
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    if ((Get-Item $s).PSIsContainer) {
        Copy-Item -Recurse -Force $s $parent
    } else {
        Copy-Item -Force $s $d
    }
}

$paths = @(
    "shared\DomainExceptions.kt",
    "shared\dto",
    "seeker\dto",
    "employer\dto",
    "survey\dto",
    "personality\dto",
    "matching\dto",
    "matching\events",
    "matching\model",
    "matching\service\MatchScoringService.kt",
    "matching\service\MatchingQueries.kt",
    "survey\scoring",
    "personality\messaging\PersonalityGenerationJob.kt",
    "llm\LlmClient.kt",
    "llm\LlmResponseParser.kt"
)
foreach ($p in $paths) { Copy-RelPath $p }

$tsrc = Join-Path $root "backend\shared\src\test\kotlin\jobs\procrush"
$tdst = Join-Path $root "backend\contracts\src\test\kotlin\jobs\procrush"
$tests = @(
    "matching\service\MatchScoringServiceTest.kt",
    "matching\events\MatchingEventsTest.kt",
    "matching\dto\InterestStatusCalculatorTest.kt",
    "matching\dto\OverviewDtosTest.kt",
    "survey\scoring\SurveyFlowRulesTest.kt",
    "survey\scoring\SurveyScoringServiceTest.kt",
    "llm\LlmResponseParserTest.kt"
)
foreach ($t in $tests) {
    $s = Join-Path $tsrc $t
    $d = Join-Path $tdst $t
    if (Test-Path $s) {
        New-Item -ItemType Directory -Force -Path (Split-Path $d -Parent) | Out-Null
        Copy-Item -Force $s $d
    }
}

Write-Host "KT count:" (Get-ChildItem -Recurse (Join-Path $root "backend\contracts") -Filter "*.kt").Count
