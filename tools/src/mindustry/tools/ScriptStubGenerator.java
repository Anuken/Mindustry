package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

public class ScriptStubGenerator{

    public static void main(String[] args) throws Exception{
        String base = "mindustry";
        Seq<String> blacklist = Seq.with("plugin", "mod", "net", "io", "tools");
        Seq<String> nameBlacklist = Seq.with("ClassAccess");
        Seq<Class<?>> whitelist = Seq.with(Draw.class, Fill.class, Lines.class, Core.class, TextureAtlas.class, TextureRegion.class, Time.class, System.class, PrintStream.class,
        AtlasRegion.class, String.class, Mathf.class, Angles.class, Color.class, Runnable.class, Object.class, Icon.class, Tex.class,
        Sounds.class, Musics.class, Call.class, Texture.class, TextureData.class, Pixmap.class, I18NBundle.class, Interval.class, DataInput.class, DataOutput.class,
        DataInputStream.class, DataOutputStream.class, Integer.class, Float.class, Double.class, Long.class, Boolean.class, Short.class, Byte.class, Character.class);
        Seq<String> nopackage = Seq.with("java.lang", "java");

        Seq<Class<?>> classes = Seq.withArrays(
            getClasses("mindustry"),
            getClasses("arc.func"),
            getClasses("arc.struct"),
            getClasses("arc.scene"),
            getClasses("arc.math")
        );
        classes.addAll(whitelist);
        classes.sort(Structs.comparing(Class::getName));

        classes.removeAll(type -> type.isSynthetic() || type.isAnonymousClass() || type.getCanonicalName() == null || Modifier.isPrivate(type.getModifiers())
        || blacklist.contains(s -> type.getName().startsWith(base + "." + s + ".")) || nameBlacklist.contains(type.getSimpleName()));
        classes.distinct();
        ObjectSet<String> used = ObjectSet.with();

        StringBuilder result = new StringBuilder("//Generated class. Do not modify.\n");
        result.append("\n").append(new Fi("core/assets/scripts/base.js").readString()).append("\n");
        for(Class type : classes){
            if(used.contains(type.getPackage().getName()) || nopackage.contains(s -> type.getName().startsWith(s))) continue;
            result.append("importPackage(Packages.").append(type.getPackage().getName()).append(")\n");
            used.add(type.getPackage().getName());
        }

        for(Class type : EventType.class.getClasses()){
            result.append("const ").append(type.getSimpleName()).append(" = ").append("Packages.").append(type.getName().replace('$', '.')).append("\n");
        }

        //Log.info(result);

        new Fi("core/assets/scripts/global.js").writeString(result.toString());
    }


    private static Seq<Class> getClasses(String packageName) throws Exception{
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Seq<File> dirs = new Seq<>();

        for(URL resource : Collections.list(classLoader.getResources(packageName.replace('.', '/')))){
            dirs.add(new File(resource.getFile()));
        }

        Seq<Class> classes = new Seq<>();
        for(File directory : dirs){
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static Seq<Class> findClasses(File directory, String packageName) throws Exception{
        Seq<Class> classes = new Seq<>();
        if(!directory.exists()) return classes;

        File[] files = directory.listFiles();
        for(File file : files){
            if(file.isDirectory()){
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            }else if(file.getName().endsWith(".class")){
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6), false, Thread.currentThread().getContextClassLoader()));
            }
        }
        return classes;
    }
}
