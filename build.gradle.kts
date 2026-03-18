
plugins {
    kotlin("multiplatform") version "2.3.20"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.16"
}

group = "org.jetbrains.kotlinx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain(17)

    macosArm64 {
        val mainCompilation = compilations.getByName("main")
        compilations.create("benchmarkBaseline") {
            associateWith(mainCompilation)
            defaultSourceSet.dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.29.0")
            }
        }
        compilations.create("benchmarkMainline") {
            associateWith(mainCompilation)
            defaultSourceSet.dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.30.0")
            }
        }
        compilations.create("benchmarkFix1") {
            associateWith(mainCompilation)
            defaultSourceSet.dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.31.0-SNAPSHOT")
            }
        }
        compilations.create("benchmarkFix2") {
            associateWith(mainCompilation)
            defaultSourceSet.dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.32.0-SNAPSHOT")
            }
        }
    }

    jvm {
        val mainCompilation = compilations.getByName("main")
        compilations.create("benchmarkReentrantLockFair") {
            associateWith(mainCompilation)
        }
        compilations.create("benchmarkReentrantLockUnfair") {
            associateWith(mainCompilation)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.16")
            }
        }
    }
}

benchmark {
    targets {
        register("macosArm64BenchmarkBaseline")
        register("macosArm64BenchmarkMainline")
        register("macosArm64BenchmarkFix1")
        register("macosArm64BenchmarkFix2")
        register("jvmBenchmarkReentrantLockFair")
        register("jvmBenchmarkReentrantLockUnfair")
    }
}
