package io.anuke.mindustry.plugin;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.io.*;

import java.net.*;

import static io.anuke.mindustry.Vars.pluginDirectory;

public class Plugins{
    private Array<LoadedPlugin> loaded = new Array<>();
    private ObjectMap<Class<?>, PluginMeta> metas = new ObjectMap<>();

    /** Returns a file named 'config.json' in a special folder for the specified plugin.
     * Call this in init(). */
    public FileHandle getConfig(Plugin plugin){
        PluginMeta load = metas.get(plugin.getClass());
        if(load == null) throw new IllegalArgumentException("Plugin is not loaded yet (or missing)!");
        return pluginDirectory.child(load.name).child("config.json");
    }

    /** @return the loaded plugin found by class, or null if not found. */
    public @Nullable LoadedPlugin getPlugin(Class<? extends Plugin> type){
        return loaded.find(l -> l.plugin.getClass() == type);
    }

    /** Loads all plugins from the folder, but does call any methods on them.*/
    public void load(){
        for(FileHandle file : pluginDirectory.list()){
            if(!file.extension().equals("jar")) continue;

            try{
                loaded.add(loadPlugin(file));
            }catch(IllegalArgumentException ignored){
            }catch(Exception e){
                Log.err("Failed to load plugin file {0}. Skipping.", file);
                e.printStackTrace();
            }
        }
    }

    /** @return all loaded plugins. */
    public Array<LoadedPlugin> all(){
        return loaded;
    }

    /** Iterates through each plugin.*/
    public void each(Consumer<Plugin> cons){
        loaded.each(p -> cons.accept(p.plugin));
    }

    private LoadedPlugin loadPlugin(FileHandle jar) throws Exception{
        FileHandle zip = new ZipFileHandle(jar);

        FileHandle metaf = zip.child("plugin.json");
        if(!metaf.exists()){
            Log.warn("Plugin {0} doesn't have a 'plugin.json' file, skipping.", jar);
            throw new IllegalArgumentException();
        }

        PluginMeta meta = JsonIO.read(PluginMeta.class, metaf.readString());

        URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.file().toURI().toURL()}, ClassLoader.getSystemClassLoader());
        Class<?> main = classLoader.loadClass(meta.main);
        metas.put(main, meta);
        return new LoadedPlugin(jar, zip, (Plugin)main.getDeclaredConstructor().newInstance(), meta);
    }

    /** Represents a plugin that has been loaded from a jar file.*/
    public static class LoadedPlugin{
        public final FileHandle jarFile;
        public final FileHandle zipRoot;
        public final Plugin plugin;
        public final PluginMeta meta;

        public LoadedPlugin(FileHandle jarFile, FileHandle zipRoot, Plugin plugin, PluginMeta meta){
            this.zipRoot = zipRoot;
            this.jarFile = jarFile;
            this.plugin = plugin;
            this.meta = meta;
        }
    }

    /** Plugin metadata information.*/
    public static class PluginMeta{
        public String name, author, main, description;
        public String version;
    }
}
