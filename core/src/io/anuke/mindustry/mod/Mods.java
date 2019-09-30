package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Pixmap.*;
import io.anuke.arc.graphics.Texture.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.io.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;

import java.io.*;
import java.net.*;

import static io.anuke.mindustry.Vars.*;

public class Mods implements Loadable{
    private Json json = new Json();
    private ContentParser parser = new ContentParser();
    private ObjectMap<String, Array<FileHandle>> bundles = new ObjectMap<>();
    private ObjectSet<String> specialFolders = ObjectSet.with("bundles", "sprites");

    private int totalSprites;
    private PixmapPacker packer;

    private Array<LoadedMod> loaded = new Array<>();
    private ObjectMap<Class<?>, ModMeta> metas = new ObjectMap<>();
    private boolean requiresRestart;

    /** Returns a file named 'config.json' in a special folder for the specified plugin.
     * Call this in init(). */
    public FileHandle getConfig(Mod mod){
        ModMeta load = metas.get(mod.getClass());
        if(load == null) throw new IllegalArgumentException("Mod is not loaded yet (or missing)!");
        return modDirectory.child(load.name).child("config.json");
    }

    /** @return the loaded mod found by class, or null if not found. */
    public @Nullable
    LoadedMod getMod(Class<? extends Mod> type){
        return loaded.find(l -> l.mod.getClass() == type);
    }

    /** Imports an external mod file.*/
    public void importMod(FileHandle file) throws IOException{
        FileHandle dest = modDirectory.child(file.name());
        if(dest.exists()){
            throw new IOException("A mod with the same filename already exists!");
        }

        file.copyTo(dest);
        try{
            loaded.add(loadMod(file));
            requiresRestart = true;
        }catch(IOException e){
            dest.delete();
            throw e;
        }catch(Throwable t){
            dest.delete();
            throw new IOException(t);
        }
    }

    /** Repacks all in-game sprites. */
    @Override
    public void loadAsync(){
        if(loaded.isEmpty()) return;

        packer = new PixmapPacker(2048, 2048, Format.RGBA8888, 2, true);
        for(LoadedMod mod : loaded){
            try{
                int packed = 0;
                for(FileHandle file : mod.root.child("sprites").list()){
                    if(file.extension().equals("png")){
                        try(InputStream stream = file.read()){
                            byte[] bytes = Streams.copyStreamToByteArray(stream, Math.max((int)file.length(), 512));
                            Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                            packer.pack(mod.name + "-" + file.nameWithoutExtension(), pixmap);
                            pixmap.dispose();
                            packed ++;
                            totalSprites ++;
                        }
                    }
                }
                Log.info("Packed {0} images for mod '{1}'.", packed, mod.meta.name);
            }catch(IOException e){
                Log.err("Error packing images for mod: {0}", mod.meta.name);
                e.printStackTrace();
                if(!headless) ui.showException(e);
            }
        }
    }

    @Override
    public void loadSync(){
        if(packer == null) return;

        //get textures packed
        if(totalSprites > 0){
            TextureFilter filter = Core.settings.getBool("linear") ? TextureFilter.Linear : TextureFilter.Nearest;
            packer.getPages().each(page -> page.updateTexture(filter, filter, false));
            packer.getPages().each(page -> page.getRects().each((name, rect) -> Core.atlas.addRegion(name, page.getTexture(), (int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height)));
        }

        packer.dispose();
    }

    /** Removes a mod file and marks it for requiring a restart. */
    public void removeMod(LoadedMod mod){
        if(mod.file.isDirectory()){
            mod.file.deleteDirectory();
        }else{
            mod.file.delete();
        }
        loaded.remove(mod);
        requiresRestart = true;
    }

    public boolean requiresRestart(){
        return requiresRestart;
    }

    /** Loads all mods from the folder, but does call any methods on them.*/
    public void load(){
        for(FileHandle file : modDirectory.list()){
            if(!file.extension().equals("jar") && !file.extension().equals("zip") && !(file.isDirectory() && file.child("mod.json").exists())) continue;

            try{
                loaded.add(loadMod(file));
            }catch(IllegalArgumentException ignored){
            }catch(Exception e){
                Log.err("Failed to load plugin file {0}. Skipping.", file);
                e.printStackTrace();
            }
        }

        //sort mods to make sure servers handle them properly.
        loaded.sort(Structs.comparing(m -> m.name));

        buildFiles();
    }

    private void buildFiles(){
        for(LoadedMod mod : loaded){
            for(FileHandle file : mod.root.list()){
                //ignore special folders like bundles or sprites
                if(file.isDirectory() && !specialFolders.contains(file.name())){
                    //TODO calling child/parent on these files will give you gibberish; create wrapper class.
                    file.walk(f -> filet.addFile(f));
                }
            }

            //load up bundles.
            FileHandle folder = mod.root.child("bundles");
            if(folder.exists()){
                for(FileHandle file : folder.list()){
                    if(file.name().startsWith("bundle") && file.extension().equals("properties")){
                        String name = file.nameWithoutExtension();
                        bundles.getOr(name, Array::new).add(file);
                    }
                }
            }
        }

        //add new keys to each bundle
        I18NBundle bundle = Core.bundle;
        while(bundle != null){
            String str = bundle.getLocale().toString();
            String locale = "bundle" + (str.isEmpty() ? "" : "_" + str);
            for(FileHandle file : bundles.getOr(locale, Array::new)){
                try{
                    PropertiesUtils.load(bundle.getProperties(), file.reader());
                }catch(Exception e){
                    throw new RuntimeException("Error loading bundle: " + file + "/" + locale, e);
                }
            }
            bundle = bundle.getParent();
        }
    }

    /** Creates all the content found in mod files. */
    public void loadContent(){
        for(LoadedMod mod : loaded){
            if(mod.root.child("content").exists()){
                FileHandle contentRoot = mod.root.child("content");
                for(ContentType type : ContentType.all){
                    FileHandle folder = contentRoot.child(type.name().toLowerCase() + "s");
                    if(folder.exists()){
                        for(FileHandle file : folder.list()){
                            if(file.extension().equals("json")){
                                try{
                                    Content loaded = parser.parse(mod.name, file.nameWithoutExtension(), file.readString(), type);
                                    Log.info("[{0}] Loaded '{1}'.", mod.meta.name, loaded);
                                }catch(Exception e){
                                    throw new RuntimeException("Failed to parse content file '" + file + "' for mod '" + mod.meta.name + "'.", e);
                                }
                            }
                        }
                    }
                }
            }
        }

        each(Mod::loadContent);
    }

    /** @return all loaded mods. */
    public Array<LoadedMod> all(){
        return loaded;
    }

    /** @return a list of mods and versions, in the format name:version. */
    public Array<String> getModStrings(){
        return loaded.select(l -> !l.meta.hidden).map(l -> l.name + ":" + l.meta.version);
    }

    /** Iterates through each mod with a main class.*/
    public void each(Consumer<Mod> cons){
        loaded.each(p -> p.mod != null, p -> cons.accept(p.mod));
    }

    /** Loads a mod file+meta, but does not add it to the list.
     * Note that directories can be loaded as mods.*/
    private LoadedMod loadMod(FileHandle sourceFile) throws Exception{
        FileHandle zip = sourceFile.isDirectory() ? sourceFile : new ZipFileHandle(sourceFile);

        FileHandle metaf = zip.child("mod.json").exists() ? zip.child("mod.json") : zip.child("plugin.json");
        if(!metaf.exists()){
            Log.warn("Mod {0} doesn't have a 'mod.json'/'plugin.json' file, skipping.", sourceFile);
            throw new IllegalArgumentException("No mod.json found.");
        }

        ModMeta meta = json.fromJson(ModMeta.class, metaf.readString());
        String camelized = meta.name.replace(" ", "");
        String mainClass = meta.main == null ? camelized.toLowerCase() + "." + camelized + "Mod" : meta.main;
        Mod mainMod;

        //make sure the main class exists before loading it; if it doesn't just don't put it there
        if(zip.child(mainClass.replace('.', '/') + ".class").exists()){
            //other platforms don't have standard java class loaders
            if(mobile){
                throw new IllegalArgumentException("This mod is not compatible with " + (ios ? "iOS" : "Android") + ".");
            }

            URLClassLoader classLoader = new URLClassLoader(new URL[]{sourceFile.file().toURI().toURL()}, ClassLoader.getSystemClassLoader());
            Class<?> main = classLoader.loadClass(mainClass);
            metas.put(main, meta);
            mainMod = (Mod)main.getDeclaredConstructor().newInstance();
        }else{
            mainMod = null;
        }

        return new LoadedMod(sourceFile, zip, mainMod, meta);
    }

    /** Represents a plugin that has been loaded from a jar file.*/
    public static class LoadedMod{
        /** The location of this mod's zip file/folder on the disk. */
        public final FileHandle file;
        /** The root zip file; points to the contents of this mod. In the case of folders, this is the same as the mod's file. */
        public final FileHandle root;
        /** The mod's main class; may be null. */
        public final @Nullable Mod mod;
        /** Internal mod name. Used for textures. */
        public final String name;
        /** This mod's metadata. */
        public final ModMeta meta;

        //TODO implement
        protected boolean enabled;

        public LoadedMod(FileHandle file, FileHandle root, Mod mod, ModMeta meta){
            this.root = root;
            this.file = file;
            this.mod = mod;
            this.meta = meta;
            this.name = meta.name.toLowerCase().replace(" ", "-");
        }
    }

    /** Plugin metadata information.*/
    public static class ModMeta{
        public String name, author, description, version, main;
        public String[] dependencies = {}; //TODO implement
        /** Hidden mods are only server-side or client-side, and do not support adding new content. */
        public boolean hidden;
    }
}
