import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("yokai.android.library")
    kotlin("multiplatform")
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidTarget()
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.i18n)

                // Logging
                api(libs.bundles.logging)

                api(libs.okio)

                api(libs.rxjava)
                api(project.dependencies.enforcedPlatform(kotlinx.coroutines.bom))
                api(kotlinx.coroutines.core)
                api(kotlinx.serialization.json)
                api(kotlinx.serialization.json.okio)

                implementation(libs.jsoup)
            }
        }
        androidMain {
            dependencies {
                // Dependency injection
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)

                // Network client
                api(libs.okhttp)
                api(libs.okhttp.logging.interceptor)
                api(libs.okhttp.dnsoverhttps)
                api(libs.okhttp.brotli)

                api(androidx.preference)

                implementation(libs.quickjs.android)

                api(libs.unifile)

                implementation(libs.libarchive)
            }
        }
        // iosMain {
        //     dependencies {
        //     }
        // }
    }
}

android {
    namespace = "yokai.core.main"
}

tasks {
    withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
