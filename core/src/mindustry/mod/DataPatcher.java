package mindustry.mod;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.logic.*;
import mindustry.mod.Mods.*;
import mindustry.mod.data.*;
import mindustry.mod.patch.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

public class DataPatcher{
    private static ModMeta dpModMeta = new ModMeta(){{
        name = internalName = "dp";
    }};

    public static final LoadedMod dpMod = new LoadedMod(new Fi("dp"), new Fi(""), null, null, dpModMeta);

    public static final int maxImageSize = 2000;
    public static final int patchFormatVersion = 2;

    private static boolean needsArrayFix = false;
    private static DataPatcher currentDataPatcher;
    private static ContentParser parser = createParser();

    private boolean applied;
    private ContentLoader contentLoader;
    private final PatchContext context = new PatchContext(parser, text -> addWarning(null, text));
    private final RootNode root = new RootNode(context);
    private Seq<Content> addedContent = new Seq<>();
    private @Nullable PatchAsset currentlyApplyingPatch;
    private @Nullable ContentAsset currentlyApplyingContent;
    private Seq<LogicVar> addedVars = new Seq<>();

    static ContentParser createParser(){
        ContentParser cont = new ContentParser(){
            @Override
            void warnContext(@Nullable Content currentContent, @Nullable Fi currentFile, String string, Object... format){
                //forward warnings to the current patcher - this is a bit hacky, but I do not want to re-initialize the parser every time
                if(currentDataPatcher!= null){
                    currentDataPatcher.warnContext(currentContent, currentFile, string, format);
                }
            }
        };
        cont.allowAssetLoading = false;
        cont.allowPatching = false;

        return cont;
    }

    public boolean isPatched(Object object){
        return context.isPatched(object);
    }

    /** Applies the specified patches. If patches were already applied, the previous ones are un-applied - they do not stack! */
    public void apply(Seq<PatchAsset> patches, Seq<ContentAsset> content){
        apply(patches, content, true);
    }

    /** Applies the specified patches. If patches were already applied, the previous ones are un-applied - they do not stack! */
    public void apply(Seq<PatchAsset> patches, Seq<ContentAsset> content, boolean reloadContentWorld){
        //if you're un-applying data patches, and it throws an error, just crash. this is not recoverable.
        if(applied){
            unapply(reloadContentWorld);
            applied = false;
        }

        if(patches.isEmpty() && content.isEmpty()) return;

        currentDataPatcher = this;
        applied = true;
        contentLoader = Vars.content.copy();

        Attribute[] oldAttributes = Attribute.all.clone();
        var oldAttributeMap = Attribute.map.copy();
        context.reset(() -> {
            Attribute.all = oldAttributes;
            Attribute.map = oldAttributeMap;
        });

        if(!content.isEmpty()){
            content.sort();

            dpMod.erroredContent.clear();

            for(var asset : content){
                asset.errored = false;
                asset.content = null;
                asset.warnings.clear();

                currentlyApplyingContent = asset;

                if(!Structs.contains(ContentAsset.loadableContent, asset.type)){
                    warn("Content @ is of type '@', which is not supported. Skipping.", asset.path, asset.type);
                    continue;
                }

                Content current = Vars.content.getLastAdded();
                Fi file = new Fi(asset.path);

                //this is very important for resizing various arrays used in the game
                //checking for blocks is also important, as those can be added/removed, and corresponding blocks need to be updated
                if(asset.type == ContentType.item || asset.type == ContentType.liquid || asset.type == ContentType.block){
                    needsArrayFix = true;
                }

                try{
                    //this binds the content but does not load it entirely
                    asset.content = parser.parse(dpMod, asset.name, asset.data, file, asset.type);
                    asset.content.minfo.asset = asset;
                }catch(Throwable e){
                    asset.warnings.add(Strings.getFinalMessage(e));
                    asset.errored = true;

                    var lastAdded = Vars.content.getLastAdded();
                    if(current != lastAdded && lastAdded != null){
                        Vars.content.remove(lastAdded);
                        //markError should log it already
                        parser.markError(lastAdded, dpMod, file, e);
                    }else{
                        Log.err("Error loading content: " + asset.path, e);
                    }
                }
            }

            currentlyApplyingContent = null;

            parser.finishParsing();

            addedContent.clear();
            Seq<Content> all = addedContent;

            for(var arr : Vars.content.getContentMap()){
                all.addAll(arr.select(c -> c.minfo.mod == dpMod));
            }

            for(var errored : dpMod.erroredContent){
                if(errored.minfo.error != null && errored.minfo.asset != null){
                    errored.minfo.asset.warnings.add(errored.minfo.error);
                }
                Vars.content.remove(errored);
            }

            for(var cont : all){
                try{
                    cont.init();
                }catch(Throwable t){
                    Vars.content.remove(cont);
                    if(cont.minfo.asset != null) cont.minfo.asset.errored = true;
                    parser.markError(cont, dpMod, cont.minfo.sourceFile, t);
                }
            }

            for(var cont : all){
                try{
                    cont.postInit();
                }catch(Throwable t){
                    Vars.content.remove(cont);
                    if(cont.minfo.asset != null) cont.minfo.asset.errored = true;
                    parser.markError(cont, dpMod, cont.minfo.sourceFile, t);
                }
            }

            //register global variables
            for(var cont : all){
                if(!cont.hasErrored() && cont instanceof UnlockableContent u && Vars.logicVars.get("@" + (u instanceof StatusEffect ? "status-" : "") + u.name) == null){
                    addedVars.add(Vars.logicVars.put("@" + (u instanceof StatusEffect ? "status-" : "") + u.name, u, false));
                }
            }

            if(!Vars.headless){
                for(var cont : all){
                    try{
                        cont.load();
                        if(cont.minfo.asset != null && cont instanceof UnlockableContent u){
                            if(!u.uiIcon.found() && u.getContentType() != ContentType.planet && u.getContentType() != ContentType.weather){
                                cont.minfo.asset.warnings.add("[" + u.name.substring(u.minfo.mod.name.length() + 1) + "] Could not find an icon. Ensure that you have an image named '" + u.name + "' loaded. Remember that imported images always have the 'dp-' prefix automatically applied.");
                            }
                        }
                    }catch(Throwable t){
                        //not removed here, as this code is only called clientside, and removing it would cause a desync
                        if(cont.minfo.asset != null) cont.minfo.asset.errored = true;
                        parser.markError(cont, dpMod, cont.minfo.sourceFile, t);
                    }
                }
            }

            if(reloadContentWorld) fixContentArrays();
        }

        var patchIt = patches.iterator();
        while(patchIt.hasNext()){
            PatchAsset set = patchIt.next();

            set.warnings.clear();
            set.error = false;

            try{
                Object someValue = parser.getJson().fromJson(null, Jval.read(set.patch).toString(Jformat.plain));
                if(!(someValue instanceof Jval value)) throw new SerializationException("Patch must be a JSON object.");

                if(Vars.state.rules.planet != null && value.has("requiredPlanets")){
                    Jval req = value.get("requiredPlanets");
                    value.remove("requiredPlanets");

                    //this should be ignored unless this instance is a dedicated server
                    if(Vars.headless){
                        String[] planets = req.isArray() ? req.asStringArray() : new String[]{req.asString()};
                        if(!Structs.contains(planets, Vars.state.rules.planet.name)){
                            patchIt.remove();
                            continue;
                        }
                    }
                }

                set.json = value;
                currentlyApplyingPatch = set;

                set.name = value.getString("name", "");
                value.remove("name"); //patchsets can have a name, ignore it if present
                root.assignAll(value);
                currentlyApplyingPatch = null;

            }catch(Exception e){
                set.error = true;
                set.name = "";
                set.warnings.add(Strings.getSimpleMessage(e));
                currentlyApplyingPatch = null;

                Log.err("Failed to apply patch: " + set.patch, e);
            }
        }

        context.finish();
    }

    public void unapply(){
        unapply(true);
    }

    public void unapply(boolean reloadContentWorld){
        if(!applied) return;

        callContentRemove();
        Vars.content = contentLoader;
        applied = false;

        for(var lvar : addedVars){
            Vars.logicVars.remove(lvar);
        }
        context.revert();

        addedContent.clear();

        if(reloadContentWorld) fixContentArrays();
    }

    public Seq<Content> getAddedContent(){
        return addedContent;
    }

    void callContentRemove(){
        //anything not present in the snapshot was added by this patcher, including nested content (e.g. bullets) in patches
        for(var type : ContentType.all){
            int size = contentLoader.getBy(type).size;
            for(Content value : Vars.content.getBy(type)){
                if(value.id >= size || value.minfo.mod == dpMod){
                    value.removed = true;
                    value.removeContent();
                }
            }
        }
    }

    public static void fixContentArrays(){
        fixContentArrays(false);
    }

    public static void fixContentArrays(boolean force){
        if(!needsArrayFix && !force) return;
        int items = Vars.content.items().size, liquids = Vars.content.liquids().size;
        ItemModule.empty.checkArrayCapacity(items);

        //block item/liquid filter
        for(var block : Vars.content.blocks()){
            //don't waste time resizing arrays for blocks that can't use them
            if(!block.synthetic()) continue;
            if(block.lastConfig instanceof Content c && c.removed){
                block.lastConfig = null;
            }

            block.checkContentArrayCapacity(items, liquids);
        }

        //resize capacities in the world (editor). this SHOULD be the only time when fixing arrays is necessary
        if(!Vars.headless && Vars.ui != null && Vars.ui.editor != null && Vars.ui.editor.isShown()){
            int wh = Vars.state.world.width * Vars.state.world.height;
            for(int i = 0; i < wh; i++){
                Tile tile = Vars.state.world.geti(i);

                //stale checks for floor/overlay
                if(tile.floor().removed) tile.setFloor(getReplacementBlock(tile.floor()).asFloor());
                if(tile.overlay().removed) tile.setOverlay(getReplacementBlock(tile.overlay()).asFloor());

                if(tile.block().removed){
                    Block mapped = getReplacementBlock(tile.block());
                    //tile refers to stale content; get rid of it.
                    if(mapped == Blocks.air){
                        tile.remove();
                    }else{
                        //update internal reference of block to point to the new one with correct ID
                        tile.updateBlockReference(mapped);
                    }
                }

                var b = tile.build;
                if(b == null || !tile.isCenter()) continue;
                if(b.items != null) b.items.checkArrayCapacity(items);
                if(b.liquids != null) b.liquids.checkArrayCapacity(items);
            }
        }

        needsArrayFix = false;
    }

    private static Block getReplacementBlock(Block existing){
        Block other = Vars.content.block(existing.name);
        if(other == null) return Blocks.air;
        //make sure they are type compatible
        if(other.getClass() == existing.getClass()){
            return other;
        }
        //could not find an equivalent, clear it
        return Blocks.air;
    }

    void warn(String error, Object... fmt){
        warnContext(null, null, error, fmt);
    }

    void warnContext(@Nullable Content currentContent, @Nullable Fi currentFile, String error, Object... fmt){
        addWarning(currentContent, Strings.format(error, fmt));
    }

    void addWarning(@Nullable Content currentContent, String formatted){
        if(currentlyApplyingPatch != null){
            currentlyApplyingPatch.warnings.add(formatted);
        }else if(currentlyApplyingContent != null && (currentlyApplyingContent.content == null || currentlyApplyingContent.content.minfo.asset == null)){
            currentlyApplyingContent.warnings.add(formatted);
        }else if(currentContent != null && currentContent.minfo.asset != null){
            currentContent.minfo.asset.warnings.add(formatted);
        }

        Log.warn("[ContentPatcher] " + formatted);
    }
}
