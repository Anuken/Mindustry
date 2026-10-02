dependencies{
    val bundledAndroidJar = file("libs/android.jar")
    if(bundledAndroidJar.exists()){
        compileOnly(files(bundledAndroidJar))
    }else{
        val platforms = System.getenv("ANDROID_HOME")?.let{ File(it, "platforms") }
        val highestVersion = platforms?.listFiles().orEmpty()
            .mapNotNull{ Regex("^android-(\\d+)$").matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull()
        val androidJar = highestVersion?.let{ File(platforms, "android-$it/android.jar") }
        if(androidJar?.exists() == true) compileOnly(files(androidJar))
    }
}
