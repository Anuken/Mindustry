package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Texture.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.arc.util.serialization.Jval.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.graphics.MultiPacker.*;
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
    private MultiPacker packer;

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
    public @Nullable LoadedMod getMod(Class<? extends Mod> type){
        return loaded.find(l -> l.mod != null && l.mod.getClass() == type);
    }

    /** Imports an external mod file.*/
    public void importMod(FileHandle file) throws IOException{
        FileHandle dest = modDirectory.child(file.name());
        if(dest.exists()){
            throw new IOException("A mod with the same filename already exists!");
        }

        file.copyTo(dest);
        try{
            loaded.add(loadMod(dest));
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

        packer = new MultiPacker();

        for(LoadedMod mod : loaded){
            Array<FileHandle> sprites = mod.root.child("sprites").findAll(f -> f.extension().equals("png"));
            Array<FileHandle> overrides = mod.root.child("sprites-override").findAll(f -> f.extension().equals("png"));
            packSprites(sprites, mod, true);
            packSprites(overrides, mod, false);
            Log.info("Packed {0} images for mod '{1}'.", sprites.size + overrides.size, mod.meta.name);
            totalSprites += sprites.size + overrides.size;
        }

        for(AtlasRegion region : Core.atlas.getRegions()){
            PageType type = getPage(region);
            if(!packer.has(type, region.name)){
                packer.add(type, region.name, Core.atlas.getPixmap(region));
            }
        }

        Log.info("Time to pack textures: {0}", Time.elapsed());
    }

    private void packSprites(Array<FileHandle> sprites, LoadedMod mod, boolean prefix){
        for(FileHandle file : sprites){
            try(InputStream stream = file.read()){
                byte[] bytes = Streams.copyStreamToByteArray(stream, Math.max((int)file.length(), 512));
                Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                packer.add(getPage(file), (prefix ? mod.name + "-" : "") + file.nameWithoutExtension(), new PixmapRegion(pixmap));
                pixmap.dispose();
            }catch(IOException e){
                Core.app.post(() -> {
                    Log.err("Error packing images for mod: {0}", mod.meta.name);
                    e.printStackTrace();
                    if(!headless) ui.showException(e);
                });
                break;
            }
        }
        totalSprites += sprites.size;
    }

    @Override
    public void loadSync(){
        if(packer == null) return;
        Time.mark();

        //get textures packed
        if(totalSprites > 0){
            TextureFilter filter = Core.settings.getBool("linear") ? TextureFilter.Linear : TextureFilter.Nearest;

            //flush so generators can use these sprites
            packer.flush(filter, Core.atlas);

            //generate new icons
            for(Array<Content> arr : content.getContentMap()){
                arr.each(c -> {
                    if(c instanceof UnlockableContent && c.mod != null){
                        UnlockableContent u = (UnlockableContent)c;
                        u.createIcons(packer);
                    }
                });
            }

            Core.atlas = packer.flush(filter, new TextureAtlas());
            Core.atlas.setErrorRegion("error");
            Log.info("Total pages: {0}", Core.atlas.getTextures().size);
        }

        packer.dispose();
        packer = null;
        Log.info("Time to update textures: {0}", Time.elapsed());
    }

    private PageType getPage(AtlasRegion region){
        return
            region.getTexture() == Core.atlas.find("white").getTexture() ? PageType.main :
            region.getTexture() == Core.atlas.find("stone1").getTexture() ? PageType.environment :
            region.getTexture() == Core.atlas.find("clear-editor").getTexture() ? PageType.editor :
            region.getTexture() == Core.atlas.find("zone-groundZero").getTexture() ? PageType.zone :
            region.getTexture() == Core.atlas.find("whiteui").getTexture() ? PageType.ui :
            PageType.main;
    }

    private PageType getPage(FileHandle file){
        String parent = file.parent().name();
        return
            parent.equals("environment") ? PageType.environment :
            parent.equals("editor") ? PageType.editor :
            parent.equals("zones") ? PageType.zone :
            parent.equals("ui") || file.parent().parent().name().equals("ui") ? PageType.ui :
            PageType.main;
    }

    /** Removes a mod file and marks it for requiring a restart. */
    public void removeMod(LoadedMod mod){
        if(mod.root instanceof ZipFileHandle){
            mod.root.delete();
        }

        boolean deleted = mod.file.isDirectory() ? mod.file.deleteDirectory() : mod.file.delete();

        if(!deleted){
            ui.showErrorMessage("$mod.delete.error");
            return;
        }
        loaded.remove(mod);
        disabled.remove(mod);
        requiresReload = true;
    }

    public boolean requiresReload(){
        return requiresReload;
    }

    /** Loads all mods from the folder, but does not call any methods on them.*/
    public void load(){
        for(FileHandle file : modDirectory.list()){
            if(!file.extension().equals("jar") && !file.extension().equals("zip") && !(file.isDirectory() && (file.child("mod.json").exists() || file.child("mod.hjson").exists()))) continue;

            Log.debug("[Mods] Loading mod {0}", file);
            try{
                LoadedMod mod = loadMod(file);
                if(mod.enabled() || headless){
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
        for(FileHandle file : platform.getWorkshopContent(LoadedMod.class)){
            try{
                LoadedMod mod = loadMod(file);
                if(mod.enabled()){
                    loaded.add(mod);
                }else{
                    disabled.add(mod);
                }
                mod.addSteamID(file.name());
            }catch(Exception e){
                Log.err("Failed to load mod workshop file {0}. Skipping.", file);
                Log.err(e);
            }
        }

        resolveDependencies();

        //sort mods to make sure servers handle them properly.
        loaded.sort(Structs.comparing(m -> m.name));

        buildFiles();
    }

    private void resolveDependencies(){
        Array<LoadedMod> incompatible = loaded.select(m -> !m.isSupported());
        loaded.removeAll(incompatible);
        disabled.addAll(incompatible);

        for(LoadedMod mod : Array.<LoadedMod>withArrays(loaded, disabled)){
            updateDependencies(mod);
        }

        disabled.addAll(loaded.select(LoadedMod::hasUnmetDependencies));
        loaded.removeAll(LoadedMod::hasUnmetDependencies);
        disabled.each(mod -> setEnabled(mod, false));
        disabled.distinct();
        loaded.distinct();
    }

    private void updateDependencies(LoadedMod mod){
        mod.dependencies.clear();
        mod.missingDependencies.clear();
        mod.dependencies = mod.meta.dependencies.map(this::locateMod);

        for(int i = 0; i < mod.dependencies.size; i++){
            if(mod.dependencies.get(i) == null){
                mod.missingDependencies.add(mod.meta.dependencies.get(i));
            }
        }
    }

    private void topoSort(LoadedMod mod, Array<LoadedMod> stack, ObjectSet<LoadedMod> visited){
        visited.add(mod);
        mod.dependencies.each(m -> !visited.contains(m), m -> topoSort(m, stack, visited));
        stack.add(mod);
    }

    /** @return mods ordered in the correct way needed for dependencies. */
    private Array<LoadedMod> orderedMods(){
        ObjectSet<LoadedMod> visited = new ObjectSet<>();
        Array<LoadedMod> result = new Array<>();
        for(LoadedMod mod : loaded){
            if(!visited.contains(mod)){
                topoSort(mod, result, visited);
            }
        }
        return result;
    }

    private LoadedMod locateMod(String name){
        return loaded.find(mod -> mod.name.equals(name));
    }

    private void buildFiles(){
        for(LoadedMod mod : orderedMods()){
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
        //TODO make it less epic
        Core.atlas = new TextureAtlas(Core.files.internal("sprites/sprites.atlas"));

        loaded.clear();
        disabled.clear();
        load();
        Sounds.dispose();
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

        Events.fire(new ContentReloadEvent());
    }

    /** Creates all the content found in mod files. */
    public void loadContent(){
        class LoadRun implements Comparable<LoadRun>{
            final ContentType type;
            final FileHandle file;
            final LoadedMod mod;

            public LoadRun(ContentType type, FileHandle file, LoadedMod mod){
                this.type = type;
                this.file = file;
                this.mod = mod;
            }

            @Override
            public int compareTo(LoadRun l){
                int mod = this.mod.name.compareTo(l.mod.name);
                if(mod != 0) return mod;
                return this.file.name().compareTo(l.file.name());
            }
        }

        Array<LoadRun> runs = new Array<>();

        for(LoadedMod mod : orderedMods()){
            if(mod.root.child("content").exists()){
                FileHandle contentRoot = mod.root.child("content");
                for(ContentType type : ContentType.all){
                    FileHandle folder = contentRoot.child(type.name().toLowerCase() + "s");
                    if(folder.exists()){
                        for(FileHandle file : folder.list()){
                            if(file.extension().equals("json") || file.extension().equals("hjson")){
                                runs.add(new LoadRun(type, file, mod));
                            }
                        }
                    }
                }
            }
        }

        //make sure mod content is in proper order
        runs.sort();
        runs.each(l -> safeRun(l.mod, () -> {
            try{
                //this binds the content but does not load it entirely
                Content loaded = parser.parse(l.mod, l.file.nameWithoutExtension(), l.file.readString("UTF-8"), l.file, l.type);
                Log.debug("[{0}] Loaded '{1}'.", l.mod.meta.name,
                (loaded instanceof UnlockableContent ? ((UnlockableContent)loaded).localizedName : loaded));
            }catch(Exception e){
                throw new RuntimeException("Failed to parse content file '" + l.file + "' for mod '" + l.mod.meta.name + "'.", e);
            }
        }));

        //this finishes parsing content fields
        parser.finishParsing();

        //load content for code mods
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
            Core.settings.putSave("mod-" + mod.name + "-enabled", enabled);
            Core.settings.save();
            requiresReload = true;
            if(!enabled){
                loaded.remove(mod);
                if(!disabled.contains(mod)) disabled.add(mod);
            }else{
                if(!loaded.contains(mod)) loaded.add(mod);
                disabled.remove(mod);
            }
            loaded.each(this::updateDependencies);
            disabled.each(this::updateDependencies);
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
    public void each(Cons<Mod> cons){
        loaded.each(p -> p.mod != null, p -> safeRun(p, () -> cons.get(p.mod)));
    }

    public void handleError(Throwable t, LoadedMod mod){
        Array<Throwable> causes = Strings.getCauses(t);
        Content content = null;
        for(Throwable e : causes){
            if(e instanceof ModLoadException && ((ModLoadException) e).content != null){
                content = ((ModLoadException) e).content;
            }
        }

        String realCause = "<???>";
        for(int i = causes.size -1 ; i >= 0; i--){
            if(causes.get(i).getMessage() != null){
                realCause = causes.get(i).getMessage();
                break;
            }
        }

        setEnabled(mod, false);

        if(content != null){
            throw new ModLoadException(Strings.format("Error loading '{0}' from mod '{1}' ({2}):\n{3}",
                content, mod.meta.name, content.sourceFile == null ? "<unknown file>" : content.sourceFile.name(), realCause), content, t);
        }else{
            throw new ModLoadException("Error loading mod " + mod.meta.name, t);
        }
    }

    public void safeRun(LoadedMod mod, Runnable run){
        try{
            run.run();
        }catch(Throwable t){
            handleError(t, mod);
        }
    }

    /** Loads a mod file+meta, but does not add it to the list.
     * Note that directories can be loaded as mods.*/
    private LoadedMod loadMod(FileHandle sourceFile) throws Exception{
        FileHandle zip = sourceFile.isDirectory() ? sourceFile : new ZipFileHandle(sourceFile);
        if(zip.list().length == 1 && zip.list()[0].isDirectory()){
            zip = zip.list()[0];
        }

        FileHandle metaf = zip.child("mod.json").exists() ? zip.child("mod.json") : zip.child("mod.hjson").exists() ? zip.child("mod.hjson") : zip.child("plugin.json");
        if(!metaf.exists()){
            Log.warn("Mod {0} doesn't have a 'mod.json'/'plugin.json'/'mod.js' file, skipping.", sourceFile);
            throw new IllegalArgumentException("No mod.json found.");
        }

        ModMeta meta = json.fromJson(ModMeta.class, Jval.read(metaf.readString()).toString(Jformat.plain));
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
    public static class LoadedMod implements Publishable{
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
        /** This mod's dependencies as already-loaded mods. */
        public Array<LoadedMod> dependencies = new Array<>();
        /** All missing dependencies of this mod as strings. */
        public Array<String> missingDependencies = new Array<>();

        public LoadedMod(FileHandle file, FileHandle root, Mod mod, ModMeta meta){
            this.root = root;
            this.file = file;
            this.mod = mod;
            this.meta = meta;
            this.name = meta.name.toLowerCase().replace(" ", "-");
        }

        public boolean enabled(){
            return Core.settings.getBool("mod-" + name + "-enabled", true);
        }

        public boolean hasUnmetDependencies(){
            return !missingDependencies.isEmpty();
        }

        /** @return whether this mod is supported by the game verison */
        public boolean isSupported(){
            if(Version.build <= 0 || meta.minGameVersion == null) return true;
            if(meta.minGameVersion.contains(".")){
                String[] split = meta.minGameVersion.split("\\.");
                if(split.length == 2){
                    return Version.build >= Strings.parseInt(split[0], 0) && Version.revision >= Strings.parseInt(split[1], 0);
                }
            }
            return Version.build >= Strings.parseInt(meta.minGameVersion, 0);
        }

        @Override
        public String getSteamID(){
            return Core.settings.getString(name + "-steamid", null);
        }

        @Override
        public void addSteamID(String id){
            Core.settings.put(name + "-steamid", id);
            Core.settings.save();
        }

        @Override
        public void removeSteamID(){
            Core.settings.remove(name + "-steamid");
            Core.settings.save();
        }

        @Override
        public String steamTitle(){
            return meta.name;
        }

        @Override
        public String steamDescription(){
            return meta.description;
        }

        @Override
        public String steamTag(){
            return "mod";
        }

        @Override
        public FileHandle createSteamFolder(String id){
            return file;
        }

        @Override
        public FileHandle createSteamPreview(String id){
            return file.child("preview.png");
        }

        @Override
        public boolean prePublish(){
            if(!file.isDirectory()){
                ui.showErrorMessage("$mod.folder.missing");
                return false;
            }

            if(!file.child("preview.png").exists()){
                ui.showErrorMessage("$mod.preview.missing");
                return false;
            }

            return true;
        }

        @Override
        public String toString(){
            return "LoadedMod{" +
            "file=" + file +
            ", root=" + root +
            ", name='" + name + '\'' +
            '}';
        }
    }

    /** Plugin metadata information.*/
    public static class ModMeta{
        public String name, author, description, version, main, minGameVersion;
        public Array<String> dependencies = Array.with();
        /** Hidden mods are only server-side or client-side, and do not support adding new content. */
        public boolean hidden;
    }

    /** Thrown when an error occurs while loading a mod.*/
    public static class ModLoadException extends RuntimeException{
        public Content content;
        public LoadedMod mod;

        public ModLoadException(String message, Throwable cause){
            super(message, cause);
        }

        public ModLoadException(String message, @Nullable Content content, Throwable cause){
            super(message, cause);
            this.content = content;
            if(content != null){
                this.mod = content.mod;
            }
        }

        public ModLoadException(@Nullable Content content, Throwable cause){
            super(cause);
            this.content = content;
            if(content != null){
                this.mod = content.mod;
            }
        }

        public ModLoadException(String message, @Nullable Content content){
            super(message);
            this.content = content;
            if(content != null){
                this.mod = content.mod;
            }
        }
    }
}
