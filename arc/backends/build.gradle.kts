import org.gradle.api.tasks.*

subprojects{
    apply(plugin = "java-library")

    extensions.configure<SourceSetContainer>{
        named("main"){
            java.setSrcDirs(listOf("src"))
        }
    }

    dependencies{
        add("compileOnly", project(":arc-core"))
    }
}
