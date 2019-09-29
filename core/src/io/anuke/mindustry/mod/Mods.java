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

    /** @return the loaded mod found by class, or null if not found. */
    public @Nullable LoadedMod getMod(Class<? extends Mod> type){
        return loaded.find(l -> l.mod.getClass() == type);
    }

    /** Loads all mods from the folder, but does call any methods on them.*/
    public void load(){
        for(FileHandle file : modDirectory.list()){
            if(!file.extension().equals("jar") || !file.extension().equals("zip")) continue;

            try{
                loaded.add(loadmod(file));
            }catch(IllegalArgumentException ignored){
            }catch(Exception e){
                Log.err("Failed to load plugin file {0}. Skipping.", file);
                e.printStackTrace();
            }
        }

        filet.buildFiles(loaded);
    }

    /** @return all loaded mods. */
    public Array<LoadedMod> all(){
        return loaded;
    }

    /** Iterates through each mod with a main class.*/
    public void each(Consumer<Mod> cons){
        loaded.each(p -> p.mod != null, p -> cons.accept(p.mod));
    }

    private LoadedMod loadmod(FileHandle jar) throws Exception{
        FileHandle zip = new ZipFileHandle(jar);

        FileHandle metaf = zip.child("mod.json").exists() ? zip.child("mod.json") : zip.child("plugin.json");
        if(!metaf.exists()){
            Log.warn("Mod {0} doesn't have a 'mod.json'/'plugin.json' file, skipping.", jar);
            throw new IllegalArgumentException();
        }

        ModMeta meta = JsonIO.read(ModMeta.class, metaf.readString());
        String camelized = meta.name.replace(" ", "");
        String mainClass = meta.main == null ? camelized.toLowerCase() + "." + camelized + "Mod" : meta.main;
        Mod mainMod;

        //make sure the main class exists before loading it; if it doesn't just don't put it there
        if(zip.child(mainClass.replace('.', '/') + ".class").exists()){
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.file().toURI().toURL()}, ClassLoader.getSystemClassLoader());
            Class<?> main = classLoader.loadClass(mainClass);
            metas.put(main, meta);
            mainMod = (Mod)main.getDeclaredConstructor().newInstance();
        }else{
            mainMod = null;
        }

        return new LoadedMod(jar, zip, mainMod, meta);
    }

    /** Represents a plugin that has been loaded from a jar file.*/
    public static class LoadedMod{
        /** The location of this mod's zip file on the disk. */
        public final FileHandle file;
        /** The root zip file; points to the contents of this mod. */
        public final FileHandle root;
        /** The mod's main class; may be null. */
        public final @Nullable Mod mod;
        /** This mod's metadata. */
        public final ModMeta meta;

        public LoadedMod(FileHandle file, FileHandle root, Mod mod, ModMeta meta){
            this.root = root;
            this.file = file;
            this.mod = mod;
            this.meta = meta;
        }
    }

    /** Plugin metadata information.*/
    public static class ModMeta{
        public String name, author, description, version, main;
        public String[] dependencies = {}; //TODO implement
    }
}
