import org.gradle.api.tasks.*

subprojects{
    extensions.configure<SourceSetContainer>{
        named("main"){
            resources.setSrcDirs(listOf("libs"))
            java.setSrcDirs(emptyList<String>())
        }
    }
}
