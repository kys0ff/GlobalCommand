plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "off.kys.gcmd"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val nativeTarget = when {
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        else -> throw GradleException("gcmd only supports Linux on x64 and arm64 architectures.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "off.kys.gcmd.main"
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(libs.kotlinxSerializationJson)
        }
    }
}
