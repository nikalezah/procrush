package jobs.procrush.gradle.kind

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class KindDownTask : DefaultTask() {

    @get:Internal
    abstract val workDir: DirectoryProperty

    @get:Internal
    abstract val cacheDir: DirectoryProperty

    @get:Internal
    abstract val clusterName: Property<String>

    init {
        group = "kind"
        description = "Delete the local kind cluster"
    }

    @TaskAction
    fun run() {
        val workdir = workDir.get().asFile
        val cluster = clusterName.get()
        KindSupport.requireCli(workdir, "kind")
        if (KindSupport.clusterExists(workdir, cluster)) {
            logger.lifecycle("Deleting kind cluster $cluster ...")
            KindSupport.runCommand(
                workdir,
                KindSupport.cli("kind"), "delete", "cluster", "--name", cluster,
            )
            KindSupport.clearKindDeployCache(cacheDir.get().asFile, logger)
        } else {
            logger.lifecycle("Kind cluster $cluster does not exist.")
        }
    }
}
