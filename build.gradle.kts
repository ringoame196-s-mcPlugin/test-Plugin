import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import java.net.HttpURLConnection
import java.net.URL

val minecraftVersion: String by project
val pluginVersion: String by project
val fullVersion = "$minecraftVersion-$pluginVersion"

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.7"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jmailen.kotlinter") version "4.4.0"
}

group = "com.github.ringoame196"
version = fullVersion

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

// ── 【ここを追加！】Paper API への依存関係 ──
dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

detekt {
    toolVersion = "1.23.6"
    source.setFrom("src/main/kotlin", "src/main/java")
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    ignoreFailures = false
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    relocate("kotlin", "${project.group}.libs.kotlin")
    relocate("org.intellij.lang.annotations", "${project.group}.libs.org.intellij.lang.annotations")
    relocate("org.jetbrains.annotations", "${project.group}.libs.org.jetbrains.annotations")
}

// 自動デプロイタスク（JARコピー & サーバーAPI通知）
val deployPlugin by tasks.registering {
    group = "deployment"
    description = "ビルドされたJARをサーバープラグインフォルダへコピーし、通知APIを呼び出します"

    dependsOn("shadowJar")

    val isCI = System.getenv("CI") != null
    val copyDirPath = if (isCI) {
        layout.buildDirectory.dir("deploy").get().asFile.path
    } else {
        "Z:/minecraft/TwitterServer/plugins/"
    }
    val targetDir = file(copyDirPath)

    doLast {
        if (isCI) {
            logger.lifecycle("CI環境のため、プラグインの自動デプロイとAPI通知をスキップします。")
            return@doLast
        }

        if (!targetDir.exists() || !targetDir.isDirectory) {
            logger.warn("自動コピーをスキップしました: ディレクトリが存在しません (${targetDir.path})")
            return@doLast
        }

        val jarFile = layout.buildDirectory.file("libs/${project.name}-$fullVersion.jar").get().asFile
        if (jarFile.exists()) {
            copy {
                from(jarFile)
                into(targetDir)
            }
            logger.lifecycle("プラグインをコピーしました: ${jarFile.name} -> ${targetDir.path}")
        } else {
            logger.error("コピー対象のJARファイルが見つかりません: ${jarFile.path}")
            return@doLast
        }

        // サーバーへのリロード通知API呼び出し
        val port = 25585
        val ip = "ringoame-server"
        val apiUrl = "http://$ip:$port/plugin?name=${project.name}"

        try {
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 2000
                readTimeout = 2000
                requestMethod = "GET"
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                logger.lifecycle("API Response: $response")
            } else {
                logger.warn("Server responded with error code: ${connection.responseCode}")
            }
            connection.disconnect()
        } catch (e: java.net.ConnectException) {
            logger.warn("Warning: サーバーに接続できません（オフラインの可能性があります）")
        } catch (e: java.net.SocketTimeoutException) {
            logger.warn("Warning: 接続がタイムアウトしました")
        } catch (e: Exception) {
            logger.warn("Warning: API通信で予期しないエラーが発生しました: ${e.message}")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

paper {
    main = "${project.group}.main.Main"
    apiVersion = "1." + minecraftVersion.split(".")[1]
    author = "ringoame196"
    website = "https://github.com/ringoame196-s-mcPlugin"
    description = "Minecraft plugin template"

    // パーミッションの設定例
    permissions {
        register("${project.group}.command.sample") {
            description = "サンプルコマンドを実行する権限"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
        finalizedBy(deployPlugin)
    }
}

tasks.register("printVersion") {
    doLast {
        println(fullVersion)
    }
}