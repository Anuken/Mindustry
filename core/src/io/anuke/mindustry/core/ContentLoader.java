package io.anuke.mindustry.core;

import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import static io.anuke.arc.Core.files;
import static io.anuke.mindustry.Vars.mods;

/**
 * Loads all game content.
 * Call load() before doing anything with content.
 */
@SuppressWarnings("unchecked")
public class ContentLoader{
    private boolean loaded = false;
    private ObjectMap<String, MappableContent>[] contentNameMap = new ObjectMap[ContentType.values().length];
    private Array<Content>[] contentMap = new Array[ContentType.values().length];
    private MappableContent[][] temporaryMapper;
    private ObjectSet<Consumer<Content>> initialization = new ObjectSet<>();
    private ContentList[] content = {
        new Fx(),
        new Items(),
        new StatusEffects(),
        new Liquids(),
        new Bullets(),
        new Mechs(),
        new UnitTypes(),
        new Blocks(),
        new Loadouts(),
        new TechTree(),
        new Zones(),
        new TypeIDs(),

        //these are not really content classes, but this makes initialization easier
        new LegacyColorMapper(),
    };

    /** Clears all initialized content.*/
    public void clear(){
        contentNameMap = new ObjectMap[ContentType.values().length];
        contentMap = new Array[ContentType.values().length];
        initialization = new ObjectSet<>();
        loaded = false;
    }

    /** Creates all content types. */
    public void createContent(){
        if(loaded){
            Log.info("Content already loaded, skipping.");
            return;
        }

        for(ContentType type : ContentType.values()){
            contentMap[type.ordinal()] = new Array<>();
            contentNameMap[type.ordinal()] = new ObjectMap<>();
        }

        for(ContentList list : content){
            list.load();
        }

        if(mods != null){
            mods.loadContent();
        }

        //check up ID mapping, make sure it's linear
        for(Array<Content> arr : contentMap){
            for(int i = 0; i < arr.size; i++){
                int id = arr.get(i).id;
                if(id != i){
                    throw new IllegalArgumentException("Out-of-order IDs for content '" + arr.get(i) + "' (expected " + i + " but got " + id + ")");
                }
            }
        }

        loaded = true;
    }

    /** Logs content statistics.*/
    public void logContent(){
        Log.info("--- CONTENT INFO ---");
        for(int k = 0; k < contentMap.length; k++){
            Log.info("[{0}]: loaded {1}", ContentType.values()[k].name(), contentMap[k].size);
        }
        Log.info("Total content loaded: {0}", Array.with(ContentType.values()).mapInt(c -> contentMap[c.ordinal()].size).sum());
        Log.info("-------------------");
    }

    /** Calls Content#init() on everything. Use only after all modules have been created.*/
    public void init(){
        initialize(Content::init);
    }

    /** Calls Content#load() on everything. Use only after all modules have been created on the client.*/
    public void load(){
        initialize(Content::load);
    }

    /** Initializes all content with the specified function. */
    private void initialize(Consumer<Content> callable){
        if(initialization.contains(callable)) return;

        for(ContentType type : ContentType.values()){
            for(Content content : contentMap[type.ordinal()]){
                //TODO catch error and display it per mod
                callable.accept(content);
            }
        }

        initialization.add(callable);
    }

    /** Loads block colors. */
    public void loadColors(){
        Pixmap pixmap = new Pixmap(files.internal("sprites/block_colors.png"));
        for(int i = 0; i < pixmap.getWidth(); i++){
            if(blocks().size > i){
                int color = pixmap.getPixel(i, 0);

                if(color == 0) continue;

                Block block = block(i);
                Color.rgba8888ToColor(block.color, color);
            }
        }
        pixmap.dispose();
    }

    public void dispose(){
        //clear all content, currently not used
    }

    public void handleContent(Content content){
        contentMap[content.getContentType().ordinal()].add(content);

    }

    public void handleMappableContent(MappableContent content){
        if(contentNameMap[content.getContentType().ordinal()].containsKey(content.name)){
            throw new IllegalArgumentException("Two content objects cannot have the same name! (issue: '" + content.name + "')");
        }
        contentNameMap[content.getContentType().ordinal()].put(content.name, content);
    }

    public void setTemporaryMapper(MappableContent[][] temporaryMapper){
        this.temporaryMapper = temporaryMapper;
    }

    public Array<Content>[] getContentMap(){
        return contentMap;
    }

    public <T extends MappableContent> T getByName(ContentType type, String name){
        if(contentNameMap[type.ordinal()] == null){
            return null;
        }
        return (T)contentNameMap[type.ordinal()].get(name);
    }

    public <T extends Content> T getByID(ContentType type, int id){

        if(temporaryMapper != null && temporaryMapper[type.ordinal()] != null && temporaryMapper[type.ordinal()].length != 0){
            //-1 = invalid content
            if(id < 0){
                return null;
            }
            if(temporaryMapper[type.ordinal()].length <= id || temporaryMapper[type.ordinal()][id] == null){
                return (T)contentMap[type.ordinal()].get(0); //default value is always ID 0
            }
            return (T)temporaryMapper[type.ordinal()][id];
        }

        if(id >= contentMap[type.ordinal()].size || id < 0){
            return null;
        }
        return (T)contentMap[type.ordinal()].get(id);
    }

    public <T extends Content> Array<T> getBy(ContentType type){
        return (Array<T>)contentMap[type.ordinal()];
    }

    //utility methods, just makes things a bit shorter

    public Array<Block> blocks(){
        return getBy(ContentType.block);
    }

    public Block block(int id){
        return (Block)getByID(ContentType.block, id);
    }

    public Array<Item> items(){
        return getBy(ContentType.item);
    }

    public Item item(int id){
        return (Item)getByID(ContentType.item, id);
    }

    public Array<Liquid> liquids(){
        return getBy(ContentType.liquid);
    }

    public Liquid liquid(int id){
        return (Liquid)getByID(ContentType.liquid, id);
    }

    public Array<BulletType> bullets(){
        return getBy(ContentType.bullet);
    }

    public BulletType bullet(int id){
        return (BulletType)getByID(ContentType.bullet, id);
    }

    public Array<Zone> zones(){
        return getBy(ContentType.zone);
    }

    public Array<UnitType> units(){
        return getBy(ContentType.unit);
    }
}
