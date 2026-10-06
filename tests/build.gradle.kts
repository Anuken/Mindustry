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

    create("clientDriver"){
        java.setSrcDirs(listOf("src/driver/java"))
    }

    create("serverTest"){
        java.setSrcDirs(listOf("src/server/java"))
        resources.setSrcDirs(listOf("src/server/resources", "src/test/resources"))
    }

    create("netTest"){
        java.setSrcDirs(listOf("src/net/java"))
    }
}

//dependencies that every test source set needs; the source sets below only add what is specific to them
val testCommon = configurations.create("testCommon"){
    isCanBeResolved = false
    isCanBeConsumed = false
}

val testCommonRuntime = configurations.create("testCommonRuntime"){
    isCanBeResolved = false
    isCanBeConsumed = false
}

configurations{
    for(name in listOf("test", "clientTest", "serverTest")){
        named("${name}Implementation"){ extendsFrom(testCommon) }
        named("${name}RuntimeOnly"){ extendsFrom(testCommonRuntime) }
    }
}

dependencies{
    testCommon(project(":core"))
    testCommon("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testCommon("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testCommonRuntime("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testCommonRuntime("org.junit.platform:junit-platform-launcher")

    testImplementation("com.github.Anuken:backend-headless:frog")

    //graphical client tests
    "clientTestImplementation"("com.github.Anuken:backend-sdl3:frog")
    "clientTestImplementation"("com.github.Anuken:natives-desktop:frog")

    //interactive client driver: compiled against everything the client tests have, including their harness classes
    "clientDriverImplementation"(files(sourceSets["clientTest"].runtimeClasspath))

    //real headless server tests
    "serverTestImplementation"(project(":server"))
    "serverTestImplementation"("com.github.Anuken:backend-headless:frog")
    "serverTestImplementation"("org.jline:jline:4.0.0")

    //server + real client tests: everything the server tests have. The client runs in a child process, see DriverClient
    "netTestImplementation"(files(sourceSets["serverTest"].runtimeClasspath))
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

//Boots the real server, hosts a map, and reads console commands from stdin. For connecting a client by hand.
//Run with: gradle tests:serverSpike --console=plain   (-Pservertest.port=N to change the port, default 6567)
tasks.register<JavaExec>("serverSpike"){
    group = "verification"
    description = "Hosts a map on a real server for manual client experiments."

    classpath = sourceSets["serverTest"].runtimeClasspath
    mainClass = "mindustry.server.ServerSpike"
    workingDir = file("../core/assets")
    standardInput = System.`in`
    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError")
    systemProperty("servertest.data.dir", layout.buildDirectory.dir("server_spike_data").get().asFile.absolutePath)
    systemProperty("servertest.port", (findProperty("servertest.port") ?: "6567").toString())
}

//Graphical tasks run on a private Xvfb, never on the user's desktop. -PclientTest.hostDisplay uses the real display instead. Linux only.
val virtualDisplays = mutableListOf<Process>()

fun useVirtualDisplay(options: ProcessForkOptions){
    if(!System.getProperty("os.name").lowercase().contains("linux")){
        throw GradleException("The graphical client tools are only supported on Linux.")
    }

    options.environment("LIBGL_ALWAYS_SOFTWARE", "1")
    options.environment("SDL_VIDEO_DRIVER", "x11")
    options.environment("SDL_VIDEODRIVER", "x11")

    if(project.hasProperty("clientTest.hostDisplay") || System.getenv("CLIENT_TEST_HOST_DISPLAY") != null){
        if(System.getenv("DISPLAY") == null) throw GradleException("-PclientTest.hostDisplay needs \$DISPLAY to be set.")
        return
    }

    //always a private display, even if $DISPLAY is set
    val builder = ProcessBuilder("Xvfb", "-displayfd", "1", "-screen", "0", "1920x1080x24", "-nolisten", "tcp", "+extension", "GLX")
    builder.redirectError(ProcessBuilder.Redirect.DISCARD)

    val process = try{
        builder.start()
    }catch(e: java.io.IOException){
        throw GradleException("Xvfb could not be started (apt install xvfb), or use -PclientTest.hostDisplay.", e)
    }
    virtualDisplays.add(process)

    val display = process.inputStream.bufferedReader().readLine() ?: throw GradleException("Xvfb exited without reporting a display.")
    options.environment("DISPLAY", ":" + display.trim())
    logger.lifecycle("Started private Xvfb on :" + display.trim())
}

//runs even if the task failed
tasks.register("virtualDisplayStop"){
    doLast{
        virtualDisplays.forEach{ it.destroy() }
        virtualDisplays.clear()
    }
}

val clientJvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError", "-XX:+ShowCodeDetailsInExceptionMessages", "--enable-native-access=ALL-UNNAMED")

//Boots the real graphical client and serves commands on 127.0.0.1:8765, see driver/CLIENT_TESTING.md
//Run with: gradle tests:clientDriver --console=plain   (or driver/client-driver.sh start)
tasks.register<JavaExec>("clientDriver"){
    group = "verification"
    description = "Runs the real client on a private virtual display, controlled over HTTP."

    classpath = sourceSets["clientDriver"].runtimeClasspath
    mainClass = "ClientDriver"
    workingDir = file("../core/assets")

    jvmArgs = clientJvmArgs
    val width = (findProperty("clientdriver.width") ?: "1280").toString()
    val height = (findProperty("clientdriver.height") ?: "720").toString()
    systemProperty("clienttest.data.dir", layout.buildDirectory.dir("client_driver_data").get().asFile.absolutePath)
    systemProperty("clienttest.out.dir", layout.buildDirectory.dir("client_test_output").get().asFile.absolutePath)
    systemProperty("clientdriver.port", (findProperty("clientdriver.port") ?: "8765").toString())
    systemProperty("clienttest.width", width)
    systemProperty("clienttest.height", height)
    systemProperty("clientdriver.width", width)
    systemProperty("clientdriver.height", height)

    val self = this
    doFirst{ useVirtualDisplay(self) }
    finalizedBy("virtualDisplayStop")
}

//Server tests that also run a real client: the server lives in the test JVM, the client in a child JVM started by DriverClient.
//Run with: gradle tests:netTest
tasks.register<Test>("netTest"){
    group = "verification"
    description = "Runs tests against a real server and a real graphical client."

    testClassesDirs = sourceSets["netTest"].output.classesDirs
    classpath = sourceSets["netTest"].runtimeClasspath
    //the child process runs ClientDriver from this classpath, so it has to be built
    dependsOn(sourceSets["clientDriver"].runtimeClasspath)

    //one server per JVM: Vars and Core are static
    forkEvery = 1
    maxParallelForks = 1
    outputs.upToDateWhen{ false }
    outputs.cacheIf{ false }
    useJUnitPlatform()
    workingDir = file("../core/assets")

    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError")
    systemProperty("servertest.data.dir", layout.buildDirectory.dir("net_test_server_data").get().asFile.absolutePath)
    systemProperty("nettest.data.dir", layout.buildDirectory.dir("net_test_client_data").get().asFile.absolutePath)
    systemProperty("nettest.out.dir", layout.buildDirectory.dir("client_test_output").get().asFile.absolutePath)

    testLogging{
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }

    val self = this
    doFirst{
        systemProperty("nettest.driver.classpath", sourceSets["clientDriver"].runtimeClasspath.asPath)
        //the child process inherits this environment
        useVirtualDisplay(self)
    }
    finalizedBy("virtualDisplayStop")
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

tasks.register("testAll"){
    dependsOn("test")
    dependsOn("clientTest")
    dependsOn("serverTest")
    dependsOn("netTest")
}
