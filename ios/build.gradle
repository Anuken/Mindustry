buildscript{
    repositories{
        mavenCentral()
    }

    dependencies{
        classpath "com.mobidevelop.robovm:robovm-gradle-plugin:2.3.19"
    }
}

apply plugin: "java"
apply plugin: "robovm"

task incrementConfig{
    def vfile = file('robovm.properties')
    def bversion = getBuildVersion()
    def props = new Properties()
    if(vfile.exists()){
        props.load(new FileInputStream(vfile))
    }else{
        props['app.id'] = 'io.anuke.mindustry'
        props['app.version'] = '7.0'
        props['app.mainclass'] = 'mindustry.IOSLauncher'
        props['app.executable'] = 'IOSLauncher'
        props['app.name'] = 'Mindustry'
    }

    props['app.build'] = (!props.containsKey("app.build") ? 40 : props['app.build'].toInteger() + 1) + ""
    if(bversion != "custom build"){
        props['app.version'] = versionNumber + "." + bversion + (bversion.contains(".") ? "" : ".0")
    }
    props.store(vfile.newWriter(), null)
}

dependencies{
    implementation project(":core")

    implementation arcModule("natives:natives-ios")
    implementation arcModule("natives:natives-freetype-ios")
    implementation arcModule("backends:backend-robovm")

    compileOnly project(":annotations")
}

sourceSets.main.java.srcDirs = ["src/"]

ext{
    mainClassName = "mindustry.ios.IOSLauncher"
}

task copyAssets(){
    doLast{
        delete{
            delete "assets/"
        }
    
        copy{
            from "../core/assets"
            into "assets/"
        }
    }
}

task copyNatives(){
    doLast{
        copy{
            from "../../Arc/natives/natives-freetype-ios/libs", "../../Arc/natives/natives-ios/libs"
            into "libs"
        }

    }
}

task deploy{
    if(System.getProperty("os.name").contains("Mac")) dependsOn copyAssets
    dependsOn createIPA
}

//must pack before deployment, as iOS never has the latest sprites.
copyAssets.dependsOn ":tools:pack"
copyAssets.dependsOn copyNatives
launchIPhoneSimulator.dependsOn build
launchIPadSimulator.dependsOn build
launchIOSDevice.dependsOn build
createIPA.dependsOn build

robovm{
    archs = "thumbv7:arm64"

    if(project.hasProperty("signIdentity")) println "iOS Sign Identity: " + project.property("signIdentity")
    if(project.hasProperty("provisioningProfile")) println "iOS Provisioning Profile: " + project.property("provisioningProfile")

    iosSignIdentity = project.properties["signIdentity"]
    iosProvisioningProfile = project.properties["provisioningProfile"]
    iosSkipSigning = false
}
