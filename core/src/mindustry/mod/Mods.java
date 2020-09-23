package mindustry.mod;

import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.io.*;
import java.net.*;

import static mindustry.Vars.*;

public class Mods implements Loadable{
    private Json json = new Json();
    private @Nullable Scripts scripts;
    private ContentParser parser = new ContentParser();
    private ObjectMap<String, Seq<Fi>> bundles = new ObjectMap<>();
    private ObjectSet<String> specialFolders = ObjectSet.with("bundles", "sprites", "sprites-override");

    private int totalSprites;
    private MultiPacker packer;

    Seq<LoadedMod> mods = new Seq<>();
    private ObjectMap<Class<?>, ModMeta> metas = new ObjectMap<>();
    private boolean requiresReload, createdAtlas;

    public Mods(){
        Events.on(ClientLoadEvent.class, e -> Core.app.post(this::checkWarnings));
    }

    /** Returns a file named 'config.json' in a special folder for the specified plugin.
     * Call this in init(). */
    public Fi getConfig(Mod mod){
        ModMeta load = metas.get(mod.getClass());
        if(load == null) throw new IllegalArgumentException("Mod is not loaded yet (or missing)!");
        return modDirectory.child(load.name).child("config.json");
    }

    /** Returns a list of files per mod subdirectory. */
    public void listFiles(String directory, Cons2<LoadedMod, Fi> cons){
        eachEnabled(mod -> {
            Fi file = mod.root.child(directory);
            if(file.exists()){
                for(Fi child : file.list()){
                    cons.get(mod, child);
                }
            }
        });
    }

    /** @return the loaded mod found by class, or null if not found. */
    public @Nullable LoadedMod getMod(Class<? extends Mod> type){
        return mods.find(m -> m.enabled() && m.main != null && m.main.getClass() == type);
    }

    /** Imports an external mod file.*/
    public void importMod(Fi file) throws IOException{
        Fi dest = modDirectory.child(file.name());
        if(dest.exists()){
            throw new IOException("A file with the same name already exists in the mod folder!");
        }

        file.copyTo(dest);
        try{
            mods.add(loadMod(dest));
            requiresReload = true;
            sortMods();
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
        if(!mods.contains(LoadedMod::enabled)) return;
        Time.mark();

        packer = new MultiPacker();

        eachEnabled(mod -> {
            Seq<Fi> sprites = mod.root.child("sprites").findAll(f -> f.extension().equals("png"));
            Seq<Fi> overrides = mod.root.child("sprites-override").findAll(f -> f.extension().equals("png"));
            packSprites(sprites, mod, true);
            packSprites(overrides, mod, false);
            Log.debug("Packed @ images for mod '@'.", sprites.size + overrides.size, mod.meta.name);
            totalSprites += sprites.size + overrides.size;
        });

        Log.debug("Time to pack textures: @", Time.elapsed());
    }

    private void loadIcons(){
        for(LoadedMod mod : mods){
            //try to load icon for each mod that can have one
            if(mod.root.child("icon.png").exists()){
                try{
                    mod.iconTexture = new Texture(mod.root.child("icon.png"));
                }catch(Throwable t){
                    Log.err("Failed to load icon for mod '" + mod.name + "'.", t);
                }
            }
        }
    }

    private void packSprites(Seq<Fi> sprites, LoadedMod mod, boolean prefix){
        for(Fi file : sprites){
            try(InputStream stream = file.read()){
                byte[] bytes = Streams.copyBytes(stream, Math.max((int)file.length(), 512));
                Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                packer.add(getPage(file), (prefix ? mod.name + "-" : "") + file.nameWithoutExtension(), new PixmapRegion(pixmap));
                pixmap.dispose();
            }catch(IOException e){
                Core.app.post(() -> {
                    Log.err("Error packing images for mod: @", mod.meta.name);
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
        loadIcons();

        if(packer == null) return;
        Time.mark();

        //get textures packed
        if(totalSprites > 0){
            if(!createdAtlas) Core.atlas = new TextureAtlas(Core.files.internal("sprites/sprites.atlas"));
            createdAtlas = true;

            for(AtlasRegion region : Core.atlas.getRegions()){
                PageType type = getPage(region);
                if(!packer.has(type, region.name)){
                    packer.add(type, region.name, Core.atlas.getPixmap(region));
                }
            }

            TextureFilter filter = Core.settings.getBool("linear") ? TextureFilter.linear : TextureFilter.nearest;

            //flush so generators can use these sprites
            packer.flush(filter, Core.atlas);

            //generate new icons
            for(Seq<Content> arr : content.getContentMap()){
                arr.each(c -> {
                    if(c instanceof UnlockableContent && c.minfo.mod != null){
                        UnlockableContent u = (UnlockableContent)c;
                        u.load();
                        u.createIcons(packer);
                    }
                });
            }

            Core.atlas = packer.flush(filter, new TextureAtlas());
            Core.atlas.setErrorRegion("error");
            Log.debug("Total pages: @", Core.atlas.getTextures().size);
        }

        packer.dispose();
        packer = null;
        Log.debug("Time to update textures: @", Time.elapsed());
    }

    private PageType getPage(AtlasRegion region){
        return
            region.texture == Core.atlas.find("white").texture ? PageType.main :
            region.texture == Core.atlas.find("stone1").texture ? PageType.environment :
            region.texture == Core.atlas.find("clear-editor").texture ? PageType.editor :
            region.texture == Core.atlas.find("whiteui").texture ? PageType.ui :
            PageType.main;
    }

    private PageType getPage(Fi file){
        String parent = file.parent().name();
        return
            parent.equals("environment") ? PageType.environment :
            parent.equals("editor") ? PageType.editor :
            parent.equals("ui") || file.parent().parent().name().equals("ui") ? PageType.ui :
            PageType.main;
    }

    /** Removes a mod file and marks it for requiring a restart. */
    public void removeMod(LoadedMod mod){
        if(mod.root instanceof ZipFi){
            mod.root.delete();
        }

        boolean deleted = mod.file.isDirectory() ? mod.file.deleteDirectory() : mod.file.delete();

        if(!deleted){
            ui.showErrorMessage("@mod.delete.error");
            return;
        }
        mods.remove(mod);
        mod.dispose();
        requiresReload = true;
    }

    public Scripts getScripts(){
        if(scripts == null) scripts = platform.createScripts();
        return scripts;
    }

    /** @return whether the scripting engine has been initialized. */
    public boolean hasScripts(){
        return scripts != null;
    }

    public boolean requiresReload(){
        return requiresReload;
    }

    /** Loads all mods from the folder, but does not call any methods on them.*/
    public void load(){
        for(Fi file : modDirectory.list()){
            if(!file.extension().equals("jar") && !file.extension().equals("zip") && !(file.isDirectory() && (file.child("mod.json").exists() || file.child("mod.hjson").exists()))) continue;

            Log.debug("[Mods] Loading mod @", file);
            try{
                LoadedMod mod = loadMod(file);
                mods.add(mod);
            }catch(Throwable e){
                if(e instanceof ClassNotFoundException && e.getMessage().contains("mindustry.plugin.Plugin")){
                    Log.info("Plugin @ is outdated and needs to be ported to 6.0! Update its main class to inherit from 'mindustry.mod.Plugin'.");
                }else{
                    Log.err("Failed to load mod file @. Skipping.", file);
                    Log.err(e);
                }
            }
        }

        //load workshop mods now
        for(Fi file : platform.getWorkshopContent(LoadedMod.class)){
            try{
                LoadedMod mod = loadMod(file);
                mods.add(mod);
                mod.addSteamID(file.name());
            }catch(Throwable e){
                Log.err("Failed to load mod workshop file @. Skipping.", file);
                Log.err(e);
            }
        }

        resolveModState();
        sortMods();

        buildFiles();
    }

    private void sortMods(){
        //sort mods to make sure servers handle them properly and they appear correctly in the dialog
        mods.sort(Structs.comps(Structs.comparingInt(m -> m.state.ordinal()), Structs.comparing(m -> m.name)));
    }

    private void resolveModState(){
        mods.each(this::updateDependencies);

        for(LoadedMod mod : mods){
            mod.state =
                !mod.isSupported() ? ModState.unsupported :
                mod.hasUnmetDependencies() ? ModState.missingDependencies :
                !mod.shouldBeEnabled() ? ModState.disabled :
                ModState.enabled;
        }
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

    private void topoSort(LoadedMod mod, Seq<LoadedMod> stack, ObjectSet<LoadedMod> visited){
        visited.add(mod);
        mod.dependencies.each(m -> !visited.contains(m), m -> topoSort(m, stack, visited));
        stack.add(mod);
    }

    /** @return mods ordered in the correct way needed for dependencies. */
    private Seq<LoadedMod> orderedMods(){
        ObjectSet<LoadedMod> visited = new ObjectSet<>();
        Seq<LoadedMod> result = new Seq<>();
        eachEnabled(mod -> {
            if(!visited.contains(mod)){
                topoSort(mod, result, visited);
            }
        });
        return result;
    }

    public LoadedMod locateMod(String name){
        return mods.find(mod -> mod.enabled() && mod.name.equals(name));
    }

    private void buildFiles(){
        for(LoadedMod mod : orderedMods()){
            boolean zipFolder = !mod.file.isDirectory() && mod.root.parent() != null;
            String parentName = zipFolder ? mod.root.name() : null;
            for(Fi file : mod.root.list()){
                //ignore special folders like bundles or sprites
                if(file.isDirectory() && !specialFolders.contains(file.name())){
                    file.walk(f -> tree.addFile(mod.file.isDirectory() ? f.path().substring(1 + mod.file.path().length()) :
                        zipFolder ? f.path().substring(parentName.length() + 1) : f.path(), f));
                }
            }

            //load up bundles.
            Fi folder = mod.root.child("bundles");
            if(folder.exists()){
                for(Fi file : folder.list()){
                    if(file.name().startsWith("bundle") && file.extension().equals("properties")){
                        String name = file.nameWithoutExtension();
                        bundles.get(name, Seq::new).add(file);
                    }
                }
            }
        }

        //add new keys to each bundle
        I18NBundle bundle = Core.bundle;
        while(bundle != null){
            String str = bundle.getLocale().toString();
            String locale = "bundle" + (str.isEmpty() ? "" : "_" + str);
            for(Fi file : bundles.get(locale, Seq::new)){
                try{
                    PropertiesUtils.load(bundle.getProperties(), file.reader());
                }catch(Throwable e){
                    Log.err("Error loading bundle: " + file + "/" + locale, e);
                }
            }
            bundle = bundle.getParent();
        }
    }

    /** Check all warnings related to content and show relevant dialogs. Client only. */
    private void checkWarnings(){
        //show 'scripts have errored' info
        if(scripts != null && scripts.hasErrored()){
           ui.showErrorMessage("@mod.scripts.disable");
        }

        //show list of errored content
        if(mods.contains(LoadedMod::hasContentErrors)){
            ui.loadfrag.hide();
            new Dialog(""){{

                setFillParent(true);
                cont.margin(15);
                cont.add("@error.title");
                cont.row();
                cont.image().width(300f).pad(2).colspan(2).height(4f).color(Color.scarlet);
                cont.row();
                cont.add("@mod.errors").wrap().growX().center().get().setAlignment(Align.center);
                cont.row();
                cont.pane(p -> {
                    mods.each(m -> m.enabled() && m.hasContentErrors(), m -> {
                        p.add(m.name).color(Pal.accent).left();
                        p.row();
                        p.image().fillX().pad(4).color(Pal.accent);
                        p.row();
                        p.table(d -> {
                            d.left().marginLeft(15f);
                            for(Content c : m.erroredContent){
                                d.add(c.minfo.sourceFile.nameWithoutExtension()).left().padRight(10);
                                d.button("@details", Icon.downOpen, Styles.transt, () -> {
                                    new Dialog(""){{
                                        setFillParent(true);
                                        cont.pane(e -> e.add(c.minfo.error).wrap().grow()).grow();
                                        cont.row();
                                        cont.button("@ok", Icon.left, this::hide).size(240f, 60f);
                                    }}.show();
                                }).size(190f, 50f).left().marginLeft(6);
                                d.row();
                            }
                        }).left();
                        p.row();
                    });
                });

                cont.row();
                cont.button("@ok", this::hide).size(300, 50);
            }}.show();
        }
    }

    public boolean hasContentErrors(){
        return mods.contains(LoadedMod::hasContentErrors) || (scripts != null && scripts.hasErrored());
    }

    /** This must be run on the main thread! */
    public void loadScripts(){
        Time.mark();
        boolean[] any = {false};

        try{
            eachEnabled(mod -> {
                if(mod.root.child("scripts").exists()){
                    content.setCurrentMod(mod);
                    //if there's only one script file, use it (for backwards compatibility); if there isn't, use "main.js"
                    Seq<Fi> allScripts = mod.root.child("scripts").findAll(f -> f.extEquals("js"));
                    Fi main = allScripts.size == 1 ? allScripts.first() : mod.root.child("scripts").child("main.js");
                    if(main.exists() && !main.isDirectory()){
                        try{
                            if(scripts == null){
                                scripts = platform.createScripts();
                            }
                            any[0] = true;
                            scripts.run(mod, main);
                        }catch(Throwable e){
                            Core.app.post(() -> {
                                Log.err("Error loading main script @ for mod @.", main.name(), mod.meta.name);
                                Log.err(e);
                            });
                        }
                    }else{
                        Core.app.post(() -> Log.err("No main.js found for mod @.", mod.meta.name));
                    }
                }
            });
        }finally{
            content.setCurrentMod(null);
        }

        if(any[0]){
            Log.info("Time to initialize modded scripts: @", Time.elapsed());
        }
    }

    /** Creates all the content found in mod files. */
    public void loadContent(){

        //load class mod content first
        for(LoadedMod mod : orderedMods()){
            //hidden mods can't load content
            if(mod.main != null && !mod.meta.hidden){
                content.setCurrentMod(mod);
                mod.main.loadContent();
            }
        }

        content.setCurrentMod(null);

        class LoadRun implements Comparable<LoadRun>{
            final ContentType type;
            final Fi file;
            final LoadedMod mod;

            public LoadRun(ContentType type, Fi file, LoadedMod mod){
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

        Seq<LoadRun> runs = new Seq<>();

        for(LoadedMod mod : orderedMods()){
            if(mod.root.child("content").exists()){
                Fi contentRoot = mod.root.child("content");
                for(ContentType type : ContentType.all){
                    Fi folder = contentRoot.child(type.name().toLowerCase() + "s");
                    if(folder.exists()){
                        for(Fi file : folder.findAll(f -> f.extension().equals("json") || f.extension().equals("hjson"))){
                            runs.add(new LoadRun(type, file, mod));
                        }
                    }
                }
            }
        }

        //make sure mod content is in proper order
        runs.sort();
        for(LoadRun l : runs){
            Content current = content.getLastAdded();
            try{
                //this binds the content but does not load it entirely
                Content loaded = parser.parse(l.mod, l.file.nameWithoutExtension(), l.file.readString("UTF-8"), l.file, l.type);
                Log.debug("[@] Loaded '@'.", l.mod.meta.name, (loaded instanceof UnlockableContent ? ((UnlockableContent)loaded).localizedName : loaded));
            }catch(Throwable e){
                if(current != content.getLastAdded() && content.getLastAdded() != null){
                    parser.markError(content.getLastAdded(), l.mod, l.file, e);
                }else{
                    ErrorContent error = new ErrorContent();
                    parser.markError(error, l.mod, l.file, e);
                }
            }
        }

        //this finishes parsing content fields
        parser.finishParsing();
    }

    public void handleContentError(Content content, Throwable error){
        parser.markError(content, error);
    }

    /** @return a list of mods and versions, in the format name:version. */
    public Seq<String> getModStrings(){
        return mods.select(l -> !l.meta.hidden && l.enabled()).map(l -> l.name + ":" + l.meta.version);
    }

    /** Makes a mod enabled or disabled. shifts it.*/
    public void setEnabled(LoadedMod mod, boolean enabled){
        if(mod.enabled() != enabled){
            Core.settings.put("mod-" + mod.name + "-enabled", enabled);
            requiresReload = true;
            mod.state = enabled ? ModState.enabled : ModState.disabled;
            mods.each(this::updateDependencies);
            sortMods();
        }
    }

    /** @return the mods that the client is missing.
     * The inputted array is changed to contain the extra mods that the client has but the server doesn't.*/
    public Seq<String> getIncompatibility(Seq<String> out){
        Seq<String> mods = getModStrings();
        Seq<String> result = mods.copy();
        for(String mod : mods){
            if(out.remove(mod)){
                result.remove(mod);
            }
        }
        return result;
    }

    public Seq<LoadedMod> list(){
        return mods;
    }

    /** Iterates through each mod with a main class. */
    public void eachClass(Cons<Mod> cons){
        mods.each(p -> p.main != null, p -> contextRun(p, () -> cons.get(p.main)));
    }

    /** Iterates through each enabled mod. */
    public void eachEnabled(Cons<LoadedMod> cons){
        mods.each(LoadedMod::enabled, cons);
    }

    public void contextRun(LoadedMod mod, Runnable run){
        try{
            run.run();
        }catch(Throwable t){
            throw new RuntimeException("Error loading mod " + mod.meta.name, t);
        }
    }

    /** Loads a mod file+meta, but does not add it to the list.
     * Note that directories can be loaded as mods.*/
    private LoadedMod loadMod(Fi sourceFile) throws Exception{
        Time.mark();

        ZipFi rootZip = null;

        try{
            Fi zip = sourceFile.isDirectory() ? sourceFile : (rootZip = new ZipFi(sourceFile));
            if(zip.list().length == 1 && zip.list()[0].isDirectory()){
                zip = zip.list()[0];
            }

            Fi metaf =
                zip.child("mod.json").exists() ? zip.child("mod.json") :
                zip.child("mod.hjson").exists() ? zip.child("mod.hjson") :
                zip.child("plugin.json").exists() ? zip.child("plugin.json") :
                zip.child("plugin.hjson");

            if(!metaf.exists()){
                Log.warn("Mod @ doesn't have a '[mod/plugin].[h]json' file, skipping.", sourceFile);
                throw new IllegalArgumentException("Invalid file: No mod.json found.");
            }

            ModMeta meta = json.fromJson(ModMeta.class, Jval.read(metaf.readString()).toString(Jformat.plain));
            meta.cleanup();
            String camelized = meta.name.replace(" ", "");
            String mainClass = meta.main == null ? camelized.toLowerCase() + "." + camelized + "Mod" : meta.main;
            String baseName = meta.name.toLowerCase().replace(" ", "-");

            if(mods.contains(m -> m.name.equals(baseName))){
                throw new IllegalArgumentException("A mod with the name '" + baseName + "' is already imported.");
            }

            Mod mainMod;

            Fi mainFile = zip;
            String[] path = (mainClass.replace('.', '/') + ".class").split("/");
            for(String str : path){
                if(!str.isEmpty()){
                    mainFile = mainFile.child(str);
                }
            }

            //make sure the main class exists before loading it; if it doesn't just don't put it there
            if(mainFile.exists()){
                //mobile versions don't support class mods
                if(mobile){
                    throw new IllegalArgumentException("Java class mods are not supported on mobile.");
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

            if(!headless){
                Log.info("Loaded mod '@' in @ms", meta.name, Time.elapsed());
            }
            return new LoadedMod(sourceFile, zip, mainMod, meta);

        }catch(Exception e){
            //delete root zip file so it can be closed on windows
            if(rootZip != null) rootZip.delete();
            throw e;
        }
    }

    /** Represents a mod's state. May be a jar file, folder or zip. */
    public static class LoadedMod implements Publishable, Disposable{
        /** The location of this mod's zip file/folder on the disk. */
        public final Fi file;
        /** The root zip file; points to the contents of this mod. In the case of folders, this is the same as the mod's file. */
        public final Fi root;
        /** The mod's main class; may be null. */
        public final @Nullable Mod main;
        /** Internal mod name. Used for textures. */
        public final String name;
        /** This mod's metadata. */
        public final ModMeta meta;
        /** This mod's dependencies as already-loaded mods. */
        public Seq<LoadedMod> dependencies = new Seq<>();
        /** All missing dependencies of this mod as strings. */
        public Seq<String> missingDependencies = new Seq<>();
        /** Script files to run. */
        public Seq<Fi> scripts = new Seq<>();
        /** Content with intialization code. */
        public ObjectSet<Content> erroredContent = new ObjectSet<>();
        /** Current state of this mod. */
        public ModState state = ModState.enabled;
        /** Icon texture. Should be disposed. */
        public @Nullable Texture iconTexture;

        public LoadedMod(Fi file, Fi root, Mod main, ModMeta meta){
            this.root = root;
            this.file = file;
            this.main = main;
            this.meta = meta;
            this.name = meta.name.toLowerCase().replace(" ", "-");
        }

        public boolean enabled(){
            return state == ModState.enabled || state == ModState.contentErrors;
        }

        public boolean shouldBeEnabled(){
            return Core.settings.getBool("mod-" + name + "-enabled", true);
        }

        public boolean hasUnmetDependencies(){
            return !missingDependencies.isEmpty();
        }

        public boolean hasContentErrors(){
            return !erroredContent.isEmpty();
        }

        /** @return whether this mod is supported by the game verison */
        public boolean isSupported(){
            if(isOutdated()) return false;

            int major = getMinMajor(), minor = getMinMinor();

            if(Version.build <= 0) return true;

            return Version.build >= major && Version.revision >= minor;
        }

        /** @return whether this mod is outdated, e.g. not compatible with v6. */
        public boolean isOutdated(){
            //must be at least 105 to indicate v6 compat
            return getMinMajor() < 105;
        }

        public int getMinMajor(){
            int major = 0;

            String ver = meta.minGameVersion == null ? "0" : meta.minGameVersion;

            if(ver.contains(".")){
                String[] split = ver.split("\\.");
                if(split.length == 2){
                    major = Strings.parseInt(split[0], 0);
                }
            }else{
                major = Strings.parseInt(ver, 0);
            }

            return major;
        }

        public int getMinMinor(){
            String ver = meta.minGameVersion == null ? "0" : meta.minGameVersion;

            if(ver.contains(".")){
                String[] split = ver.split("\\.");
                if(split.length == 2){
                    return Strings.parseInt(split[1], 0);
                }
            }

            return 0;
        }

        @Override
        public void dispose(){
            if(iconTexture != null){
                iconTexture.dispose();
                iconTexture = null;
            }
        }

        @Override
        public String getSteamID(){
            return Core.settings.getString(name + "-steamid", null);
        }

        @Override
        public void addSteamID(String id){
            Core.settings.put(name + "-steamid", id);
        }

        @Override
        public void removeSteamID(){
            Core.settings.remove(name + "-steamid");
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
        public Fi createSteamFolder(String id){
            return file;
        }

        @Override
        public Fi createSteamPreview(String id){
            return file.child("preview.png");
        }

        @Override
        public boolean prePublish(){
            if(!file.isDirectory()){
                ui.showErrorMessage("@mod.folder.missing");
                return false;
            }

            if(!file.child("preview.png").exists()){
                ui.showErrorMessage("@mod.preview.missing");
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

    /** Mod metadata information.*/
    public static class ModMeta{
        public String name, displayName, author, description, version, main, minGameVersion = "0";
        public Seq<String> dependencies = Seq.with();
        /** Hidden mods are only server-side or client-side, and do not support adding new content. */
        public boolean hidden;

        public String displayName(){
            return displayName == null ? name : displayName;
        }

        //removes all colors
        public void cleanup(){
            if(displayName != null) displayName = Strings.stripColors(displayName);
            if(author != null) author = Strings.stripColors(author);
            if(description != null) description = Strings.stripColors(description);
        }
    }

    public enum ModState{
        enabled,
        contentErrors,
        missingDependencies,
        unsupported,
        disabled,
    }
}
