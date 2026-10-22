plugins{
    java
}

val appName = providers.gradleProperty("appName").get()
val versionModifier = providers.gradleProperty("versionModifier").get()
val hasSprites = rootProject.extra["hasSprites"] as Boolean

@Suppress("UNCHECKED_CAST")
val deployName = rootProject.extra["deployName"] as (String) -> String

interface Injected{
    @get:Inject val execOps: ExecOperations
}
val injected = objects.newInstance<Injected>()

val steamworksVersion = "185e76cfe630a6f0e1dd7b9bf57be41f6064a1f2"

val mainClassName = "mindustry.desktop.DesktopLauncher"
val assetsDir = file("../core/assets")

val jdkDir = System.getenv("JDK_DIR") ?: ""
val iconFile = File("$rootDir/core/assets/icons/icon.icns")
val platforms = listOf("Linux64", "Windows64", "MacOS")

val debugModule = File(rootDir.parentFile, "Mindustry-Debug").exists() && !project.hasProperty("release")
val bundleId = mainClassName.substringBefore(".desktop") + ".mac"

//only on the classpath of the run task when launching the debugger, so it never ends up in dist
val debugRuntime = configurations.create("debugRuntime"){
    isCanBeConsumed = false
}

sourceSets.main{
    java.setSrcDirs(listOf("src/"))
}

tasks.compileJava{
    options.isFork = true
}

dependencies{
    implementation(project(":core"))
    implementation("com.github.Anuken:natives-desktop:frog")

    if(debugModule) debugRuntime(project(":debug"))

    implementation("com.github.Anuken:steamworks4j:$steamworksVersion")

    implementation("com.github.Anuken:backend-sdl3:frog")
}

tasks.register<JavaExec>("run"){
    dependsOn(tasks.classes)
    mainClass = mainClassName
    classpath = sourceSets.main.get().runtimeClasspath + files(Callable<Any>{
        if(args.contains("debug")) debugRuntime else emptyList()
    })
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true

    if(System.getProperty("os.name").lowercase().contains("mac")){
        jvmArgs("-XstartOnFirstThread")
    }

    jvmArgs("-XX:+ShowCodeDetailsInExceptionMessages")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    //fails on java 17
    //jvmArgs("--sun-misc-unsafe-memory-access=allow")

    project.findProperty("jvmArgs")?.toString()?.split(" ")?.filter{ it.isNotEmpty() }?.let{ jvmArgs(it) }
    project.findProperty("dataDir")?.let{ environment("MINDUSTRY_DATA_DIR", it) }

    doFirst{
        if(args?.contains("debug") == true){
            mainClass = "mindustry.debug.DebugLauncher"
        }
    }
}

tasks.register<Jar>("dist"){
    dependsOn(configurations.runtimeClasspath)
    dependsOn(":desktop:processResources")

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.map{ cfg -> cfg.map{ if(it.isDirectory) it else zipTree(it) } })
    from(files(assetsDir))
    exclude("config/**")
    exclude("**hs_err**.log")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    //don't include steam shared libraries unless necessary
    if(!versionModifier.contains("steam")){
        exclude("**steam**.so", "**steam**.dll", "**steam**.dylib", "**frogworks**.so", "**frogworks**.dll", "**frogworks**.dylib", "**libsdkencryptedappticket**")
    }

    archiveFileName = "$appName.jar"

    manifest{
        attributes["Main-Class"] = mainClassName
        //note: this doesn't do anything when launched from the bundled JVM
        attributes["Enable-Native-Access"] = "ALL-UNNAMED"
        attributes["Multi-Release"] = "true"
    }
}

if(!hasSprites && System.getenv("JITPACK") != "true"){
    println("Scheduling sprite packing.")
    tasks.named("run"){ dependsOn(":tools:pack") }
    tasks.named("dist"){ dependsOn(":tools:pack") }
}

//this is only for local testing
//add -Prelease -PversionModifier=steam as build properties
tasks.register("steamtest"){
    dependsOn("dist")
    doLast{
        copy{
            from("build/libs/Mindustry.jar")
            val destination = project.findProperty("destination")
            if(destination != null){
                into(destination)
            }else if(System.getProperty("os.name").contains("Mac")){
                into("/Users/anuke/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources")
            }else{
                into("/home/anuke/.steam/steam/steamapps/common/Mindustry/jre")
            }
            rename("Mindustry.jar", "desktop.jar")
        }
    }
}

//required templates:
//- Windows32: Not provided by Packr! This uses Java 8
//required JDKs:
//- Windows64
//- Linux64
//- Mac
platforms.forEach{ platform ->
    tasks.register("packr$platform"){
        dependsOn("dist")

        doLast{
            copy{
                into("build/packr/")
                rename("$appName.jar", "desktop.jar")
                from("build/libs/$appName.jar")
            }

            delete("build/packr/output/")

            val packrArgs = mutableListOf("java", "-jar", jdkDir + "packr.jar")

            packrArgs += listOf(
                "--platform", if(platform == "MacOS") "Mac" else platform,
                "--jdk", jdkDir + "jre-${platform.lowercase()}",
                "--executable", appName,
                "--classpath", "$rootDir/desktop/build/packr/desktop.jar",
                "--mainclass", mainClassName,
                "--verbose",
                "--bundle", bundleId,
                "--icon", iconFile.path,
                "--output", "$rootDir/desktop/build/packr/output",
                "--removelibs", "$rootDir/desktop/build/packr/desktop.jar"
            )

            packrArgs += "--vmargs"

            if(platform == "MacOS"){
                packrArgs += "XstartOnFirstThread"
            }

            packrArgs += "Dhttps.protocols=TLSv1.2,TLSv1.1,TLSv1"
            packrArgs += "XX:+ShowCodeDetailsInExceptionMessages"
            packrArgs += "enable-native-access=ALL-UNNAMED"
            packrArgs += "sun-misc-unsafe-memory-access=allow"

            injected.execOps.exec{
                commandLine(packrArgs)
                standardOutput = System.out
            }

            val outputJsonFile =
                if(platform != "MacOS") file("build/packr/output/Mindustry.json")
                else file("build/packr/output/$appName.app/Contents/Resources/Mindustry.json")

            if(platform != "MacOS"){
                copy{
                    into("build/packr/output/jre/")
                    from("build/packr/output/desktop.jar")
                }

                delete("build/packr/output/desktop.jar")

                outputJsonFile.writeText(outputJsonFile.readText().replace("desktop.jar", "jre/desktop.jar"))
            }else{
                copy{
                    into("build/packr/output/$appName.app/Contents/")
                    from("build/packr/output/Contents/")
                }

                delete("build/packr/output/Contents/")
            }

            //packr is broken and won't let me add one hyphen, so I have to do that myself later
            outputJsonFile.writeText(
                outputJsonFile.readText()
                    .replace("-enable-native-access=ALL-UNNAMED", "--enable-native-access=ALL-UNNAMED")
                    .replace("-sun-misc-unsafe-memory-access=allow", "--sun-misc-unsafe-memory-access=allow")
            )

            if(platform == "Windows64"){
                copy{
                    from("build/packr/output/jre/bin/msvcr100.dll")
                    into("build/packr/output/")
                    rename("msvcr100.dll", "MSVCR100.dll")
                }
            }

            if(versionModifier.contains("steam")){
                val lib = if(platform == "MacOS" || platform == "Linux64") "lib" else ""
                val extension = when(platform){
                    "Windows64" -> "64.dll"
                    "Linux64" -> ".so"
                    else -> ".dylib"
                }

                copy{
                    from(
                        zipTree(
                            if(platform == "MacOS") "build/packr/output/$appName.app/Contents/Resources/desktop.jar"
                            else "build/packr/output/jre/desktop.jar"
                        ).matching{
                            include("${lib}frogworks$extension")
                            include("${lib}steam_api$extension")
                        }
                    )
                    into(if(platform != "MacOS") "build/packr/output/" else "build/packr/output/$appName.app/Contents/Resources")
                }
            }

            copy{
                from("build/packr/output")
                into("../deploy/$platform")
            }
        }

        finalizedBy("zip$platform")
    }

    tasks.register<Zip>("zip$platform"){
        from("build/packr/output")
        archiveFileName = "${deployName(platform)}.zip"
        destinationDirectory.set(file("../deploy"))

        eachFile{
            if(file.canExecute()){
                permissions{ unix("rwxr-xr-x") }
            }
        }

        doLast{
            delete("build/packr/")
        }
    }
}
