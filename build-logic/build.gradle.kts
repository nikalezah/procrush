plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle)
}

gradlePlugin {
    plugins {
        register("procrushRoot") {
            id = "procrush.root"
            implementationClass = "jobs.procrush.gradle.ProcrushRootPlugin"
        }
        register("procrushApi") {
            id = "procrush.api"
            implementationClass = "jobs.procrush.gradle.ProcrushApiPlugin"
        }
    }
}
