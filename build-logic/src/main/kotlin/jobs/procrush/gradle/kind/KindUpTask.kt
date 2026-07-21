package jobs.procrush.gradle.kind

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class KindUpTask : DefaultTask() {

    @get:Internal
    abstract val workDir: DirectoryProperty

    @get:Internal
    abstract val cacheDir: DirectoryProperty

    @get:Internal
    abstract val secretFile: RegularFileProperty

    @get:Internal
    abstract val kindConfigFile: RegularFileProperty

    @get:Internal
    abstract val k8sBaseDir: DirectoryProperty

    @get:Internal
    abstract val overlayDir: DirectoryProperty

    @get:Internal
    abstract val clusterName: Property<String>

    @get:Internal
    abstract val namespace: Property<String>

    @get:Internal
    abstract val ingressUrl: Property<String>

    /** Set at configuration time; must be Serializable for configuration cache. */
    @get:Internal
    var services: List<KindServiceSpec> = emptyList()

    init {
        group = "kind"
        description =
            "Ensure kind cluster (create or start stopped nodes) + ingress, build/load changed thin images, apply manifests, restart only when needed"
    }

    @TaskAction
    fun run() {
        val workdir = workDir.get().asFile
        val cluster = clusterName.get()
        val ns = namespace.get()

        KindSupport.requireCli(workdir, "kind")
        KindSupport.requireCli(workdir, "kubectl")
        KindSupport.requireCli(workdir, "docker")

        if (!secretFile.get().asFile.exists()) {
            throw GradleException(
                "Missing deploy/k8s/base/secret.yaml — copy from secret.yaml.example and set LLM_API_KEY",
            )
        }

        val clusterCreated = KindSupport.ensureCluster(
            workdir,
            cluster,
            kindConfigFile.get().asFile,
            logger,
        )
        if (clusterCreated) {
            KindSupport.clearKindDeployCache(cacheDir.get().asFile, logger)
        }
        KindSupport.ensureIngress(workdir, ingressUrl.get(), logger)

        val cache = cacheDir.get().asFile
        cache.mkdirs()

        val changed = mutableListOf<KindChangedService>()
        for (service in services) {
            val hashFile = cache.resolve("${service.name}.hash")
            val fingerprint = KindSupport.artifactFingerprint(service.artifactDir, service.dockerfile)
            val previous = if (hashFile.exists()) hashFile.readText().trim() else ""
            if (fingerprint == previous) {
                logger.lifecycle("[${service.name}] artifact unchanged, skip image rebuild")
                cache.resolve("${service.name}.stamp").writeText(fingerprint)
                continue
            }
            val existed = KindSupport.deploymentExists(workdir, ns, service.deployment)
            logger.lifecycle("[${service.name}] building image ${service.image}:local ...")
            KindSupport.runCommand(
                workdir,
                KindSupport.cli("docker"), "build",
                "-f", service.dockerfile.path,
                "-t", "${service.image}:local",
                service.buildContext.path,
            )
            logger.lifecycle("[${service.name}] loading image into kind cluster $cluster ...")
            KindSupport.runCommand(
                workdir,
                KindSupport.cli("kind"), "load", "docker-image", "${service.image}:local",
                "--name", cluster,
            )
            changed += KindChangedService(service, fingerprint, existed)
        }

        val manifestsHash = KindSupport.manifestsFingerprint(
            k8sBaseDir.get().asFile,
            overlayDir.get().asFile,
        )
        val manifestsHashFile = cache.resolve("manifests.hash")
        val previousManifests =
            if (manifestsHashFile.exists()) manifestsHashFile.readText().trim() else ""
        val needApply = !KindSupport.namespaceExists(workdir, ns) || manifestsHash != previousManifests
        if (needApply) {
            logger.lifecycle("Applying Kubernetes manifests ...")
            KindSupport.runCommand(
                workdir,
                KindSupport.cli("kubectl"), "apply", "-k", overlayDir.get().asFile.path,
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
                KindSupport.runCommand(
                    workdir,
                    KindSupport.cli("kubectl"), "rollout", "restart",
                    "deployment/${item.service.deployment}",
                    "-n", ns,
                )
            } else {
                logger.lifecycle(
                    "[${item.service.name}] deployment did not exist before apply, skip restart",
                )
            }
            cache.resolve("${item.service.name}.hash").writeText(item.fingerprint)
            cache.resolve("${item.service.name}.stamp").writeText(item.fingerprint)
        }

        logger.lifecycle("")
        logger.lifecycle("ProCrush kind stack is up.")
        logger.lifecycle("Open:               http://127.10.0.10")
        logger.lifecycle("API health:         http://127.10.0.10/api/auth/me (401 without session is OK)")
        logger.lifecycle("")
        logger.lifecycle("Infra endpoints:    127.10.0.11:5432 postgres, 127.10.0.13:6379 redis, 127.10.0.14:5672 rabbitmq")
        logger.lifecycle("Observability:      http://127.10.0.16:3000 grafana (logs/traces), http://127.10.0.17:9090 prometheus")
    }
}
