
import arc.files.*
import arc.graphics.*
import arc.packer.*
import arc.struct.*
import arc.util.*
import arc.util.io.*
import arc.util.serialization.Jval.*
import java.util.concurrent.*

plugins{
    java
}

val versionNumber = providers.gradleProperty("versionNumber").get()

interface Injected{
    @get:Inject val execOps: ExecOperations
}
val injected = objects.newInstance<Injected>()

val genFolder = "../core/assets-raw/sprites_out/generated/"
val enableAA = true

sourceSets.main{
    java.setSrcDirs(listOf("src/"))
}

dependencies{
    implementation(project(":core"))

    implementation("com.github.Anuken:natives-desktop:frog")
    implementation("com.github.Anuken:backend-headless:frog")

    implementation("com.google.guava:guava:33.7.2-jre")
    implementation("com.github.javaparser:javaparser-core:3.28.2")
}

fun antialias(file: File){
    val result = Pixmap(Fi(file))
    Pixmaps.antialias(result)
    Fi(file).writePng(result)
    result.dispose()
}

tasks.register("pack"){
    dependsOn(tasks.classes, configurations.runtimeClasspath)

    doLast{
        //cleanup old sprites
        delete("../core/assets-raw/sprites_out/")
        delete("../core/assets/sprites/fallback")

        //copy in new sprites
        copy{
            from("../core/assets-raw/sprites/")
            into("../core/assets-raw/sprites_out/")
        }

        File(rootDir, "core/build/last_pack_version").writeText(versionNumber)

        //run generation task; generate all needed sprites
        file(genFolder).mkdirs()

        var ms = System.currentTimeMillis()
        injected.execOps.javaexec{
            mainClass.set("mindustry.tools.ImagePacker")
            classpath = sourceSets.main.get().runtimeClasspath
            workingDir = file(genFolder)
        }
        println("Image generation: ${(System.currentTimeMillis() - ms) / 1000f} seconds")

        copy{
            from("../core/assets-raw/sprites_out/ui/icons")
            into("../core/assets-raw/sprites_out/ui/")
        }

        delete("../core/assets-raw/sprites_out/ui/icons")

        if(enableAA){
            val executor = Executors.newFixedThreadPool(OS.cores)
            ms = System.currentTimeMillis()

            //antialias everything except UI elements
            fileTree(File(rootDir, "core/assets-raw/sprites_out/").absolutePath){ include("**/*.png") }.visit{
                val path = file.path
                if(isDirectory || path.contains(".9.png") || path.contains("aaaa")) return@visit

                val target = file
                executor.execute{ antialias(target) }
            }

            Threads.await(executor)

            println("Antialias: ${(System.currentTimeMillis() - ms) / 1000f} seconds")
        }

        ms = System.currentTimeMillis()

        //pack normal sprites
        TexturePacker.process(
            File(rootDir, "core/assets-raw/sprites_out/").absolutePath,
            File(rootDir, "core/assets/sprites/").absolutePath,
            "sprites.aatls"
        )

        println("Sprite packing: ${(System.currentTimeMillis() - ms) / 1000f} seconds")
    }
}

//Takes SVGs from core/assets-raw/icons and generates a font from them, placing it in core/assets/fonts/icon.ttf
//This uses fantasticicon, launched via npx - I don't like depending on it, but there are no pure Java solutions that I am aware of
//Codepoints are kept the same via core/assets-raw/icons/codepoints.json
//TODO: No icons exist yet; I have not drawn them. This task works in principle, but it needs all the SVGs to exist, otherwise it explodes or removes existing icons from the font.
tasks.register("generateIcons"){
    doLast{
        val iconDir = rootProject.file("core/assets-raw/icons")
        val mapFile = File(iconDir, "codepoints.json")
        val outDir = rootProject.file("core/assets/fonts")
        val svgs = iconDir.listFiles{ it.name.endsWith(".svg") }.orEmpty().sortedBy{ it.name }

        //existing [{name, code}] entries -> name to codepoint
        val codes = LinkedHashMap<String, Int>()
        if(mapFile.exists()){
            for(entry in read(mapFile.readText()).asArray()){
                codes[entry.getString("name")] = entry.getInt("code")
            }
        }

        //new icons get the next free codepoint
        var next = maxOf(0xE800, (codes.values.maxOrNull() ?: 0) + 1)
        val used = svgs.map{ it.name.removeSuffix(".svg") }
        for(name in used){
            if(!codes.containsKey(name)) codes[name] = next++
        }

        //only keep icons that exist, sorted by codepoint
        val usedSet = used.toSet()
        val sorted = codes.entries
            .filter{ it.key in usedSet }
            .sortedBy{ it.value }
            .associate{ it.key to it.value }

        mapFile.writeText(
            "[\n" + sorted.entries.joinToString(",\n"){ (k, v) ->
                "    " + newObject().put("name", k).put("code", v).toString(Jformat.plain)
            } + "\n]\n"
        )

        val codepoints = newObject()
        sorted.forEach{ (k, v) -> codepoints.put(k, v) }

        val config = File(temporaryDir, "fantasticon.json")
        config.writeText(
            newObject()
                .put("inputDir", iconDir.absolutePath)
                .put("outputDir", outDir.absolutePath)
                .put("name", "icon")
                .put("fontTypes", newArray().add("ttf"))
                .put("assetTypes", newArray())
                .put("normalize", true)
                .put("codepoints", codepoints)
                .toString(Jformat.plain)
        )

        outDir.mkdirs()
        val npx = if(System.getProperty("os.name").lowercase().contains("win")) "npx.cmd" else "npx"
        val proc = ProcessBuilder(npx, "--yes", "fantasticon", "--config", config.absolutePath)
            .redirectErrorStream(true)
            .start()
        proc.outputStream.close()
        proc.inputStream.bufferedReader().forEachLine{ println(it) }
        if(proc.waitFor() != 0) throw GradleException("fantasticon failed!")
    }
}

tasks.register<JavaExec>("updateScripts"){
    dependsOn(tasks.classes)
    mainClass = "mindustry.tools.ScriptMainGenerator"
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = file("../core/assets")
}

tasks.register<JavaExec>("fixMaps"){
    dependsOn(tasks.classes)
    mainClass = "mindustry.tools.MapFixer"
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = file("../core/assets")
}

fun uniEscape(string: String) = buildString{
    for(ch in string){
        when{
            ch == '\\' -> append("\\\\")
            ch in '\uE000'..'\uF8FF' -> append("\\u").append("%04x".format(ch.code))
            else -> append(ch)
        }
    }
}

tasks.register("updateBundles"){
    doLast{
        val bundlesDir = File(rootDir, "core/assets/bundles")
        val bundleFile = Fi.get(File(bundlesDir, "bundle.properties").path)

        val base = OrderedMap<String, String>()
        PropertiesUtils.load(base, bundleFile.reader())

        //map of line number to comment (or just empty string for blank line) content
        val commentAndBlankLines = IntMap<String>()
        val lines = bundleFile.reader().readLines()
        var offset = 0
        for(i in lines.indices){
            val line = lines[i]
            if(i > 0 && lines[i - 1].endsWith("\\")) offset++ //multiline value escaped with \\ at end of line
            else if(line.isEmpty() || line.startsWith("#")) commentAndBlankLines.put(i - offset, line)
        }

        Log.info("Updating bundles...")

        Fi.get(bundlesDir.path).walk{ child ->
            if(child.name() == "bundle.properties" || child.name() == "global.properties" || child.parent().name() == "output") return@walk
            if(project.hasProperty("bundle") && child.name() != project.property("bundle")) return@walk

            Log.info("| @", child.nameWithoutExtension())

            val other = OrderedMap<String, String>()

            val removals = Seq<String>()

            PropertiesUtils.load(other, child.reader())

            for(key in other.orderedKeys()){
                if(!base.containsKey(key)){
                    removals.add(key)
                    Log.info("&lr- Removing unused key '@'...", key)
                }
            }
            if(removals.size > 0) Log.info("&lr@ keys removed.", removals.size)
            for(s in removals){
                other.remove(s)
            }

            var added = 0

            for(key in base.orderedKeys()){
                if(other.get(key) == null || other.get(key).trim().isEmpty()){
                    other.put(key, base.get(key))
                    added++
                    Log.info("&lc- Adding missing key '@'...", key)
                }
            }

            fun processor(key: String, value: String): String =
                (key + " =" + (if(value.trim().isEmpty()) "" else " ") + uniEscape(value)).replace("\n", "\\n") + "\n"

            val output = child.sibling("output/" + child.name())

            if(added > 0) Log.info("&lc@ keys added.", added)
            if(removals.size + added > 0) Log.info("Writing bundle to @", output)
            val result = StringBuilder()

            var i = 0
            //add everything ordered
            for(key in base.orderedKeys()){
                //append any comments or blank lines as needed to match the english bundle
                while(commentAndBlankLines.containsKey(i++)) result.append(commentAndBlankLines.get(i - 1) + "\n")
                if(other.get(key) == null) continue

                result.append(processor(key, other.get(key)))
                other.remove(key)
            }

            child.writeString(result.toString())
        }
    }
}
