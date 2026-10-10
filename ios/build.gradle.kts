import java.util.*

buildscript{
    repositories{
        mavenCentral()
    }

    dependencies{
        classpath("com.mobidevelop.robovm:robovm-gradle-plugin:2.3.26")
    }
}

plugins{
    java
}

apply(plugin = "robovm")

val versionNumber = providers.gradleProperty("versionNumber").get()
val buildVersion = rootProject.extra["buildVersion"] as String

run{
    val vfile = file("robovm.properties")
    val props = Properties()
    if(vfile.exists()){
        vfile.inputStream().use{ props.load(it) }
    }else{
        props["app.id"] = "io.anuke.mindustry"
        props["app.version"] = "7.0"
        props["app.mainclass"] = "mindustry.ios.IOSLauncher"
        props["app.executable"] = "IOSLauncher"
        props["app.name"] = "Mindustry"
    }

    props["app.build"] = (if(!props.containsKey("app.build")) 40 else props.getProperty("app.build").toInt() + 1).toString()
    if(buildVersion != "custom build"){
        props["app.version"] = versionNumber + "." + buildVersion + (if(buildVersion.contains(".")) "" else ".0")
    }
    vfile.bufferedWriter().use{ props.store(it, null) }
}

tasks.register("incrementConfig")

dependencies{
    implementation(project(":core"))

    implementation("com.github.Anuken:backend-robovm:frog")

    compileOnly(project(":annotations"))
}

sourceSets.main{
    java.setSrcDirs(listOf("src/"))
}

tasks.register("deploy"){
    dependsOn("createIPA")
}

tasks.named("launchIPhoneSimulator"){ dependsOn("build") }
tasks.named("launchIPadSimulator"){ dependsOn("build") }
tasks.named("launchIOSDevice"){ dependsOn("build") }
tasks.named("createIPA"){
    dependsOn("build")
    dependsOn(":tools:pack")
    dependsOn(":core:preGen")
}

configure<org.robovm.gradle.RoboVMPluginExtension>{
    archs = "arm64"

    if(project.hasProperty("signIdentity")) println("iOS Sign Identity: " + project.property("signIdentity"))
    if(project.hasProperty("provisioningProfile")) println("iOS Provisioning Profile: " + project.property("provisioningProfile"))

    iosSignIdentity = project.findProperty("signIdentity") as String?
    iosProvisioningProfile = project.findProperty("provisioningProfile") as String?
    setIosSkipSigning(false)
}
