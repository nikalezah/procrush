import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).copy(
                        port = 8082,
                        proxy =
                            mutableListOf(
                                KotlinWebpackConfig.DevServer.Proxy(
                                    context = mutableListOf("/api"),
                                    target = "http://localhost:8080",
                                    secure = false,
                                    changeOrigin = true,
                                ),
                            ),
                    )
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.app.shared)
            implementation(libs.compose.ui)
        }
    }
}
