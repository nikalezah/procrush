import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec
import java.io.File
import java.security.MessageDigest

val kindClusterName: String = System.getenv("KIND_CLUSTER_NAME") ?: "procrush"
val kindNamespace = "procrush"
val kindDeployCacheDir = layout.projectDirectory.dir(".kind-deploy-cache")

tasks.named<Exec>("generateI18n") {
    inputs.file(layout.projectDirectory.file("i18n/error-codes.yaml"))
    inputs.dir(layout.projectDirectory.dir("i18n/locales"))
    inputs.file(layout.projectDirectory.file("i18n/scripts/generate.mjs"))
    inputs.file(layout.projectDirectory.file("i18n/scripts/validate.mjs"))
    outputs.file(layout.projectDirectory.file("i18n/generated/kotlin/jobs/procrush/i18n/ErrorCode.kt"))
    outputs.file(layout.projectDirectory.file("i18n/generated/typescript/errorCodes.ts"))
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

fun hashTree(root: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    if (!root.exists()) {
        return sha256Hex(ByteArray(0))
    }
    root.walkTopDown()
        .filter { it.isFile }
        .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
        .forEach { file ->
            digest.update(file.relativeTo(root).invariantSeparatorsPath.toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(file.readBytes())
        }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun artifactFingerprint(artifactDir: File, dockerfile: File): String {
    val combined = MessageDigest.getInstance("SHA-256")
    combined.update(hashTree(artifactDir).toByteArray(Charsets.UTF_8))
    combined.update(0)
    combined.update(if (dockerfile.exists()) dockerfile.readBytes() else ByteArray(0))
    return combined.digest().joinToString("") { "%02x".format(it) }
}

fun runCommand(workdir: File, vararg args: String) {
    val process = ProcessBuilder(*args)
        .directory(workdir)
        .inheritIO()
        .start()
    val code = process.waitFor()
    if (code != 0) {
        throw GradleException("Command failed (exit $code): ${args.joinToString(" ")}")
    }
}

fun redeployToKind(
    service: String,
    image: String,
    deployment: String,
    artifactDir: File,
    dockerfile: File,
    buildContext: File,
    workdir: File,
    logger: org.gradle.api.logging.Logger,
) {
    val cacheDir = kindDeployCacheDir.asFile
    cacheDir.mkdirs()
    val hashFile = cacheDir.resolve("$service.hash")
    val fingerprint = artifactFingerprint(artifactDir, dockerfile)
    val previous = if (hashFile.exists()) hashFile.readText().trim() else ""

    if (fingerprint == previous) {
        logger.lifecycle("[$service] artifact unchanged, skip redeploy")
        cacheDir.resolve("$service.stamp").writeText(fingerprint)
        return
    }

    logger.lifecycle("[$service] building image $image:local ...")
    runCommand(
        workdir,
        "docker", "build",
        "-f", dockerfile.path,
        "-t", "$image:local",
        buildContext.path,
    )

    logger.lifecycle("[$service] loading image into kind cluster $kindClusterName ...")
    runCommand(
        workdir,
        "kind", "load", "docker-image", "$image:local",
        "--name", kindClusterName,
    )

    logger.lifecycle("[$service] restarting deployment/$deployment in $kindNamespace ...")
    runCommand(
        workdir,
        "kubectl", "rollout", "restart", "deployment/$deployment",
        "-n", kindNamespace,
    )

    hashFile.writeText(fingerprint)
    cacheDir.resolve("$service.stamp").writeText(fingerprint)
    logger.lifecycle("[$service] redeployed")
}

fun Project.backendSourceTree(vararg paths: String) =
    files(
        paths.map { path ->
            fileTree(path) {
                exclude("**/build/**", "**/.gradle/**")
            }
        },
    )

// Spektor's generate task is not configuration-cache compatible; mark it so kind
// deploy builds that depend on it do not fail CC serialization.
subprojects {
    tasks.configureEach {
        if (name == "spektorGenerate") {
            notCompatibleWithConfigurationCache("Spektor plugin is not configuration-cache compatible")
        }
    }
}

tasks.register("apiToKind") {
    group = "kind"
    description = "Build api locally and redeploy to kind if the artifact changed"
    dependsOn("generateI18n", ":backend:api:installDist")
    inputs.files(
        backendSourceTree(
            "backend/api",
            "backend/contracts",
            "backend/config",
            "backend/domain",
            "backend/platform",
        ),
    )
    inputs.file("i18n/error-codes.yaml")
    inputs.dir("i18n/generated/kotlin")
    inputs.files(fileTree("openapi") { exclude("**/dist/**") })
    inputs.file("deploy/Dockerfile.api.dev")
    outputs.file(kindDeployCacheDir.file("api.stamp"))
    notCompatibleWithConfigurationCache("shells out to docker/kind/kubectl")

    doLast {
        redeployToKind(
            service = "api",
            image = "procrush-api",
            deployment = "api",
            artifactDir = file("backend/api/build/install/api"),
            dockerfile = file("deploy/Dockerfile.api.dev"),
            buildContext = file("backend/api/build/install/api"),
            workdir = rootDir,
            logger = logger,
        )
    }
}

tasks.register("personalityToKind") {
    group = "kind"
    description = "Build personality locally and redeploy to kind if the artifact changed"
    dependsOn("generateI18n", ":backend:personality:installDist")
    inputs.files(
        backendSourceTree(
            "backend/personality",
            "backend/contracts",
            "backend/config",
            "backend/domain",
            "backend/platform",
        ),
    )
    inputs.file("i18n/error-codes.yaml")
    inputs.dir("i18n/generated/kotlin")
    inputs.file("deploy/Dockerfile.personality.dev")
    outputs.file(kindDeployCacheDir.file("personality.stamp"))
    notCompatibleWithConfigurationCache("shells out to docker/kind/kubectl")

    doLast {
        redeployToKind(
            service = "personality",
            image = "procrush-personality",
            deployment = "personality",
            artifactDir = file("backend/personality/build/install/personality"),
            dockerfile = file("deploy/Dockerfile.personality.dev"),
            buildContext = file("backend/personality/build/install/personality"),
            workdir = rootDir,
            logger = logger,
        )
    }
}

tasks.register("matchingToKind") {
    group = "kind"
    description = "Build matching locally and redeploy to kind if the artifact changed"
    dependsOn("generateI18n", ":backend:matching:installDist")
    inputs.files(
        backendSourceTree(
            "backend/matching",
            "backend/contracts",
            "backend/config",
            "backend/platform/redis",
            "backend/platform/kafka",
            "backend/platform/observability",
        ),
    )
    inputs.file("i18n/error-codes.yaml")
    inputs.dir("i18n/generated/kotlin")
    inputs.file("deploy/Dockerfile.matching.dev")
    outputs.file(kindDeployCacheDir.file("matching.stamp"))
    notCompatibleWithConfigurationCache("shells out to docker/kind/kubectl")

    doLast {
        redeployToKind(
            service = "matching",
            image = "procrush-matching",
            deployment = "matching",
            artifactDir = file("backend/matching/build/install/matching"),
            dockerfile = file("deploy/Dockerfile.matching.dev"),
            buildContext = file("backend/matching/build/install/matching"),
            workdir = rootDir,
            logger = logger,
        )
    }
}

tasks.register<Exec>("frontendBuild") {
    group = "build"
    description = "Build the React frontend (npm run build)"
    dependsOn("generateI18n")
    workingDir = layout.projectDirectory.dir("frontend").asFile
    val npmCmd = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
    commandLine(npmCmd, "run", "build")
    inputs.files(
        fileTree("frontend") {
            exclude("node_modules/**", "dist/**", ".vite/**")
        },
    )
    inputs.dir("i18n/locales")
    inputs.dir("i18n/generated/typescript")
    inputs.file("i18n/error-codes.yaml")
    inputs.files(fileTree("openapi") { exclude("**/dist/**") })
    outputs.dir(layout.projectDirectory.dir("frontend/dist"))
    notCompatibleWithConfigurationCache("shells out to npm")
}

tasks.register("frontendToKind") {
    group = "kind"
    description = "Build frontend locally and redeploy to kind if the artifact changed"
    dependsOn("frontendBuild")
    inputs.files(
        fileTree("frontend") {
            exclude("node_modules/**", "dist/**", ".vite/**")
        },
    )
    inputs.dir("i18n/locales")
    inputs.dir("i18n/generated/typescript")
    inputs.file("i18n/error-codes.yaml")
    inputs.files(fileTree("openapi") { exclude("**/dist/**") })
    inputs.file("deploy/Dockerfile.frontend.dev")
    outputs.file(kindDeployCacheDir.file("frontend.stamp"))
    notCompatibleWithConfigurationCache("shells out to docker/kind/kubectl")

    doLast {
        redeployToKind(
            service = "frontend",
            image = "procrush-frontend",
            deployment = "frontend",
            artifactDir = file("frontend/dist"),
            dockerfile = file("deploy/Dockerfile.frontend.dev"),
            buildContext = file("frontend"),
            workdir = rootDir,
            logger = logger,
        )
    }
}

tasks.register("appsToKind") {
    group = "kind"
    description = "Build and redeploy all application services to kind"
    dependsOn("apiToKind", "personalityToKind", "matchingToKind", "frontendToKind")
}
