pluginManagement {
    repositories {
        // 国内镜像优先，加速依赖下载
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "jetbrains-copy-path-tool"
