import org.gradle.api.tasks.testing.logging.*

plugins{
    java
}

sourceSets{
    test{
        resources.setSrcDirs(listOf("src/test/resources"))
    }

    create("clientTest"){
        java.setSrcDirs(listOf("src/client/java"))
        resources.setSrcDirs(listOf("src/client/resources", "src/test/resources"))
    }

    create("serverTest"){
        java.setSrcDirs(listOf("src/server/java"))
        resources.setSrcDirs(listOf("src/server/resources", "src/test/resources"))
    }
}

dependencies{
    testImplementation(project(":core"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("com.github.Anuken:backend-headless:frog")
    testImplementation("org.json:json:20230618")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    //graphical client tests
    "clientTestImplementation"(project(":core"))
    "clientTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.1")
    "clientTestImplementation"("org.junit.jupiter:junit-jupiter-params:5.7.1")
    "clientTestImplementation"("com.github.Anuken:backend-sdl3:frog")
    "clientTestImplementation"("com.github.Anuken:natives-desktop:frog")
    "clientTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    "clientTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")

    //real headless server tests
    "serverTestImplementation"(project(":core"))
    "serverTestImplementation"(project(":server"))
    "serverTestImplementation"("com.github.Anuken:backend-headless:frog")
    "serverTestImplementation"("org.jline:jline:4.0.0")
    "serverTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.1")
    "serverTestImplementation"("org.junit.jupiter:junit-jupiter-params:5.7.1")
    "serverTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    "serverTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.test{
    //fork every test so mods don't interact with each other
    forkEvery = 1
    //always run tests; cached or up-to-date results can be stale when tests depend on files outside the inputs
    outputs.upToDateWhen{ false }
    outputs.cacheIf{ false }
    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError")
    useJUnitPlatform()
    workingDir = file("../core/assets")
    testLogging{
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

//Boots the real dedicated server (ServerLauncher) in a headless application. Run with: gradle tests:serverTest
tasks.register<Test>("serverTest"){
    group = "verification"
    description = "Runs tests against a real headless server instance."

    testClassesDirs = sourceSets["serverTest"].output.classesDirs
    classpath = sourceSets["serverTest"].runtimeClasspath

    //one server per JVM: Vars and Core are static
    forkEvery = 1
    maxParallelForks = 1
    outputs.upToDateWhen{ false }
    outputs.cacheIf{ false }
    useJUnitPlatform()
    workingDir = file("../core/assets")

    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError")
    systemProperty("servertest.data.dir", layout.buildDirectory.dir("server_test_data").get().asFile.absolutePath)

    testLogging{
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

//Runs the desktop client in a window. Linux only. Needs a display: uses $DISPLAY if set, otherwise starts a private Xvfb.
//Rendering is done in software (Mesa llvmpipe), no GPU needed. Required packages on Debian/Ubuntu: xvfb libgl1-mesa-dri libglx-mesa0
//Run with: gradle tests:clientTest
//Screenshots end up in tests/build/client_test_output
var xvfbProcess: Process? = null

tasks.register<Test>("clientTest"){
    group = "verification"
    description = "Runs tests against the real graphical client (needs a display, or Xvfb)."

    testClassesDirs = sourceSets["clientTest"].output.classesDirs
    classpath = sourceSets["clientTest"].runtimeClasspath

    //one client per JVM: Vars and Core are static
    forkEvery = 1
    maxParallelForks = 1
    outputs.upToDateWhen{ false }
    outputs.cacheIf{ false }
    useJUnitPlatform()
    workingDir = file("../core/assets")

    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError", "-XX:+ShowCodeDetailsInExceptionMessages", "--enable-native-access=ALL-UNNAMED")
    systemProperty("clienttest.data.dir", layout.buildDirectory.dir("client_test_data").get().asFile.absolutePath)
    systemProperty("clienttest.out.dir", layout.buildDirectory.dir("client_test_output").get().asFile.absolutePath)

    //software rendering, and x11 even if the machine runs wayland
    environment("LIBGL_ALWAYS_SOFTWARE", "1")
    environment("SDL_VIDEO_DRIVER", "x11")
    environment("SDL_VIDEODRIVER", "x11")

    testLogging{
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }

    doFirst{
        if(!System.getProperty("os.name").lowercase().contains("linux")){
            throw GradleException("clientTest is only supported on Linux.")
        }

        if(System.getenv("DISPLAY") == null){
            //-displayfd 1 makes Xvfb pick a free display number and print it to stdout
            val builder = ProcessBuilder("Xvfb", "-displayfd", "1", "-screen", "0", "1920x1080x24", "-nolisten", "tcp", "+extension", "GLX")
            builder.redirectError(ProcessBuilder.Redirect.DISCARD)

            val process = try{
                builder.start()
            }catch(e: java.io.IOException){
                throw GradleException("DISPLAY is not set and Xvfb could not be started (is it installed? apt install xvfb). Either install it or run under xvfb-run / a real display.", e)
            }
            xvfbProcess = process

            val display = process.inputStream.bufferedReader().readLine()
                ?: throw GradleException("Xvfb exited without reporting a display.")
            environment("DISPLAY", ":" + display.trim())
            logger.lifecycle("Started Xvfb on :" + display.trim())
        }

        delete(layout.buildDirectory.dir("client_test_output").get().asFile)
    }

    finalizedBy("clientTestStopXvfb")
}

//runs even if clientTest failed
tasks.register("clientTestStopXvfb"){
    doLast{
        xvfbProcess?.destroy()
        xvfbProcess = null
    }
}
