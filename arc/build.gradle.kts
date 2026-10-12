buildscript{
    repositories{
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }

    dependencies{
        classpath("com.badlogicgames.jnigen:jnigen-gradle:3.1.2")
    }
}

plugins{
    id("de.undercouch.download") version "5.0.1"
}

allprojects{
    apply(plugin = "maven-publish")
    group = "com.github.Anuken"
    version = "1.0"

    repositories{
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots")
        google()
        maven("https://jitpack.io")
    }

    tasks.withType<JavaCompile>().configureEach{
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.compilerArgs.addAll(listOf("--release", "8"))
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc>().configureEach{
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply{
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("-release", "17")
        }
    }
}

subprojects{
    apply(plugin = "java-library")

    tasks.named<JavaCompile>("compileJava"){
        options.isFork = true
        options.isIncremental = true
    }

    configure<JavaPluginExtension>{
        withJavadocJar()
        withSourcesJar()
    }

    configure<PublishingExtension>{
        publications{
            create<MavenPublication>("maven"){
                from(components["java"])
            }
        }
    }
}

if(System.getProperty("compileMethodParams") != null){
    allprojects{
        tasks.withType<JavaCompile>().configureEach{
            options.compilerArgs.add("-parameters")
        }
    }
}
