$root = "C:\Documents\projects\procrush"
$sharedMain = Join-Path $root "backend\shared\src\main\kotlin"
$sharedRes = Join-Path $root "backend\shared\src\main\resources"

function Ensure-Copy($src, $dst) {
    if (-not (Test-Path $src)) { Write-Host "SKIP missing $src"; return }
    $parent = Split-Path $dst -Parent
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    if ((Get-Item $src).PSIsContainer) {
        Copy-Item -Recurse -Force $src $parent
    } else {
        Copy-Item -Force $src $dst
    }
}

function Copy-Tree($baseSrc, $baseDst, $rel) {
    Ensure-Copy (Join-Path $baseSrc $rel) (Join-Path $baseDst $rel)
}

# INFRA
$infraMain = Join-Path $root "backend\infra\src\main\kotlin"
$infraRes = Join-Path $root "backend\infra\src\main\resources"
$jp = Join-Path $sharedMain "jobs\procrush"
$infraJp = Join-Path $infraMain "jobs\procrush"

Copy-Tree $jp $infraJp "bootstrap\config"
Copy-Tree $jp $infraJp "bootstrap\DatabaseFactory.kt"
Copy-Tree $jp $infraJp "bootstrap\redis"
Copy-Tree $jp $infraJp "bootstrap\rabbitmq"
Copy-Tree $jp $infraJp "bootstrap\kafka"
Copy-Tree $jp $infraJp "llm\LlmFactory.kt"
Copy-Tree $jp $infraJp "llm\OpenAiCompatibleLlmClient.kt"
Copy-Tree $jp $infraJp "llm\OllamaLlmClient.kt"
Copy-Tree $jp $infraJp "llm\StubLlmClient.kt"
Copy-Tree (Join-Path $sharedMain "db") (Join-Path $infraMain "db") "migration"
Ensure-Copy $sharedRes (Join-Path $infraRes "") 

# SCHEMA (all tables for migrations)
$schemaMain = Join-Path $root "backend\schema\src\main\kotlin"
$schemaJp = Join-Path $schemaMain "jobs\procrush"
Copy-Tree $jp $schemaJp "shared\tables"
Copy-Tree $jp $schemaJp "auth\tables"
Copy-Tree $jp $schemaJp "seeker\tables"
Copy-Tree $jp $schemaJp "employer\tables"
Copy-Tree $jp $schemaJp "survey\tables"
Copy-Tree $jp $schemaJp "matching\tables"

# DOMAIN REFERENCE
$refMain = Join-Path $root "backend\domain\reference\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $refMain "shared\repository"

# DOMAIN AUTH
$authMain = Join-Path $root "backend\domain\auth\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $authMain "auth\repository"
Copy-Tree $jp $authMain "auth\service"
# auth tables in schema only - repos reference them via schema dep

# DOMAIN SEEKER
$seekerMain = Join-Path $root "backend\domain\seeker\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $seekerMain "seeker\repository"

# DOMAIN EMPLOYER
$employerMain = Join-Path $root "backend\domain\employer\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $employerMain "employer\repository"
Copy-Tree $jp $employerMain "employer\service"

# DOMAIN SURVEY
$surveyMain = Join-Path $root "backend\domain\survey\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $surveyMain "survey\repository"
Copy-Tree $jp $surveyMain "survey\service"

# DOMAIN MATCHING
$matchMain = Join-Path $root "backend\domain\matching\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $matchMain "matching\repository"
Copy-Tree $jp $matchMain "matching\service\MatchInterestService.kt"
Copy-Tree $jp $matchMain "matching\service\RedisMatchInterestNotifier.kt"
Copy-Tree $jp $matchMain "matching\cache"
Copy-Tree $jp $matchMain "matching\client"
Copy-Tree $jp $matchMain "matching\kafka"

# DOMAIN PERSONALITY
$persMain = Join-Path $root "backend\domain\personality\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $persMain "personality\messaging\PersonalityJobPublisher.kt"
Copy-Tree $jp $persMain "personality\service\PersonalityGenerationCoordinator.kt"
Copy-Tree $jp $persMain "personality\service\PersonalityGenerationLockGuard.kt"
Copy-Tree $jp $persMain "personality\service\PersonalityProfileReader.kt"
Copy-Tree $jp $persMain "personality\service\PersonalityProfileService.kt"
Copy-Tree $jp $persMain "personality\service\RedisPersonalityStatusNotifier.kt"
Copy-Tree $jp $persMain "personality\llm\PersonalityProfileMapper.kt"

# BOOTSTRAP
$bootMain = Join-Path $root "backend\bootstrap\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $bootMain "bootstrap\modules"
Copy-Tree (Join-Path $root "backend\api\src\main\kotlin\jobs\procrush") $bootMain "bootstrap\AppContext.kt"
Copy-Tree $jp $bootMain "seeker\service\SeekerProfileService.kt"
Copy-Tree $jp $bootMain "employer\service\EmployerProfileService.kt"
Copy-Tree $jp $bootMain "fixtures"

# PERSONALITY WORKER
$persWorker = Join-Path $root "backend\personality\src\main\kotlin\jobs\procrush"
Copy-Tree $jp $persWorker "bootstrap\WorkerContext.kt"
Copy-Tree $jp $persWorker "personality\messaging\PersonalityJobConsumer.kt"
Copy-Tree $jp $persWorker "personality\messaging\PersonalityMessageDedup.kt"
Copy-Tree $jp $persWorker "personality\service\PersonalityGenerationHandler.kt"
Copy-Tree $jp $persWorker "personality\llm\PersonalityPromptBuilder.kt"
Copy-Tree $jp $persWorker "personality\llm\PersonalityProfileValidator.kt"

Write-Host "Migration copy complete"
