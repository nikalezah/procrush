import java.security.MessageDigest

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

fun nodeAvailable(): Boolean =
    try {
        val nodeCmd =
            if (System.getProperty("os.name").lowercase().contains("windows")) "node.exe" else "node"
        ProcessBuilder(nodeCmd, "--version")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }

tasks.register<Exec>("generateI18n") {
    group = "i18n"
    description = "Generate ErrorCode from i18n/error-codes.yaml (requires Node.js)"
    workingDir = layout.projectDirectory.dir("i18n").asFile
    val nodeCmd =
        if (System.getProperty("os.name").lowercase().contains("windows")) "node.exe" else "node"
    commandLine(nodeCmd, "scripts/generate.mjs")
    notCompatibleWithConfigurationCache("uses script-local onlyIf / Node detection")

    inputs.file(layout.projectDirectory.file("i18n/error-codes.yaml"))
    inputs.dir(layout.projectDirectory.dir("i18n/locales"))
    inputs.file(layout.projectDirectory.file("i18n/scripts/generate.mjs"))
    inputs.file(layout.projectDirectory.file("i18n/scripts/validate.mjs"))
    outputs.file(layout.projectDirectory.file("i18n/generated/kotlin/jobs/procrush/i18n/ErrorCode.kt"))
    outputs.file(layout.projectDirectory.file("i18n/generated/typescript/errorCodes.ts"))

    val generatedKotlin =
        layout.projectDirectory.file("i18n/generated/kotlin/jobs/procrush/i18n/ErrorCode.kt")
    val generatedTs =
        layout.projectDirectory.file("i18n/generated/typescript/errorCodes.ts")

    // Prod Docker images (JDK only) have no Node; generated sources are committed in the repo.
    onlyIf {
        if (nodeAvailable()) {
            true
        } else if (generatedKotlin.asFile.exists() && generatedTs.asFile.exists()) {
            logger.lifecycle(
                "Skipping generateI18n: Node.js not available; using committed generated sources",
            )
            false
        } else {
            throw GradleException(
                "generateI18n requires Node.js, and i18n/generated sources are missing",
            )
        }
    }
}

// --- Kind deploy (bootstrap + local build → thin image → conditional rollout) ---

val isWindowsOs: Boolean = System.getProperty("os.name").lowercase().contains("windows")
val kindClusterName: String = System.getenv("KIND_CLUSTER_NAME") ?: "procrush"
val kindNamespace = "procrush"
val kindDeployCacheDir = layout.projectDirectory.dir(".kind-deploy-cache")
val kindIngressUrl =
    "https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/kind/deploy.yaml"
val kindK8sDir = layout.projectDirectory.dir("deploy/k8s")
val kindOverlayDir = kindK8sDir.dir("overlays/kind")
val kindSecretFile = kindK8sDir.file("base/secret.yaml")
val kindConfigFile = kindK8sDir.file("kind-config.yaml")

fun cli(name: String): String = if (isWindowsOs) "$name.exe" else name

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

fun manifestsFingerprint(baseDir: File, overlayDir: File): String {
    val combined = MessageDigest.getInstance("SHA-256")
    combined.update(hashTree(baseDir).toByteArray(Charsets.UTF_8))
    combined.update(0)
    combined.update(hashTree(overlayDir).toByteArray(Charsets.UTF_8))
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

fun runCommandCapture(workdir: File, vararg args: String): Pair<Int, String> {
    val process = ProcessBuilder(*args)
        .directory(workdir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    val code = process.waitFor()
    return code to output
}

fun commandSucceeds(workdir: File, vararg args: String): Boolean =
    try {
        runCommandCapture(workdir, *args).first == 0
    } catch (_: Exception) {
        false
    }

fun requireCli(workdir: File, name: String) {
    val checkArgs =
        if (isWindowsOs) {
            arrayOf("where.exe", name)
        } else {
            arrayOf("sh", "-c", "command -v $name")
        }
    if (!commandSucceeds(workdir, *checkArgs)) {
        throw GradleException("Required command not found: $name")
    }
}

fun clusterExists(workdir: File): Boolean {
    val (code, output) = runCommandCapture(workdir, cli("kind"), "get", "clusters")
    if (code != 0) {
        return false
    }
    return output.lineSequence().any { it.trim() == kindClusterName }
}

fun kindNodes(workdir: File): List<String> {
    val (code, output) = runCommandCapture(
        workdir,
        cli("kind"), "get", "nodes",
        "--name", kindClusterName,
    )
    if (code != 0) {
        throw GradleException("Failed to list kind nodes for cluster $kindClusterName:\n$output")
    }
    return output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
}

fun dockerContainerRunning(workdir: File, container: String): Boolean {
    val (code, output) = runCommandCapture(
        workdir,
        cli("docker"), "inspect",
        "-f", "{{.State.Running}}",
        container,
    )
    return code == 0 && output.trim().equals("true", ignoreCase = true)
}

/**
 * After Docker Desktop stop/restart, kind node containers remain stopped while
 * `kind get clusters` still lists the cluster. Start them and wait for the API.
 */
fun ensureClusterNodesRunning(workdir: File, logger: org.gradle.api.logging.Logger) {
    val nodes = kindNodes(workdir)
    if (nodes.isEmpty()) {
        throw GradleException(
            "Kind cluster $kindClusterName is registered but has no nodes. " +
                "Recreate with: ./gradlew kindDown && ./gradlew kindUp",
        )
    }
    val stopped = nodes.filterNot { dockerContainerRunning(workdir, it) }
    if (stopped.isEmpty()) {
        return
    }
    logger.lifecycle(
        "Kind cluster $kindClusterName nodes are stopped (e.g. after Docker quit). Starting: ${stopped.joinToString()}",
    )
    for (node in stopped) {
        runCommand(workdir, cli("docker"), "start", node)
    }
    waitForClusterApi(workdir, logger)
}

fun waitForClusterApi(
    workdir: File,
    logger: org.gradle.api.logging.Logger,
    timeoutSeconds: Int = 120,
) {
    logger.lifecycle("Waiting for Kubernetes API (up to ${timeoutSeconds}s) ...")
    val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
    while (System.currentTimeMillis() < deadline) {
        if (commandSucceeds(workdir, cli("kubectl"), "get", "--raw=/readyz")) {
            logger.lifecycle("Kubernetes API is ready.")
            return
        }
        Thread.sleep(2_000)
    }
    throw GradleException(
        "Kubernetes API for kind cluster $kindClusterName did not become ready within ${timeoutSeconds}s. " +
            "Try: ./gradlew kindDown && ./gradlew kindUp",
    )
}

fun deploymentExists(workdir: File, deployment: String): Boolean =
    commandSucceeds(
        workdir,
        cli("kubectl"), "get", "deployment", deployment,
        "-n", kindNamespace,
    )

fun namespaceExists(workdir: File): Boolean =
    commandSucceeds(workdir, cli("kubectl"), "get", "namespace", kindNamespace)

fun ingressControllerReady(workdir: File): Boolean {
    val (code, output) = runCommandCapture(
        workdir,
        cli("kubectl"), "get", "deployment", "ingress-nginx-controller",
        "-n", "ingress-nginx",
        "-o", "jsonpath={.status.readyReplicas}",
    )
    if (code != 0) {
        return false
    }
    return (output.trim().toIntOrNull() ?: 0) >= 1
}

/** @return true if a new cluster was created in this invocation */
fun ensureCluster(workdir: File, logger: org.gradle.api.logging.Logger): Boolean {
    val created =
        if (clusterExists(workdir)) {
            logger.lifecycle("Kind cluster $kindClusterName already exists.")
            false
        } else {
            logger.lifecycle("Creating kind cluster $kindClusterName ...")
            runCommand(
                workdir,
                cli("kind"), "create", "cluster",
                "--name", kindClusterName,
                "--config", kindConfigFile.asFile.path,
            )
            true
        }
    runCommand(workdir, cli("kubectl"), "config", "use-context", "kind-$kindClusterName")
    if (!created) {
        ensureClusterNodesRunning(workdir, logger)
        if (!commandSucceeds(workdir, cli("kubectl"), "get", "--raw=/readyz")) {
            waitForClusterApi(workdir, logger)
        }
    }
    return created
}

fun clearKindDeployCache(logger: org.gradle.api.logging.Logger) {
    val cacheDir = kindDeployCacheDir.asFile
    if (!cacheDir.exists()) {
        return
    }
    cacheDir.listFiles()?.forEach { it.delete() }
    logger.lifecycle("Cleared .kind-deploy-cache (images must be rebuilt for a new cluster)")
}

fun ensureIngress(workdir: File, logger: org.gradle.api.logging.Logger) {
    if (ingressControllerReady(workdir)) {
        logger.lifecycle("ingress-nginx already ready.")
        return
    }
    logger.lifecycle("Installing ingress-nginx ...")
    runCommand(workdir, cli("kubectl"), "apply", "-f", kindIngressUrl)
    logger.lifecycle("Waiting for ingress-nginx admission jobs and controller ...")
    runCommand(
        workdir,
        cli("kubectl"), "wait", "--namespace", "ingress-nginx",
        "--for=condition=complete", "job/ingress-nginx-admission-create",
        "--timeout=120s",
    )
    runCommand(
        workdir,
        cli("kubectl"), "wait", "--namespace", "ingress-nginx",
        "--for=condition=complete", "job/ingress-nginx-admission-patch",
        "--timeout=120s",
    )
    runCommand(
        workdir,
        cli("kubectl"), "rollout", "status", "deployment/ingress-nginx-controller",
        "-n", "ingress-nginx",
        "--timeout=180s",
    )
}

data class KindAppService(
    val name: String,
    val image: String,
    val deployment: String,
    val artifactDir: File,
    val dockerfile: File,
    val buildContext: File,
)

data class KindChangedService(
    val service: KindAppService,
    val fingerprint: String,
    val deploymentExisted: Boolean,
)

// Spektor's generate task is not configuration-cache compatible; mark it so kind
// deploy builds that depend on it do not fail CC serialization.
// Pin JVM 25 so local installDist matches eclipse-temurin:25-jre images.
subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(25)
        }
    }
    tasks.configureEach {
        if (name == "spektorGenerate") {
            notCompatibleWithConfigurationCache("Spektor plugin is not configuration-cache compatible")
        }
    }
}

tasks.register<Exec>("frontendBuild") {
    group = "build"
    description = "Build the React frontend (npm run build)"
    dependsOn("generateI18n")
    workingDir = layout.projectDirectory.dir("frontend").asFile
    val npmCmd = if (isWindowsOs) "npm.cmd" else "npm"
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

tasks.register("kindUp") {
    group = "kind"
    description =
        "Ensure kind cluster (create or start stopped nodes) + ingress, build/load changed thin images, apply manifests, restart only when needed"
    dependsOn(
        "generateI18n",
        ":backend:api:installDist",
        ":backend:personality:installDist",
        ":backend:matching:installDist",
        "frontendBuild",
    )
    notCompatibleWithConfigurationCache("shells out to docker/kind/kubectl")

    doLast {
        val workdir = rootDir
        requireCli(workdir, "kind")
        requireCli(workdir, "kubectl")
        requireCli(workdir, "docker")

        if (!kindSecretFile.asFile.exists()) {
            throw GradleException(
                "Missing deploy/k8s/base/secret.yaml — copy from secret.yaml.example and set LLM_API_KEY",
            )
        }

        val clusterCreated = ensureCluster(workdir, logger)
        if (clusterCreated) {
            clearKindDeployCache(logger)
        }
        ensureIngress(workdir, logger)

        val cacheDir = kindDeployCacheDir.asFile
        cacheDir.mkdirs()

        val services = listOf(
            KindAppService(
                name = "api",
                image = "procrush-api",
                deployment = "api",
                artifactDir = file("backend/api/build/install/api"),
                dockerfile = file("deploy/Dockerfile.api.dev"),
                buildContext = file("backend/api/build/install/api"),
            ),
            KindAppService(
                name = "personality",
                image = "procrush-personality",
                deployment = "personality",
                artifactDir = file("backend/personality/build/install/personality"),
                dockerfile = file("deploy/Dockerfile.personality.dev"),
                buildContext = file("backend/personality/build/install/personality"),
            ),
            KindAppService(
                name = "matching",
                image = "procrush-matching",
                deployment = "matching",
                artifactDir = file("backend/matching/build/install/matching"),
                dockerfile = file("deploy/Dockerfile.matching.dev"),
                buildContext = file("backend/matching/build/install/matching"),
            ),
            KindAppService(
                name = "frontend",
                image = "procrush-frontend",
                deployment = "frontend",
                artifactDir = file("frontend/dist"),
                dockerfile = file("deploy/Dockerfile.frontend.dev"),
                buildContext = file("frontend"),
            ),
        )

        val changed = mutableListOf<KindChangedService>()
        for (service in services) {
            val hashFile = cacheDir.resolve("${service.name}.hash")
            val fingerprint = artifactFingerprint(service.artifactDir, service.dockerfile)
            val previous = if (hashFile.exists()) hashFile.readText().trim() else ""
            if (fingerprint == previous) {
                logger.lifecycle("[${service.name}] artifact unchanged, skip image rebuild")
                cacheDir.resolve("${service.name}.stamp").writeText(fingerprint)
                continue
            }
            val existed = deploymentExists(workdir, service.deployment)
            logger.lifecycle("[${service.name}] building image ${service.image}:local ...")
            runCommand(
                workdir,
                cli("docker"), "build",
                "-f", service.dockerfile.path,
                "-t", "${service.image}:local",
                service.buildContext.path,
            )
            logger.lifecycle("[${service.name}] loading image into kind cluster $kindClusterName ...")
            runCommand(
                workdir,
                cli("kind"), "load", "docker-image", "${service.image}:local",
                "--name", kindClusterName,
            )
            changed += KindChangedService(service, fingerprint, existed)
        }

        val manifestsHash = manifestsFingerprint(
            kindK8sDir.dir("base").asFile,
            kindOverlayDir.asFile,
        )
        val manifestsHashFile = cacheDir.resolve("manifests.hash")
        val previousManifests =
            if (manifestsHashFile.exists()) manifestsHashFile.readText().trim() else ""
        val needApply = !namespaceExists(workdir) || manifestsHash != previousManifests
        if (needApply) {
            logger.lifecycle("Applying Kubernetes manifests ...")
            runCommand(
                workdir,
                cli("kubectl"), "apply", "-k", kindOverlayDir.asFile.path,
            )
            manifestsHashFile.writeText(manifestsHash)
        } else {
            logger.lifecycle("Manifests unchanged, skip kubectl apply")
        }

        for (item in changed) {
            if (item.deploymentExisted) {
                logger.lifecycle(
                    "[${item.service.name}] restarting deployment/${item.service.deployment} ...",
                )
                runCommand(
                    workdir,
                    cli("kubectl"), "rollout", "restart",
                    "deployment/${item.service.deployment}",
                    "-n", kindNamespace,
                )
            } else {
                logger.lifecycle(
                    "[${item.service.name}] deployment did not exist before apply, skip restart",
                )
            }
            cacheDir.resolve("${item.service.name}.hash").writeText(item.fingerprint)
            cacheDir.resolve("${item.service.name}.stamp").writeText(item.fingerprint)
        }

        logger.lifecycle("")
        logger.lifecycle("ProCrush kind stack is up.")
        logger.lifecycle("Open:               http://127.10.0.10")
        logger.lifecycle("API health:         http://127.10.0.10/api/auth/me (401 without session is OK)")
        logger.lifecycle("")
        logger.lifecycle("Infra endpoints:    127.10.0.11:5432 postgres, 127.10.0.13:6379 redis, 127.10.0.14:5672 rabbitmq")
        logger.lifecycle("Observability:      http://127.10.0.16:3000 grafana, http://127.10.0.17:9090 prometheus")
    }
}

tasks.register("kindDown") {
    group = "kind"
    description = "Delete the local kind cluster"
    notCompatibleWithConfigurationCache("shells out to kind")

    doLast {
        val workdir = rootDir
        requireCli(workdir, "kind")
        if (clusterExists(workdir)) {
            logger.lifecycle("Deleting kind cluster $kindClusterName ...")
            runCommand(workdir, cli("kind"), "delete", "cluster", "--name", kindClusterName)
            clearKindDeployCache(logger)
        } else {
            logger.lifecycle("Kind cluster $kindClusterName does not exist.")
        }
    }
}
