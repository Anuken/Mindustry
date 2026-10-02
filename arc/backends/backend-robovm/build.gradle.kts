import de.undercouch.gradle.tasks.download.*

sourceSets.main{
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("res"))
}

val robovmVersion = "2.3.26"

dependencies{
    listOf("robovm-rt", "robovm-objc", "robovm-cocoatouch").forEach{ api("com.mobidevelop.robovm:$it:$robovmVersion") }
}

tasks.register<Download>("fetchMetalANGLEKit"){
    doFirst{
        file("build/tmp").mkdirs()
    }
    src("https://github.com/libgdx/MetalANGLEKit/releases/download/v1.2.1/metalanglekit.zip")
    dest("build/tmp/MetalANGLEKit.zip")
    onlyIfModified(true)
    useETag("all")
}

tasks.register<Verify>("verifyMetalANGLEKit"){
    dependsOn("fetchMetalANGLEKit")
    src("build/tmp/MetalANGLEKit.zip")
    algorithm("SHA-256")
    checksum("c7785cbe15eb9e5962677513725c8f0e33039f344235cc7691ee5ac35ff5ea91")
}

tasks.register<Copy>("extractMetalANGLEKit"){
    dependsOn("verifyMetalANGLEKit")

    doFirst{
        delete("res/META-INF/robovm/ios/libs/")
        file("res/META-INF/robovm/ios/libs").mkdirs()
    }
    from(zipTree("build/tmp/MetalANGLEKit.zip"))
    into("res/META-INF/robovm/ios/libs/")
}
