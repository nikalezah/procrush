package jobs.procrush.gradle.kind

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File
import java.io.Serializable
import java.security.MessageDigest

data class KindServiceSpec(
    val name: String,
    val image: String,
    val deployment: String,
    val artifactDir: File,
    val dockerfile: File,
    val buildContext: File,
) : Serializable

data class KindChangedService(
    val service: KindServiceSpec,
    val fingerprint: String,
    val deploymentExisted: Boolean,
) : Serializable

object KindSupport {
    fun isWindowsOs(): Boolean =
        System.getProperty("os.name").lowercase().contains("windows")

    fun cli(name: String): String = if (isWindowsOs()) "$name.exe" else name

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
            if (isWindowsOs()) {
                arrayOf("where.exe", name)
            } else {
                arrayOf("sh", "-c", "command -v $name")
            }
        if (!commandSucceeds(workdir, *checkArgs)) {
            throw GradleException("Required command not found: $name")
        }
    }

    fun clusterExists(workdir: File, clusterName: String): Boolean {
        val (code, output) = runCommandCapture(workdir, cli("kind"), "get", "clusters")
        if (code != 0) {
            return false
        }
        return output.lineSequence().any { it.trim() == clusterName }
    }

    fun kindNodes(workdir: File, clusterName: String): List<String> {
        val (code, output) = runCommandCapture(
            workdir,
            cli("kind"), "get", "nodes",
            "--name", clusterName,
        )
        if (code != 0) {
            throw GradleException("Failed to list kind nodes for cluster $clusterName:\n$output")
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
    fun ensureClusterNodesRunning(workdir: File, clusterName: String, logger: Logger) {
        val nodes = kindNodes(workdir, clusterName)
        if (nodes.isEmpty()) {
            throw GradleException(
                "Kind cluster $clusterName is registered but has no nodes. " +
                    "Recreate with: ./gradlew kindDown && ./gradlew kindUp",
            )
        }
        val stopped = nodes.filterNot { dockerContainerRunning(workdir, it) }
        if (stopped.isEmpty()) {
            return
        }
        logger.lifecycle(
            "Kind cluster $clusterName nodes are stopped (e.g. after Docker quit). Starting: ${stopped.joinToString()}",
        )
        for (node in stopped) {
            runCommand(workdir, cli("docker"), "start", node)
        }
        waitForClusterApi(workdir, clusterName, logger)
    }

    fun waitForClusterApi(
        workdir: File,
        clusterName: String,
        logger: Logger,
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
            "Kubernetes API for kind cluster $clusterName did not become ready within ${timeoutSeconds}s. " +
                "Try: ./gradlew kindDown && ./gradlew kindUp",
        )
    }

    fun deploymentExists(workdir: File, namespace: String, deployment: String): Boolean =
        commandSucceeds(
            workdir,
            cli("kubectl"), "get", "deployment", deployment,
            "-n", namespace,
        )

    fun namespaceExists(workdir: File, namespace: String): Boolean =
        commandSucceeds(workdir, cli("kubectl"), "get", "namespace", namespace)

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
    fun ensureCluster(
        workdir: File,
        clusterName: String,
        kindConfigFile: File,
        logger: Logger,
    ): Boolean {
        val created =
            if (clusterExists(workdir, clusterName)) {
                logger.lifecycle("Kind cluster $clusterName already exists.")
                false
            } else {
                logger.lifecycle("Creating kind cluster $clusterName ...")
                runCommand(
                    workdir,
                    cli("kind"), "create", "cluster",
                    "--name", clusterName,
                    "--config", kindConfigFile.path,
                )
                true
            }
        runCommand(workdir, cli("kubectl"), "config", "use-context", "kind-$clusterName")
        if (!created) {
            ensureClusterNodesRunning(workdir, clusterName, logger)
            if (!commandSucceeds(workdir, cli("kubectl"), "get", "--raw=/readyz")) {
                waitForClusterApi(workdir, clusterName, logger)
            }
        }
        return created
    }

    fun clearKindDeployCache(cacheDir: File, logger: Logger) {
        if (!cacheDir.exists()) {
            return
        }
        cacheDir.listFiles()?.forEach { it.delete() }
        logger.lifecycle("Cleared .kind-deploy-cache (images must be rebuilt for a new cluster)")
    }

    fun ensureIngress(workdir: File, ingressUrl: String, logger: Logger) {
        if (ingressControllerReady(workdir)) {
            logger.lifecycle("ingress-nginx already ready.")
            return
        }
        logger.lifecycle("Installing ingress-nginx ...")
        runCommand(workdir, cli("kubectl"), "apply", "-f", ingressUrl)
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
}
