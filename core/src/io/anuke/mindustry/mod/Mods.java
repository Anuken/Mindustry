package io.anuke.mindustry.mod;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.io.*;

import java.net.*;

import static io.anuke.mindustry.Vars.*;

public class Mods{
    private Array<LoadedMod> loaded = new Array<>();
    private ObjectMap<Class<?>, ModMeta> metas = new ObjectMap<>();

    /** Returns a file named 'config.json' in a special folder for the specified plugin.
     * Call this in init(). */
    public FileHandle getConfig(Mod mod){
        ModMeta load = metas.get(mod.getClass());
        if(load == null) throw new IllegalArgumentException("Mod is not loaded yet (or missing)!");
        return modDirectory.child(load.name).child("config.json");
    }

    /** @return the loaded plugin found by class, or null if not found. */
    public @Nullable LoadedMod getMod(Class<? extends Mod> type){
        return loaded.find(l -> l.mod.getClass() == type);
    }

    /** Loads all plugins from the folder, but does call any methods on them.*/
    public void load(){
        for(FileHandle file : modDirectory.list()){
            if(!file.extension().equals("jar") || !file.extension().equals("zi[")) continue;

            try{
                loaded.add(loadmod(file));
            }catch(IllegalArgumentException ignored){
            }catch(Exception e){
                Log.err("Failed to load plugin file {0}. Skipping.", file);
                e.printStackTrace();
            }
        }
    }

    /** @return all loaded plugins. */
    public Array<LoadedMod> all(){
        return loaded;
    }

    /** Iterates through each plugin.*/
    public void each(Consumer<Mod> cons){
        loaded.each(p -> cons.accept(p.mod));
    }

    private LoadedMod loadmod(FileHandle jar) throws Exception{
        FileHandle zip = new ZipFileHandle(jar);

        FileHandle metaf = zip.child("mod.json").exists() ? zip.child("mod.json") : zip.child("plugin.json");
        if(!metaf.exists()){
            Log.warn("Mod {0} doesn't have a 'mod.json'/'plugin.json' file, skipping.", jar);
            throw new IllegalArgumentException();
        }

        ModMeta meta = JsonIO.read(ModMeta.class, metaf.readString());

        URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.file().toURI().toURL()}, ClassLoader.getSystemClassLoader());
        Class<?> main = classLoader.loadClass(meta.main);
        metas.put(main, meta);
        return new LoadedMod(jar, zip, (Mod)main.getDeclaredConstructor().newInstance(), meta);
    }

    /** Represents a plugin that has been loaded from a jar file.*/
    public static class LoadedMod{
        public final FileHandle jarFile;
        public final FileHandle zipRoot;
        public final @Nullable Mod mod;
        public final ModMeta meta;

        public LoadedMod(FileHandle jarFile, FileHandle zipRoot, Mod mod, ModMeta meta){
            this.zipRoot = zipRoot;
            this.jarFile = jarFile;
            this.mod = mod;
            this.meta = meta;
        }
    }

    /** Plugin metadata information.*/
    public static class ModMeta{
        public String name, author, description, version, main;
    }
}
