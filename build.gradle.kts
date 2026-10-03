buildscript{
    repositories{
        mavenLocal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }

    dependencies{
        classpath("com.github.Anuken:arc-core:frog")
    }
}

plugins{
    id("org.jetbrains.kotlin.jvm") version "2.4.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.4.20" apply false
    //intelliJ only supports 9.1.0 as of the latest 2026 release
    id("com.android.application") version "9.1.0" apply false
}

val appName = providers.gradleProperty("appName").get()
val versionNumber = providers.gradleProperty("versionNumber").get()
val versionModifier = providers.gradleProperty("versionModifier").get()
val buildVersion = providers.gradleProperty("buildversion").getOrElse("custom build")
val modifierString = if(versionModifier != "release") "[${versionModifier.uppercase()}]" else ""

extra["buildVersion"] = buildVersion
extra["versionString"] = "$versionNumber-$versionModifier-$buildVersion"
extra["hasSprites"] = File(rootDir, "core/assets/sprites/sprites.aatls").exists() && File(rootDir, "core/build/last_pack_version").let{ it.exists() && it.readText() == versionNumber }
extra["deployName"] = { platform: String ->
    var name = if(platform == "windows") "windows64" else platform
    name = name.replaceFirstChar{ it.uppercase() }
    if(name.endsWith("64") || name.endsWith("32")){
        name = "${name.dropLast(2)}-${name.takeLast(2)}bit"
    }
    "[$name]$modifierString[v$buildVersion]$appName"
}

allprojects{
    apply(plugin = "maven-publish")

    version = findProperty("packageVersion") ?: "release"
    group = "com.github.Anuken"

    repositories{
        mavenLocal()
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots")
        maven("https://jitpack.io")
    }

    val clearCache = tasks.register("clearCache"){
        doFirst{
            delete("$rootDir/core/assets/cache")
        }
    }

    tasks.withType<JavaCompile>().configureEach{
        targetCompatibility = "17"
        sourceCompatibility = "17"
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
        dependsOn(clearCache)

        options.forkOptions.jvmArgs = options.forkOptions.jvmArgs.orEmpty() + listOf(
            "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-opens=java.base/sun.reflect.annotation=ALL-UNNAMED"
        )
    }
}

//--release can't be combined with the --add-exports the annotation processors need, so skip that project
configure(subprojects.filter{ it.name != "annotations" }){
    tasks.withType<JavaCompile>().configureEach{
        options.compilerArgs.addAll(listOf("--release", "17"))
    }

    tasks.withType<Javadoc>().configureEach{
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply{
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("-release", "17")
        }
    }
}

val cleanDeployOutput = tasks.register("cleanDeployOutput"){
    doFirst{
        if(buildVersion == "custom build" || buildVersion == ""){
            throw IllegalArgumentException("----\n\nSET A BUILD NUMBER FIRST!\n\n----")
        }
        if(!project.hasProperty("release")){
            throw IllegalArgumentException("----\n\nSET THE RELEASE PROJECT PROPERTY FIRST!\n\n----")
        }

        delete("deploy/")
    }
}

tasks.register("deployAll"){
    dependsOn(cleanDeployOutput)
    dependsOn("desktop:packrLinux64")
    dependsOn("desktop:packrWindows64")
    dependsOn("desktop:packrMacOS")
    if(versionModifier != "steam"){
        dependsOn("server:deploy")
        dependsOn("android:deploy")
    }
}

tasks.register("resolveDependencies"){
    doLast{
        rootProject.allprojects.forEach{ p ->
            val configurations = p.buildscript.configurations + p.configurations
            configurations.filter{ it.isCanBeResolved }.forEach{ it.resolve() }
        }
    }
}
