import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.copypathtool"
version = "1.4.1"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 以 IDEA Community 作为编译目标。插件只依赖平台模块（com.intellij.modules.platform），
        // 因此可安装到所有基于 IntelliJ Platform 的 IDE：
        // IntelliJ IDEA / PyCharm / GoLand / PhpStorm / WebStorm / CLion 等
        intellijIdeaCommunity("2024.2.4")
        // 平台测试框架，用于运行 BasePlatformTestCase 行为测试
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    // 平台测试框架的断言依赖
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

tasks.test {
    useJUnit()
}

intellijPlatform {
    // 本插件没有可搜索的配置项，关闭该任务可明显加快打包速度
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            // 支持 2024.1 及以上的所有 IntelliJ Platform 系列 IDE，不设上限
            sinceBuild = "241"
            untilBuild = provider { null }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
