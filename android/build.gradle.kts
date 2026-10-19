import java.util.*

plugins{
    id("com.android.application")
}

evaluationDependsOn(":core")

val versionNumber = providers.gradleProperty("versionNumber").get()
val versionType = providers.gradleProperty("versionType").get()
val buildVersion = rootProject.extra["buildVersion"] as String
val hasSprites = rootProject.extra["hasSprites"] as Boolean

@Suppress("UNCHECKED_CAST")
val deployName = rootProject.extra["deployName"] as (String) -> String

val versionPropsFile = rootProject.file("core/assets/version.properties")

fun loadVersionProps(): Properties =
    Properties().also{ props -> versionPropsFile.bufferedReader().use{ props.load(it) } }

fun findSdkDir(): String?{
    System.getenv("ANDROID_HOME")?.let{ return it }
    val props = Properties().also{ p -> File(rootDir, "local.properties").bufferedReader().use{ p.load(it) } }
    return props.getProperty("sdk.dir")
}

repositories{
    google()
}

fun MutableCollection<String>.only(vararg paths: String){
    clear()
    addAll(paths)
}

tasks.register<Copy>("deploy"){
    dependsOn("assembleRelease")

    from("build/outputs/apk/release/android-release.apk")
    into("../deploy/")
    rename("android-release.apk", "${deployName("android")}.apk")
}

android{
    namespace = "io.anuke.mindustry"
    compileSdk = 36

    sourceSets{
        getByName("main"){
            manifest.srcFile("AndroidManifest.xml")
            java.directories.only("src")
            aidl.directories.only("src")
            renderscript.directories.only("src")
            res.directories.only("res")
            assets.directories.only("assets", "src/main/assets", "../core/assets/")
            jniLibs.directories.only("../arc/natives/natives-android/libs")
        }

        getByName("androidTest").setRoot("tests")
    }

    packaging{
        resources{
            excludes += "META-INF/robovm/ios/robovm.xml"
        }
    }

    defaultConfig{
        val props = loadVersionProps()
        val vcode = props.getProperty("androidBuildCode")?.toInt() ?: 1
        val versionNameResult = "$versionNumber-$versionType-${buildVersion.replace(" ", "-")}"

        applicationId = "io.anuke.mindustry"
        minSdk = 21
        targetSdk = 36

        versionName = versionNameResult
        versionCode = vcode

        if(project.hasProperty("release")){
            props["androidBuildCode"] = (vcode + 1).toString()
        }
        versionPropsFile.bufferedWriter().use{ props.store(it, null) }

        multiDexEnabled = true
    }

    compileOptions{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs{
        create("release"){
            if(project.hasProperty("RELEASE_STORE_FILE")){
                storeFile = file(project.property("RELEASE_STORE_FILE") as String)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
            }else if(System.getenv("CI") == "true"){
                storeFile = file("../../bekeystore.jks")
                storePassword = System.getenv("keystore_password")
                keyAlias = System.getenv("keystore_alias")
                keyPassword = System.getenv("keystore_alias_password")
            }else{
                println("No keystore property found. Releases will be unsigned.")
            }
        }
    }

    buildTypes{
        all{
            //TODO without these lines (r8 enabled), Mindustry crashes with missing default interface method errors.
            //WHY THE HELL ARE DEFAULT INTERFACES NOT BEING DESUGARED? WHY DID UPDATING AGP MAKE THIS HAPPEN?
            //When I ENABLE shrinking, r8 goes and REMOVES ALL DEFAULT INTERFACE CLASSES, which breaks mods. Why?
            //-keep class mindustry.** { *; } should *keep the classes* - WHY IS R8 REMOVING THEM?
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }

        if(project.hasProperty("RELEASE_STORE_FILE") || System.getenv("CI") == "true"){
            getByName("release"){
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies{
    implementation(project(":core"))

    implementation("com.github.Anuken:backend-android:frog")
    implementation("com.jakewharton.android.repackaged:dalvik-dx:9.0.0_r3")
}

tasks.register<Exec>("run"){
    commandLine("${findSdkDir()}/platform-tools/adb", "shell", "am", "start", "-n", "io.anuke.mindustry/mindustry.android.AndroidLauncher")
}

if(!hasSprites){
    tasks.configureEach{
        if(name == "assembleDebug" || name == "assembleRelease"){
            dependsOn(":tools:pack")
        }
    }
}
