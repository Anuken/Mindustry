plugins{
    `java-library`
}

sourceSets.main{
    java.setSrcDirs(listOf("src"))
}

dependencies{
    compileOnly(project(":arc-core"))
}
