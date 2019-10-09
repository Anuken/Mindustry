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
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.plugin.*;
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
    private Array<LoadedMod> disabled = new Array<>();
    private ObjectMap<Class<?>, ModMeta> metas = new ObjectMap<>();
    private boolean requiresReload;

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
            loaded.add(loadMod(file, false));
            requiresReload = true;
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
        Time.mark();

        packer = new PixmapPacker(2048, 2048, Format.RGBA8888, 2, true);

        for(LoadedMod mod : loaded){
            int[] packed = {0};
            boolean[] failed = {false};
            mod.root.child("sprites").walk(file -> {
                if(failed[0]) return;
                if(file.extension().equals("png")){
                    try(InputStream stream = file.read()){
                        byte[] bytes = Streams.copyStreamToByteArray(stream, Math.max((int)file.length(), 512));
                        Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                        packer.pack(mod.name + "-" + file.nameWithoutExtension(), pixmap);
                        pixmap.dispose();
                        packed[0] ++;
                        totalSprites ++;
                    }catch(IOException e){
                        failed[0] = true;
                        Core.app.post(() -> {
                            Log.err("Error packing images for mod: {0}", mod.meta.name);
                            e.printStackTrace();
                            if(!headless) ui.showException(e);
                        });
                    }
                }
            });
            Log.info("Packed {0} images for mod '{1}'.", packed[0], mod.meta.name);
        }

        Log.info("Time to pack textures: {0}", Time.elapsed());
    }

    @Override
    public void loadSync(){
        if(packer == null) return;
        Time.mark();

        Texture editor = Core.atlas.find("clear-editor").getTexture();
        PixmapPacker editorPacker = new PixmapPacker(2048, 2048, Format.RGBA8888, 2, true);

        for(AtlasRegion region : Core.atlas.getRegions()){
            if(region.getTexture() == editor){
                editorPacker.pack(region.name, Core.atlas.getPixmap(region).crop());
            }
        }

        //get textures packed
        if(totalSprites > 0){
            TextureFilter filter = Core.settings.getBool("linear") ? TextureFilter.Linear : TextureFilter.Nearest;

            packer.updateTextureAtlas(Core.atlas, filter, filter, false);
            //generate new icons
            for(Array<Content> arr : content.getContentMap()){
                arr.each(c -> {
                    if(c instanceof UnlockableContent && c.mod != null){
                        UnlockableContent u = (UnlockableContent)c;
                        u.createIcons(packer, editorPacker);
                    }
                });
            }

            editorPacker.updateTextureAtlas(Core.atlas, filter, filter, false);
            packer.updateTextureAtlas(Core.atlas, filter, filter, false);
        }

        packer.dispose();
        packer = null;
        Log.info("Time to update textures: {0}", Time.elapsed());
    }

    /** Removes a mod file and marks it for requiring a restart. */
    public void removeMod(LoadedMod mod){
        if(mod.file.isDirectory()){
            mod.file.deleteDirectory();
        }else{
            mod.file.delete();
        }
        loaded.remove(mod);
        requiresReload = true;
    }

    public boolean requiresReload(){
        return requiresReload;
    }

    /** Loads all mods from the folder, but does call any methods on them.*/
    public void load(){
        for(FileHandle file : modDirectory.list()){
            if(!file.extension().equals("jar") && !file.extension().equals("zip") && !(file.isDirectory() && file.child("mod.json").exists())) continue;

            try{
                LoadedMod mod = loadMod(file, false);
                if(mod.enabled()){
                    loaded.add(mod);
                }else{
                    disabled.add(mod);
                }
            }catch(Exception e){
                Log.err("Failed to load mod file {0}. Skipping.", file);
                Log.err(e);
            }
        }

        //load workshop mods now
        for(FileHandle file : platform.getExternalMods()){
            try{
                LoadedMod mod = loadMod(file, true);
                if(mod.enabled()){
                    loaded.add(mod);
                }else{
                    disabled.add(mod);
                }
            }catch(Exception e){
                Log.err("Failed to load mod workshop file {0}. Skipping.", file);
                Log.err(e);
            }
        }

        //sort mods to make sure servers handle them properly.
        loaded.sort(Structs.comparing(m -> m.name));

        buildFiles();
    }

    private void buildFiles(){
        for(LoadedMod mod : loaded){
            boolean zipFolder = !mod.file.isDirectory() && mod.root.parent() != null;
            String parentName = zipFolder ? mod.root.name() : null;
            for(FileHandle file : mod.root.list()){
                //ignore special folders like bundles or sprites
                if(file.isDirectory() && !specialFolders.contains(file.name())){
                    //TODO calling child/parent on these files will give you gibberish; create wrapper class.
                    file.walk(f -> tree.addFile(mod.file.isDirectory() ? f.path().substring(1 + mod.file.path().length()) :
                        zipFolder ? f.path().substring(parentName.length() + 1) : f.path(), f));
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

    /** Reloads all mod content. How does this even work? I refuse to believe that it functions correctly.*/
    public void reloadContent(){
        //epic memory leak
        Core.atlas = new TextureAtlas(Core.files.internal("sprites/sprites.atlas"));
        loaded.clear();
        disabled.clear();
        load();
        buildFiles();
        Musics.dispose();
        Sounds.dispose();
        Musics.load();
        Sounds.load();
        Core.assets.finishLoading();
        content.clear();
        content.createContent();
        loadAsync();
        loadSync();
        content.init();
        content.load();
        content.loadColors();
        data.load();
        requiresReload = false;
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
                                    //this binds the content but does not load it entirely
                                    Content loaded = parser.parse(mod, file.nameWithoutExtension(), file.readString(), type);
                                    Log.info("[{0}] Loaded '{1}'.", mod.meta.name, (loaded instanceof UnlockableContent ? ((UnlockableContent)loaded).localizedName : loaded));
                                }catch(Exception e){
                                    throw new RuntimeException("Failed to parse content file '" + file + "' for mod '" + mod.meta.name + "'.", e);
                                }
                            }
                        }
                    }
                }
            }
        }

        //this finishes parsing content fields
        parser.finishParsing();

        each(Mod::loadContent);
    }

    /** @return all loaded mods. */
    public Array<LoadedMod> all(){
        return loaded;
    }

    /** @return all disabled mods. */
    public Array<LoadedMod> disabled(){
        return disabled;
    }

    /** @return a list of mod names only, without versions. */
    public Array<String> getModNames(){
        return loaded.select(l -> !l.meta.hidden).map(l -> l.name + ":" + l.meta.version);
    }

    /** @return a list of mods and versions, in the format name:version. */
    public Array<String> getModStrings(){
        return loaded.select(l -> !l.meta.hidden).map(l -> l.name + ":" + l.meta.version);
    }

    /** Makes a mod enabled or disabled. shifts it.*/
    public void setEnabled(LoadedMod mod, boolean enabled){
        if(mod.enabled() != enabled){
            Core.settings.putSave(mod.name + "-enabled", enabled);
            requiresReload = true;
            if(!enabled){
                loaded.remove(mod);
                disabled.add(mod);
            }else{
                loaded.add(mod);
                disabled.remove(mod);
            }
        }
    }

    /** @return the mods that the client is missing.
     * The inputted array is changed to contain the extra mods that the client has but the server doesn't.*/
    public Array<String> getIncompatibility(Array<String> out){
        Array<String> mods = getModStrings();
        Array<String> result = mods.copy();
        for(String mod : mods){
            if(out.remove(mod)){
                result.remove(mod);
            }
        }
        return result;
    }

    /** Iterates through each mod with a main class.*/
    public void each(Consumer<Mod> cons){
        loaded.each(p -> p.mod != null, p -> cons.accept(p.mod));
    }

    /** Loads a mod file+meta, but does not add it to the list.
     * Note that directories can be loaded as mods.*/
    private LoadedMod loadMod(FileHandle sourceFile, boolean workshop) throws Exception{
        FileHandle zip = sourceFile.isDirectory() ? sourceFile : new ZipFileHandle(sourceFile);
        if(zip.list().length == 1 && zip.list()[0].isDirectory()){
            zip = zip.list()[0];
        }

        FileHandle metaf = zip.child("mod.json").exists() ? zip.child("mod.json") : zip.child("plugin.json");
        if(!metaf.exists()){
            Log.warn("Mod {0} doesn't have a 'mod.json'/'plugin.json' file, skipping.", sourceFile);
            throw new IllegalArgumentException("No mod.json found.");
        }

        ModMeta meta = json.fromJson(ModMeta.class, metaf.readString());
        String camelized = meta.name.replace(" ", "");
        String mainClass = meta.main == null ? camelized.toLowerCase() + "." + camelized + "Mod" : meta.main;
        String baseName = meta.name.toLowerCase().replace(" ", "-");

        if(loaded.contains(m -> m.name.equals(baseName)) || disabled.contains(m -> m.name.equals(baseName))){
            throw new IllegalArgumentException("A mod with the name '" + baseName + "' is already imported.");
        }

        Mod mainMod;

        FileHandle mainFile = zip;
        String[] path = (mainClass.replace('.', '/') + ".class").split("/");
        for(String str : path){
            if(!str.isEmpty()){
                mainFile = mainFile.child(str);
            }
        }

        //make sure the main class exists before loading it; if it doesn't just don't put it there
        if(mainFile.exists()){
            //other platforms don't have standard java class loaders
            if(!headless && Version.build != -1){
                throw new IllegalArgumentException("Java class mods are currently unsupported outside of custom builds.");
            }

            URLClassLoader classLoader = new URLClassLoader(new URL[]{sourceFile.file().toURI().toURL()}, ClassLoader.getSystemClassLoader());
            Class<?> main = classLoader.loadClass(mainClass);
            metas.put(main, meta);
            mainMod = (Mod)main.getDeclaredConstructor().newInstance();
        }else{
            mainMod = null;
        }

        //all plugins are hidden implicitly
        if(mainMod instanceof Plugin){
            meta.hidden = true;
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
        /** The ID of this mod in the workshop.*/
        public @Nullable String workshopID;

        public LoadedMod(FileHandle file, FileHandle root, Mod mod, ModMeta meta){
            this.root = root;
            this.file = file;
            this.mod = mod;
            this.meta = meta;
            this.name = meta.name.toLowerCase().replace(" ", "-");
        }

        public boolean enabled(){
            return Core.settings.getBool(name + "-enabled", true);
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
