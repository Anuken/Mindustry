plugins{
    `java-library`
}

sourceSets.main{
    java.setSrcDirs(listOf("src/main/java/"))
    resources.setSrcDirs(listOf("src/main/resources/"))
}

dependencies{
    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.github.Anuken:arc-core:frog")
}

tasks.withType<JavaCompile>().configureEach{
    options.isFork = true

    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
            "--add-exports", "java.base/sun.reflect.annotation=ALL-UNNAMED"
        )
    )
}
