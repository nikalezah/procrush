package jobs.procrush.gradle

import jobs.procrush.gradle.frontend.FrontendBuildTask
import jobs.procrush.gradle.i18n.GenerateI18nTask
import jobs.procrush.gradle.i18n.NodeAvailableValueSource
import jobs.procrush.gradle.kind.KindDownTask
import jobs.procrush.gradle.kind.KindServiceSpec
import jobs.procrush.gradle.kind.KindUpTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class ProcrushRootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "procrush.root must be applied to the root project"
        }

        // Pin JVM 25 so local installDist matches eclipse-temurin:25-jre images.
        target.subprojects {
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                extensions.configure<KotlinJvmProjectExtension>("kotlin") {
                    jvmToolchain(25)
                }
            }
        }

        val layout = target.layout
        val providers = target.providers
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val nodeCmd = if (isWindows) "node.exe" else "node"
        val npmCmd = if (isWindows) "npm.cmd" else "npm"

        val nodeAvailable = providers.of(NodeAvailableValueSource::class.java) {}

        val generateI18n = target.tasks.register("generateI18n", GenerateI18nTask::class.java) {
            nodeCommand.set(nodeCmd)
            this.nodeAvailable.set(nodeAvailable)
            workingDir.set(layout.projectDirectory.dir("i18n"))
            errorCodesYaml.set(layout.projectDirectory.file("i18n/error-codes.yaml"))
            localesDir.set(layout.projectDirectory.dir("i18n/locales"))
            generateScript.set(layout.projectDirectory.file("i18n/scripts/generate.mjs"))
            validateScript.set(layout.projectDirectory.file("i18n/scripts/validate.mjs"))
            generatedKotlin.set(
                layout.projectDirectory.file("i18n/generated/kotlin/jobs/procrush/i18n/ErrorCode.kt"),
            )
            generatedTypescript.set(
                layout.projectDirectory.file("i18n/generated/typescript/errorCodes.ts"),
            )
        }

        val frontendBuild = target.tasks.register("frontendBuild", FrontendBuildTask::class.java) {
            dependsOn(generateI18n)
            npmCommand.set(npmCmd)
            workingDir.set(layout.projectDirectory.dir("frontend"))
            frontendSources.from(
                target.fileTree("frontend") {
                    exclude("node_modules/**", "dist/**", ".vite/**")
                },
            )
            localesDir.set(layout.projectDirectory.dir("i18n/locales"))
            generatedTypescriptDir.set(layout.projectDirectory.dir("i18n/generated/typescript"))
            errorCodesYaml.set(layout.projectDirectory.file("i18n/error-codes.yaml"))
            openapiSources.from(
                target.fileTree("openapi") {
                    exclude("**/dist/**")
                },
            )
            outputDir.set(layout.projectDirectory.dir("frontend/dist"))
        }

        val clusterName = providers.environmentVariable("KIND_CLUSTER_NAME")
            .orElse("procrush")
        val kindNamespace = "procrush"
        val kindIngressUrl =
            "https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/kind/deploy.yaml"
        val kindK8sDir = layout.projectDirectory.dir("deploy/k8s")
        val kindOverlayDir = kindK8sDir.dir("overlays/kind")
        val kindDeployCacheDir = layout.projectDirectory.dir(".kind-deploy-cache")

        target.tasks.register("kindUp", KindUpTask::class.java) {
            dependsOn(
                generateI18n,
                ":backend:api:installDist",
                ":backend:personality:installDist",
                ":backend:matching:installDist",
                frontendBuild,
            )
            workDir.set(layout.projectDirectory)
            cacheDir.set(kindDeployCacheDir)
            secretFile.set(kindK8sDir.file("base/secret.yaml"))
            kindConfigFile.set(kindK8sDir.file("kind-config.yaml"))
            k8sBaseDir.set(kindK8sDir.dir("base"))
            overlayDir.set(kindOverlayDir)
            this.clusterName.set(clusterName)
            namespace.set(kindNamespace)
            ingressUrl.set(kindIngressUrl)
            services = listOf(
                    KindServiceSpec(
                        name = "api",
                        image = "procrush-api",
                        deployment = "api",
                        artifactDir = layout.projectDirectory.dir("backend/api/build/install/api").asFile,
                        dockerfile = layout.projectDirectory.file("deploy/Dockerfile.api.dev").asFile,
                        buildContext = layout.projectDirectory.dir("backend/api/build/install/api").asFile,
                    ),
                    KindServiceSpec(
                        name = "personality",
                        image = "procrush-personality",
                        deployment = "personality",
                        artifactDir = layout.projectDirectory.dir("backend/personality/build/install/personality").asFile,
                        dockerfile = layout.projectDirectory.file("deploy/Dockerfile.personality.dev").asFile,
                        buildContext = layout.projectDirectory.dir("backend/personality/build/install/personality").asFile,
                    ),
                    KindServiceSpec(
                        name = "matching",
                        image = "procrush-matching",
                        deployment = "matching",
                        artifactDir = layout.projectDirectory.dir("backend/matching/build/install/matching").asFile,
                        dockerfile = layout.projectDirectory.file("deploy/Dockerfile.matching.dev").asFile,
                        buildContext = layout.projectDirectory.dir("backend/matching/build/install/matching").asFile,
                    ),
                    KindServiceSpec(
                        name = "frontend",
                        image = "procrush-frontend",
                        deployment = "frontend",
                        artifactDir = layout.projectDirectory.dir("frontend/dist").asFile,
                        dockerfile = layout.projectDirectory.file("deploy/Dockerfile.frontend.dev").asFile,
                        buildContext = layout.projectDirectory.dir("frontend").asFile,
                    ),
                )
        }

        target.tasks.register("kindDown", KindDownTask::class.java) {
            workDir.set(layout.projectDirectory)
            cacheDir.set(kindDeployCacheDir)
            this.clusterName.set(clusterName)
        }
    }
}
