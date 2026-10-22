import java.util.*

pluginManagement{
    repositories{
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

include("desktop", "core", "server", "ios", "annotations", "tools", "tests")

var hasSdk = System.getenv("ANDROID_HOME") != null

val localProperties = Properties()
val localPropertiesFile = File(settingsDir, "local.properties")
if(localPropertiesFile.exists()){
    localPropertiesFile.inputStream().use{ localProperties.load(it) }
    if(localProperties.containsKey("sdk.dir")) hasSdk = true
}

if(hasSdk){
    include("android")
}else{
    println("No Android SDK found. Skipping Android module.")
}

fun flagEnabled(name: String) =
    providers.gradleProperty(name).isPresent || localProperties.getProperty(name, "false") == "true"

if(flagEnabled("localRhino")){
    includeBuild("../rhino")
}

if(flagEnabled("localSteamworks")){
    includeBuild("../steamworks4j")
}

includeBuild("arc")

if(File(rootDir.parentFile, "Mindustry-Debug").exists()){
    include(":debug")
    project(":debug").projectDir = File(rootDir.parentFile, "Mindustry-Debug")
}
