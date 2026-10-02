plugins{
    `java-library`
}

sourceSets.main{
    resources.setSrcDirs(listOf("angle/"))
}

val lwjglVersion = "3.4.3"
val desktopNatives = listOf("natives-linux", "natives-linux-arm64", "natives-macos-arm64", "natives-windows")

fun lwjgl(module: String, classifiers: List<String>) =
    listOf("org.lwjgl:$module:$lwjglVersion") + classifiers.map{ "org.lwjgl:$module:$lwjglVersion:$it" }

val lwjglLibs = listOf("lwjgl", "lwjgl-sdl", "lwjgl-jemalloc", "lwjgl-opengl").flatMap{ lwjgl(it, desktopNatives) } +
    lwjgl("lwjgl-opengles", listOf("natives-windows"))

val natives = configurations.create("natives")

tasks.jar{
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(provider{ natives.map{ if(it.isDirectory) it else zipTree(it) } })
}

dependencies{
    lwjglLibs.forEach{
        natives(it)
        api(it)
    }
}
