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
import arc.util.io.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.mod.ContentParser.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

public class Mods implements Loadable{
    private static final String[] metaFiles = {"mod.json", "mod.hjson", "plugin.json", "plugin.hjson"};
    private static final ObjectSet<String> blacklistedMods = ObjectSet.with("ui-lib");

    private Json json = new Json();
    private @Nullable Scripts scripts;
    private ContentParser parser = new ContentParser();
    private ObjectMap<String, Seq<Fi>> bundles = new ObjectMap<>();
    private ObjectSet<String> specialFolders = ObjectSet.with("bundles", "sprites", "sprites-override");

    private int totalSprites;
    private ObjectFloatMap<String> textureResize = new ObjectFloatMap<>();
    private MultiPacker packer;

    /** Ordered mods cache. Set to null to invalidate. */
    private @Nullable Seq<LoadedMod> lastOrderedMods = new Seq<>();

    private ModClassLoader mainLoader = new ModClassLoader(getClass().getClassLoader());

    Seq<LoadedMod> mods = new Seq<>();
    private ObjectMap<Class<?>, ModMeta> metas = new ObjectMap<>();
    private boolean requiresReload;

    public Mods(){
        Events.on(ClientLoadEvent.class, e -> Core.app.post(this::checkWarnings));
    }

    /** @return the main class loader for all mods */
    public ClassLoader mainLoader(){
        return mainLoader;
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

    /** @return the loaded mod found by name, or null if not found. */
    public @Nullable LoadedMod getMod(String name){
        return mods.find(m -> m.name.equals(name));
    }

    /** @return the loaded mod found by class, or null if not found. */
    public @Nullable LoadedMod getMod(Class<? extends Mod> type){
        return mods.find(m -> m.main != null && m.main.getClass() == type);
    }

    /** Imports an external mod file. Folders are not supported here. */
    public LoadedMod importMod(Fi file) throws IOException{
        //for some reason, android likes to add colons to file names, e.g. primary:ExampleJavaMod.jar, which breaks dexing
        String baseName = file.nameWithoutExtension().replace(':', '_').replace(' ', '_');
        String finalName = baseName;
        //find a name to prevent any name conflicts
        int count = 1;
        while(modDirectory.child(finalName + ".zip").exists()){
            finalName = baseName + "" + count++;
        }

        Fi dest = modDirectory.child(finalName + ".zip");

        file.copyTo(dest);
        try{
            var loaded = loadMod(dest, true, true);
            mods.add(loaded);
            //invalidate ordered mods cache
            lastOrderedMods = null;
            requiresReload = true;
            //enable the mod on import
            Core.settings.put("mod-" + loaded.name + "-enabled", true);
            sortMods();
            //try to load the mod's icon so it displays on import
            Core.app.post(() -> loadIcon(loaded));

            Events.fire(Trigger.importMod);

            return loaded;
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

        //TODO this should estimate sprite sizes per page
        packer = new MultiPacker();
        //all packing tasks to await
        var tasks = new Seq<Future<Runnable>>();

        eachEnabled(mod -> {
            Seq<Fi> sprites = mod.root.child("sprites").findAll(f -> f.extension().equals("png"));
            Seq<Fi> overrides = mod.root.child("sprites-override").findAll(f -> f.extension().equals("png"));

            packSprites(sprites, mod, true, tasks);
            packSprites(overrides, mod, false, tasks);

            Log.debug("Packed @ images for mod '@'.", sprites.size + overrides.size, mod.meta.name);
            totalSprites += sprites.size + overrides.size;
        });

        for(var result : tasks){
            try{
                var packRun = result.get();
                if(packRun != null){ //can be null for very strange reasons, ignore if that's the case
                    try{
                        //actually pack the image
                        packRun.run();
                    }catch(Exception e){ //the image can fail to fit in the spritesheet
                        Log.err("Failed to fit image into the spritesheet, skipping.");
                        Log.err(e);
                    }
                }
            }catch(Exception e){ //this means loading the image failed, log it and move on
                Log.err(e);
            }
        }

        Log.debug("Time to pack textures: @", Time.elapsed());
    }

    private void loadIcons(){
        for(LoadedMod mod : mods){
            loadIcon(mod);
        }
    }

    private void loadIcon(LoadedMod mod){
        //try to load icon for each mod that can have one
        if(mod.root.child("icon.png").exists() && !headless){
            try{
                mod.iconTexture = new Texture(mod.root.child("icon.png"));
                mod.iconTexture.setFilter(TextureFilter.linear);
            }catch(Throwable t){
                Log.err("Failed to load icon for mod '" + mod.name + "'.", t);
            }
        }
    }

    private void packSprites(Seq<Fi> sprites, LoadedMod mod, boolean prefix, Seq<Future<Runnable>> tasks){
        boolean bleed = Core.settings.getBool("linear", true) && !mod.meta.pregenerated;
        float textureScale = mod.meta.texturescale;

        for(Fi file : sprites){
            String name = file.nameWithoutExtension();

            if(!prefix && !Core.atlas.has(name)){
                Log.warn("Sprite '@' in mod '@' attempts to override a non-existent sprite. Ignoring.", name, mod.name);
                continue;

                //(horrible code below)
            }else if(prefix && !mod.meta.keepOutlines && name.endsWith("-outline") && file.path().contains("units") && !file.path().contains("blocks")){
                Log.warn("Sprite '@' in mod '@' is likely to be an unnecessary unit outline. These should not be separate sprites. Ignoring.", name, mod.name);
                //TODO !!! document this on the wiki !!!
                //do not allow packing standard outline sprites for now, they are no longer necessary and waste space!
                //TODO also full regions are bad:  || name.endsWith("-full")
                continue;
            }

            //read and bleed pixmaps in parallel
            tasks.add(mainExecutor.submit(() -> {

                try{
                    Pixmap pix = new Pixmap(file.readBytes());
                    //only bleeds when linear filtering is on at startup
                    if(bleed){
                        Pixmaps.bleed(pix, 2);
                    }
                    //this returns a *runnable* which actually packs the resulting pixmap; this has to be done synchronously outside the method
                    return () -> {
                        String fullName = (prefix ? mod.name + "-" : "") + name;
                        packer.add(getPage(file), fullName, new PixmapRegion(pix));
                        if(textureScale != 1.0f){
                            textureResize.put(fullName, textureScale);
                        }
                        pix.dispose();
                    };
                }catch(Exception e){
                    //rethrow exception with details about the cause of failure
                    throw new Exception("Failed to load image " + file + " for mod " + mod.name, e);
                }
            }));
        }
    }

    @Override
    public void loadSync(){
        loadIcons();

        if(packer == null) return;
        Time.mark();

        //get textures packed
        if(totalSprites > 0){

            class RegionEntry{
                String name;
                PixmapRegion region;
                int[] splits, pads;

                RegionEntry(String name, PixmapRegion region, int[] splits, int[] pads){
                    this.name = name;
                    this.region = region;
                    this.splits = splits;
                    this.pads = pads;
                }
            }

            Seq<RegionEntry>[] entries = new Seq[PageType.all.length];
            for(int i = 0; i < PageType.all.length; i++){
                entries[i] = new Seq<>();
            }

            ObjectMap<Texture, PageType> pageTypes = ObjectMap.of(
            Core.atlas.find("white").texture, PageType.main,
            Core.atlas.find("stone1").texture, PageType.environment,
            Core.atlas.find("clear-editor").texture, PageType.editor,
            Core.atlas.find("whiteui").texture, PageType.ui,
            Core.atlas.find("rubble-1-0").texture, PageType.rubble
            );

            for(AtlasRegion region : Core.atlas.getRegions()){
                PageType type = pageTypes.get(region.texture, PageType.main);

                if(!packer.has(type, region.name)){
                    entries[type.ordinal()].add(new RegionEntry(region.name, Core.atlas.getPixmap(region), region.splits, region.pads));
                }
            }

            //sort each page type by size first, for optimal packing
            for(int i = 0; i < PageType.all.length; i++){
                var rects = entries[i];
                var type = PageType.all[i];
                //TODO is this in reverse order?
                rects.sort(Structs.comparingInt(o -> -Math.max(o.region.width, o.region.height)));

                for(var entry : rects){
                    packer.add(type, entry.name, entry.region, entry.splits, entry.pads);
                }
            }

            Core.atlas.dispose();

            //dead shadow-atlas for getting regions, but not pixmaps
            var shadow = Core.atlas;
            //dummy texture atlas that returns the 'shadow' regions; used for mod loading
            Core.atlas = new TextureAtlas(){
                {
                    //needed for the correct operation of the found() method in the TextureRegion
                    error = shadow.find("error");
                }

                @Override
                public AtlasRegion find(String name){
                    var base = packer.get(name);

                    if(base != null){
                        var reg = new AtlasRegion(shadow.find(name).texture, base.x, base.y, base.width, base.height);
                        reg.name = name;
                        reg.pixmapRegion = base;
                        return reg;
                    }

                    return shadow.find(name);
                }

                @Override
                public boolean isFound(TextureRegion region){
                    return region != shadow.find("error");
                }

                @Override
                public TextureRegion find(String name, TextureRegion def){
                    return !has(name) ? def : find(name);
                }

                @Override
                public boolean has(String s){
                    return shadow.has(s) || packer.get(s) != null;
                }

                //return the *actual* pixmap regions, not the disposed ones.
                @Override
                public PixmapRegion getPixmap(AtlasRegion region){
                    PixmapRegion out = packer.get(region.name);
                    //this should not happen in normal situations
                    if(out == null) return packer.get("error");
                    return out;
                }
            };

            TextureFilter filter = Core.settings.getBool("linear", true) ? TextureFilter.linear : TextureFilter.nearest;

            Time.mark();
            //generate new icons
            for(Seq<Content> arr : content.getContentMap()){
                arr.each(c -> {
                    //TODO this can be done in parallel
                    if(c instanceof UnlockableContent u && c.minfo.mod != null){
                        u.load();
                        u.loadIcon();
                        if(u.generateIcons && !c.minfo.mod.meta.pregenerated){
                            u.createIcons(packer);
                        }
                    }
                });
            }
            Log.debug("Time to generate icons: @", Time.elapsed());

            //dispose old atlas data
            Core.atlas = packer.flush(filter, new TextureAtlas());

            textureResize.each(e -> Core.atlas.find(e.key).scale = e.value);

            Core.atlas.setErrorRegion("error");
            Log.debug("Total pages: @", Core.atlas.getTextures().size);

            packer.printStats();
        }

        packer.dispose();
        packer = null;
        Log.debug("Total time to generate & flush textures synchronously: @", Time.elapsed());
    }

    private PageType getPage(Fi file){
        String path = file.path();
        return
            path.contains("sprites/blocks/environment") || path.contains("sprites-override/blocks/environment") ? PageType.environment :
            path.contains("sprites/editor") || path.contains("sprites-override/editor") ? PageType.editor :
            path.contains("sprites/rubble") || path.contains("sprites-override/rubble") ? PageType.rubble :
            path.contains("sprites/ui") || path.contains("sprites-override/ui") ? PageType.ui :
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

    /** @return whether to skip mod loading due to previous initialization failure. */
    public boolean skipModLoading(){
        return failedToLaunch && Core.settings.getBool("modcrashdisable", true);
    }

    /** Loads all mods from the folder, but does not call any methods on them.*/
    public void load(){
        var candidates = new Seq<Fi>();

        // Add local mods
        Seq.with(modDirectory.list())
        .filter(f -> f.extEquals("jar") || f.extEquals("zip") || (f.isDirectory() && Structs.contains(metaFiles, meta -> f.child(meta).exists())))
        .each(candidates::add);

        // Add Steam workshop mods
        platform.getWorkshopContent(LoadedMod.class)
        .each(candidates::add);

        var mapping = new ObjectMap<String, Fi>();
        var metas = new Seq<ModMeta>();

        for(Fi file : candidates){
            ModMeta meta = null;

            try{
                Fi zip = file.isDirectory() ? file : new ZipFi(file);

                if(zip.list().length == 1 && zip.list()[0].isDirectory()){
                    zip = zip.list()[0];
                }

                meta = findMeta(zip);
            }catch(Throwable ignored){
            }

            if(meta == null || meta.name == null) continue;
            metas.add(meta);
            mapping.put(meta.name, file);
        }

        var resolved = resolveDependencies(metas);
        for(var entry : resolved){
            var file = mapping.get(entry.key);
            var steam = platform.getWorkshopContent(LoadedMod.class).contains(file);

            Log.debug("[Mods] Loading mod @", file);

            try{
                LoadedMod mod = loadMod(file, false, entry.value == ModState.enabled);
                mod.state = entry.value;
                mods.add(mod);
                //invalidate ordered mods cache
                lastOrderedMods = null;
                if(steam) mod.addSteamID(file.name());
            }catch(Throwable e){
                if(e instanceof ClassNotFoundException && e.getMessage().contains("mindustry.plugin.Plugin")){
                    Log.info("Plugin '@' is outdated and needs to be ported to 6.0! Update its main class to inherit from 'mindustry.mod.Plugin'. See https://mindustrygame.github.io/wiki/modding/6-migrationv6/", file.name());
                }else if(steam){
                    Log.err("Failed to load mod workshop file @. Skipping.", file);
                    Log.err(e);
                }else{
                    Log.err("Failed to load mod file @. Skipping.", file);
                    Log.err(e);
                }
            }
        }

        // Resolve the state
        mods.each(this::updateDependencies);
        for(var mod : mods){
            // Skip mods where the state has already been resolved
            if(mod.state != ModState.enabled) continue;
            if(!mod.isSupported()){
                mod.state = ModState.unsupported;
            }else if(!mod.shouldBeEnabled()){
                mod.state = ModState.disabled;
            }
        }

        sortMods();
        buildFiles();
    }

    private void sortMods(){
        //sort mods to make sure servers handle them properly and they appear correctly in the dialog
        mods.sort(Structs.comps(Structs.comparingInt(m -> m.state.ordinal()), Structs.comparing(m -> m.name)));
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

    /** @return mods ordered in the correct way needed for dependencies. */
    public Seq<LoadedMod> orderedMods(){
        //update cache if it's "dirty"/empty
        if(lastOrderedMods == null){
            //only enabled mods participate; this state is resolved in load()
            Seq<LoadedMod> enabled = mods.select(LoadedMod::enabled);

            var mapping = enabled.asMap(m -> m.meta.name);
            lastOrderedMods = resolveDependencies(enabled.map(m -> m.meta)).orderedKeys().map(mapping::get);
        }
        return lastOrderedMods;
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
        Events.fire(new FileTreeInitEvent());

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
                                d.button("@details", Icon.downOpen, Styles.cleart, () -> {
                                    new Dialog(""){{
                                        setFillParent(true);
                                        cont.pane(e -> e.add(c.minfo.error).wrap().grow().labelAlign(Align.center, Align.left)).grow();
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
                    String lower = type.name().toLowerCase(Locale.ROOT);
                    Fi folder = contentRoot.child(lower + (lower.endsWith("s") ? "" : "s"));
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
                Log.debug("[@] Loaded '@'.", l.mod.meta.name, (loaded instanceof UnlockableContent u ? u.localizedName : loaded));
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

    /** Adds a listener for parsed JSON objects. */
    public void addParseListener(ParseListener hook){
        parser.listeners.add(hook);
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
        orderedMods().each(p -> p.main != null, p -> contextRun(p, () -> cons.get(p.main)));
    }

    /** Iterates through each enabled mod. */
    public void eachEnabled(Cons<LoadedMod> cons){
        orderedMods().each(LoadedMod::enabled, cons);
    }

    public void contextRun(LoadedMod mod, Runnable run){
        try{
            run.run();
        }catch(Throwable t){
            throw new RuntimeException("Error loading mod " + mod.meta.name, t);
        }
    }

    /** Tries to find the config file of a mod/plugin. */
    @Nullable
    public ModMeta findMeta(Fi file){
        Fi metaFile = null;
        for(String name : metaFiles){
            if((metaFile = file.child(name)).exists()){
                break;
            }
        }

        if(!metaFile.exists()){
            return null;
        }

        ModMeta meta = json.fromJson(ModMeta.class, Jval.read(metaFile.readString()).toString(Jformat.plain));
        meta.cleanup();
        return meta;
    }

    /** Resolves the loading order of a list mods/plugins using their internal names. */
    public OrderedMap<String, ModState> resolveDependencies(Seq<ModMeta> metas){
        var context = new ModResolutionContext();

        for(var meta : metas){
            Seq<ModDependency> dependencies = new Seq<>();
            for(var dependency : meta.dependencies){
                dependencies.add(new ModDependency(dependency, true));
            }
            for(var dependency : meta.softDependencies){
                dependencies.add(new ModDependency(dependency, false));
            }
            context.dependencies.put(meta.name, dependencies);
        }

        for(var key : context.dependencies.keys()){
            if (context.ordered.contains(key)) {
                continue;
            }
            resolve(key, context);
            context.visited.clear();
        }

        var result = new OrderedMap<String, ModState>();
        for(var name : context.ordered){
            result.put(name, ModState.enabled);
        }
        result.putAll(context.invalid);
        return result;
    }

    private boolean resolve(String element, ModResolutionContext context){
        context.visited.add(element);
        for(final var dependency : context.dependencies.get(element)){
            // Circular dependencies ?
            if(context.visited.contains(dependency.name) && !context.ordered.contains(dependency.name)){
                context.invalid.put(dependency.name, ModState.circularDependencies);
                return false;
                // If dependency present, resolve it, or if it's not required, ignore it
            }else if(context.dependencies.containsKey(dependency.name)){
                if(!context.ordered.contains(dependency.name) && !resolve(dependency.name, context) && dependency.required){
                    context.invalid.put(element, ModState.incompleteDependencies);
                    return false;
                }
                // The dependency is missing, but if not required, skip
            }else if(dependency.required){
                context.invalid.put(element, ModState.missingDependencies);
                return false;
            }
        }
        if(!context.ordered.contains(element)){
            context.ordered.add(element);
        }
        return true;
    }

    /** Loads a mod file+meta, but does not add it to the list.
     * Note that directories can be loaded as mods. */
    private LoadedMod loadMod(Fi sourceFile) throws Exception{
        return loadMod(sourceFile, false, true);
    }

    /** Loads a mod file+meta, but does not add it to the list.
     * Note that directories can be loaded as mods. */
    private LoadedMod loadMod(Fi sourceFile, boolean overwrite, boolean initialize) throws Exception{
        Time.mark();

        ZipFi rootZip = null;

        try{
            Fi zip = sourceFile.isDirectory() ? sourceFile : (rootZip = new ZipFi(sourceFile));
            if(zip.list().length == 1 && zip.list()[0].isDirectory()){
                zip = zip.list()[0];
            }

            ModMeta meta = findMeta(zip);

            if(meta == null){
                Log.warn("Mod @ doesn't have a '[mod/plugin].[h]json' file, skipping.", zip);
                throw new ModLoadException("Invalid file: No mod.json found.");
            }

            String camelized = meta.name.replace(" ", "");
            String mainClass = meta.main == null ? camelized.toLowerCase(Locale.ROOT) + "." + camelized + "Mod" : meta.main;
            String baseName = meta.name.toLowerCase(Locale.ROOT).replace(" ", "-");

            var other = mods.find(m -> m.name.equals(baseName));

            if(other != null){
                //steam mods can't really be deleted, they need to be unsubscribed
                if(overwrite && !other.hasSteamID()){
                    //close zip file
                    if(other.root instanceof ZipFi){
                        other.root.delete();
                    }
                    //delete the old mod directory
                    if(other.file.isDirectory()){
                        other.file.deleteDirectory();
                    }else{
                        other.file.delete();
                    }
                    //unload
                    mods.remove(other);
                }else{
                    throw new ModLoadException("A mod with the name '" + baseName + "' is already imported.");
                }
            }

            ClassLoader loader = null;
            Mod mainMod;
            Fi mainFile = zip;

            if(android){
                mainFile = mainFile.child("classes.dex");
            }else{
                String[] path = (mainClass.replace('.', '/') + ".class").split("/");
                for(String str : path){
                    if(!str.isEmpty()){
                        mainFile = mainFile.child(str);
                    }
                }
            }

            //make sure the main class exists before loading it; if it doesn't just don't put it there
            //if the mod is explicitly marked as java, try loading it anyway
            if(
                (mainFile.exists() || meta.java) &&
                !skipModLoading() &&
                Core.settings.getBool("mod-" + baseName + "-enabled", true) &&
                Version.isAtLeast(meta.minGameVersion) &&
                (meta.getMinMajor() >= 136 || headless) &&
                initialize
            ){
                if(ios){
                    throw new ModLoadException("Java class mods are not supported on iOS.");
                }

                loader = platform.loadJar(sourceFile, mainLoader);
                mainLoader.addChild(loader);
                Class<?> main = Class.forName(mainClass, true, loader);

                //detect mods that incorrectly package mindustry in the jar
                if((main.getSuperclass().getName().equals("mindustry.mod.Plugin") || main.getSuperclass().getName().equals("mindustry.mod.Mod")) &&
                    main.getSuperclass().getClassLoader() != Mod.class.getClassLoader()){
                    throw new ModLoadException(
                        "This mod/plugin has loaded Mindustry dependencies from its own class loader. " +
                        "You are incorrectly including Mindustry dependencies in the mod JAR - " +
                        "make sure Mindustry is declared as `compileOnly` in Gradle, and that the JAR is created with `runtimeClasspath`!"
                    );
                }

                metas.put(main, meta);
                mainMod = (Mod)main.getDeclaredConstructor().newInstance();
            }else{
                mainMod = null;
            }

            //all plugins are hidden implicitly
            if(mainMod instanceof Plugin){
                meta.hidden = true;
            }

            //disallow putting a description after the version
            if(meta.version != null){
                int line = meta.version.indexOf('\n');
                if(line != -1){
                    meta.version = meta.version.substring(0, line);
                }
            }

            //skip mod loading if it failed
            if(skipModLoading()){
                Core.settings.put("mod-" + baseName + "-enabled", false);
            }

            if(!headless && Core.settings.getBool("mod-" + baseName + "-enabled", true)){
                Log.info("Loaded mod '@' in @ms", meta.name, Time.elapsed());
            }

            return new LoadedMod(sourceFile, zip, mainMod, loader, meta);
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
        /** Content with initialization code. */
        public ObjectSet<Content> erroredContent = new ObjectSet<>();
        /** Current state of this mod. */
        public ModState state = ModState.enabled;
        /** Icon texture. Should be disposed. */
        public @Nullable Texture iconTexture;
        /** Class loader for JAR mods. Null if the mod isn't loaded or this isn't a jar mod. */
        public @Nullable ClassLoader loader;

        public LoadedMod(Fi file, Fi root, Mod main, ClassLoader loader, ModMeta meta){
            this.root = root;
            this.file = file;
            this.loader = loader;
            this.main = main;
            this.meta = meta;
            this.name = meta.name.toLowerCase(Locale.ROOT).replace(" ", "-");
        }

        /** @return whether this is a java class mod. */
        public boolean isJava(){
            return meta.java || main != null;
        }

        @Nullable
        public String getRepo(){
            return Core.settings.getString("mod-" + name + "-repo", meta.repo);
        }

        public void setRepo(String repo){
            Core.settings.put("mod-" + name + "-repo", repo);
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

        /** @return whether this mod is supported by the game version */
        public boolean isSupported(){
            //no unsupported mods on servers
            if(headless) return true;

            if(isOutdated() || isBlacklisted()) return false;

            return Version.isAtLeast(meta.minGameVersion);
        }

        /** Some mods are known to cause issues with the game; this detects and returns whether a mod is manually blacklisted. */
        public boolean isBlacklisted(){
            return blacklistedMods.contains(name);
        }

        /** @return whether this mod is outdated, e.g. not compatible with v7. */
        public boolean isOutdated(){
            //must be at least 136 to indicate v7 compat
            return getMinMajor() < 136;
        }

        public int getMinMajor(){
            return meta.getMinMajor();
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
        public String name, minGameVersion = "0";
        public @Nullable String displayName, author, description, subtitle, version, main, repo;
        public Seq<String> dependencies = Seq.with();
        public Seq<String> softDependencies = Seq.with();
        /** Hidden mods are only server-side or client-side, and do not support adding new content. */
        public boolean hidden;
        /** If true, this mod should be loaded as a Java class mod. This is technically optional, but highly recommended. */
        public boolean java;
        /** If true, -outline regions for units are kept when packing. Only use if you know exactly what you are doing. */
        public boolean keepOutlines;
        /** To rescale textures with a different size. Represents the size in pixels of the sprite of a 1x1 block. */
        public float texturescale = 1.0f;
        /** If true, bleeding is skipped and no content icons are generated. */
        public boolean pregenerated;

        public String displayName(){
            return displayName == null ? name : displayName;
        }

        public String shortDescription(){
            return Strings.truncate(subtitle == null ? (description == null || description.length() > maxModSubtitleLength ? "" : description) : subtitle, maxModSubtitleLength, "...");
        }

        //removes all colors
        public void cleanup(){
            if(name != null) name = Strings.stripColors(name);
            if(displayName != null) displayName = Strings.stripColors(displayName);
            if(author != null) author = Strings.stripColors(author);
            if(description != null) description = Strings.stripColors(description);
            if(subtitle != null) subtitle = Strings.stripColors(subtitle).replace("\n", "");
        }

        public int getMinMajor(){
            String ver = minGameVersion == null ? "0" : minGameVersion;
            int dot = ver.indexOf(".");
            return dot != -1 ? Strings.parseInt(ver.substring(0, dot), 0) : Strings.parseInt(ver, 0);
        }

        @Override
        public String toString(){
            return "ModMeta{" +
            "name='" + name + '\'' +
            ", minGameVersion='" + minGameVersion + '\'' +
            ", displayName='" + displayName + '\'' +
            ", author='" + author + '\'' +
            ", description='" + description + '\'' +
            ", subtitle='" + subtitle + '\'' +
            ", version='" + version + '\'' +
            ", main='" + main + '\'' +
            ", repo='" + repo + '\'' +
            ", dependencies=" + dependencies +
            ", softDependencies=" + softDependencies +
            ", hidden=" + hidden +
            ", java=" + java +
            ", keepOutlines=" + keepOutlines +
            ", texturescale=" + texturescale +
            ", pregenerated=" + pregenerated +
            '}';
        }
    }

    public static class ModLoadException extends RuntimeException{
        public ModLoadException(String message){
            super(message);
        }
    }

    public enum ModState{
        enabled,
        contentErrors,
        missingDependencies,
        incompleteDependencies,
        circularDependencies,
        unsupported,
        disabled,
    }

    public static class ModResolutionContext {
        public final ObjectMap<String, Seq<ModDependency>> dependencies = new ObjectMap<>();
        public final ObjectSet<String> visited = new ObjectSet<>();
        public final OrderedSet<String> ordered = new OrderedSet<>();
        public final ObjectMap<String, ModState> invalid = new OrderedMap<>();
    }

    public static final class ModDependency{
        public final String name;
        public final boolean required;

        public ModDependency(String name, boolean required){
            this.name = name;
            this.required = required;
        }
    }
}
