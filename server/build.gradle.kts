plugins{
    java
    `maven-publish`
}

val appName = providers.gradleProperty("appName").get()
val versionString = rootProject.extra["versionString"] as String

@Suppress("UNCHECKED_CAST")
val deployName = rootProject.extra["deployName"] as (String) -> String

val mainClassName = "mindustry.server.ServerLauncher"
val assetsDir = file("../core/assets")
val serverFolder = "../deploy/$appName-server-$versionString"

sourceSets.main{
    java.setSrcDirs(listOf("src/"))
}

dependencies{
    implementation(project(":core"))
    implementation("com.github.Anuken:backend-headless:frog")
    implementation("org.jline:jline:4.0.0")
}

tasks.register<JavaExec>("run"){
    dependsOn(tasks.classes)
    mainClass = mainClassName
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true
    project.findProperty("jvmArgs")?.toString()?.split(" ")?.filter{ it.isNotEmpty() }?.let{ jvmArgs(it) }
}

tasks.register<JavaExec>("debug"){
    dependsOn(tasks.classes)
    mainClass = mainClassName
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true
    debug = true
}

tasks.register<Jar>("dist"){
    dependsOn(configurations.runtimeClasspath, tasks.classes)

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.map{ cfg -> cfg.map{ if(it.isDirectory) it else zipTree(it) } })
    from(files(assetsDir))
    exclude("bloomshaders/**")
    exclude("sprites/**")
    exclude("music/**")
    exclude("sounds/**")
    exclude("fonts/**")
    exclude("bundles/**")
    exclude("cubemaps/**")
    exclude("vfxshaders/**")
    exclude("config/**")
    exclude("cursors/**")
    exclude("shaders/**")
    exclude("icons/icon.icns")
    exclude("icons/icon.ico")
    exclude("icons/icon_64.png")
    exclude("**hs_err**.log")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest{
        attributes["Main-Class"] = mainClassName
    }
}

tasks.register<Zip>("dzip"){
    from(serverFolder)
    archiveFileName = "${deployName("server")}.zip"
    destinationDirectory.set(file("../deploy/"))

    finalizedBy("cleanup")
}

tasks.register("cleanup"){
    doLast{
        delete(serverFolder)
    }
}

tasks.register("deploy"){
    dependsOn("dist")

    finalizedBy("dzip")

    doLast{
        copy{
            from("build/libs/server-release.jar")
            into(serverFolder)
            rename("server-release.jar", "server.jar")
        }

        copy{
            from("server_template")
            into(serverFolder)
        }
    }
}

java{
    withJavadocJar()
    withSourcesJar()
}

publishing{
    publications{
        create<MavenPublication>("maven"){
            from(components["java"])
        }
    }
}
