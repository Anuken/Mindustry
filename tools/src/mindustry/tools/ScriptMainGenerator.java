package mindustry.tools;

import arc.*;
import arc.Graphics.Cursor.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import com.google.common.reflect.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.world.*;

import java.io.*;
import java.lang.reflect.*;

public class ScriptMainGenerator{

    public static void main(String[] args) throws Exception{
        String base = "mindustry";
        Seq<String> blacklist = Seq.with("tools", "arc.flabel.effects");
        Seq<String> nameBlacklist = Seq.with();
        Seq<Class<?>> whitelist = Seq.with(Draw.class, Fill.class, Lines.class, Core.class, TextureAtlas.class, TextureRegion.class, Time.class, System.class, PrintStream.class,
        AtlasRegion.class, String.class, Mathf.class, Angles.class, Color.class, Runnable.class, Object.class, Icon.class, Tex.class, Shader.class,
        Sounds.class, Musics.class, Call.class, Texture.class, TextureData.class, Pixmap.class, I18NBundle.class, Interval.class, DataInput.class, DataOutput.class,
        DataInputStream.class, DataOutputStream.class, Integer.class, Float.class, Double.class, Long.class, Boolean.class, Short.class, Byte.class, Character.class);
        Seq<String> nopackage = Seq.with("java.lang", "java");

        Seq<Class<?>> classes = Seq.withArrays(
            getClasses("mindustry"),
            getClasses("arc.func"),
            getClasses("arc.struct"),
            getClasses("arc.scene"),
            getClasses("arc.math"),
            getClasses("arc.audio"),
            getClasses("arc.input"),
            getClasses("arc.util"),
            getClasses("arc.files"),
            getClasses("arc.flabel"),
            getClasses("arc.struct")
        );
        classes.addAll(whitelist);
        classes.sort(Structs.comparing(Class::getName));

        classes.removeAll(type -> type.isSynthetic() || type.isAnonymousClass() || type.getCanonicalName() == null || Modifier.isPrivate(type.getModifiers())
        || blacklist.contains(s -> type.getName().startsWith(base + "." + s + ".")) || nameBlacklist.contains(type.getSimpleName()) || blacklist.contains(type.getPackage().getName()));
        classes.add(NetConnection.class, SaveIO.class, SystemCursor.class);

        classes.distinct();
        classes.sortComparing(Class::getName);
        ObjectSet<String> used = ObjectSet.with();

        StringBuilder result = new StringBuilder("//Generated class. Do not modify.\n");
        result.append("\n").append(new Fi("core/assets/scripts/base.js").readString()).append("\n");
        for(Class type : classes){
            if(used.contains(type.getPackage().getName()) || nopackage.contains(s -> type.getName().startsWith(s))) continue;
            result.append("importPackage(Packages.").append(type.getPackage().getName()).append(")\n");
            used.add(type.getPackage().getName());
        }

        Log.info("Imported @ packages.", used.size);

        for(Class type : EventType.class.getClasses()){
            result.append("const ").append(type.getSimpleName()).append(" = ").append("Packages.").append(type.getName().replace('$', '.')).append("\n");
        }

        new Fi("core/assets/scripts/global.js").writeString(result.toString());

        //map simple name to type
        Seq<String> packages = Seq.with(
        "mindustry.entities.effect",
        "mindustry.entities.bullet",
        "mindustry.entities.abilities",
        "mindustry.ai.types",
        "mindustry.type.weather",
        "mindustry.type.weapons",
        "mindustry.type.ammo",
        "mindustry.game.Objectives",
        "mindustry.world.blocks",
        "mindustry.world.consumers",
        "mindustry.world.draw",
        "mindustry.type",
        "mindustry.entities.pattern",
        "mindustry.entities.part"
        );

        String classTemplate = "package mindustry.mod;\n" +
        "\n" +
        "import arc.struct.*;\n" +
        "/** Generated class. Maps simple class names to concrete classes. For use in JSON mods. */\n" +
        "@SuppressWarnings(\"deprecation\")\n" +
        "public class ClassMap{\n" +
        "    public static final ObjectMap<String, Class<?>> classes = new ObjectMap<>();\n" +
        "    \n" +
        "    static{\n$CLASSES$" +
        "    }\n" +
        "}\n";

        StringBuilder cdef = new StringBuilder();

        Seq<Class<?>> mapped = classes.select(c -> Modifier.isPublic(c.getModifiers()) && packages.contains(c.getCanonicalName()::startsWith))
        .add(Block.class); //special case

        for(Class<?> c : mapped){
            cdef.append("        classes.put(\"").append(c.getSimpleName()).append("\", ").append(c.getCanonicalName()).append(".class);\n");
        }

        new Fi("core/src/mindustry/mod/ClassMap.java").writeString(classTemplate.replace("$CLASSES$", cdef.toString()));
        Log.info("Generated @ class mappings.", mapped.size);
    }

    public static Seq<Class> getClasses(String packageName) throws Exception{
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        var result = new Seq<Class>();

        for(ClassPath.ClassInfo info : ClassPath.from(loader).getAllClasses()){
            if(info.getName().startsWith(packageName + ".")){
                result.add(info.load());
            }
        }
        return result;
    }
}
