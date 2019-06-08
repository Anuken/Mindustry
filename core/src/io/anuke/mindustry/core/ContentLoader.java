package io.anuke.mindustry.core;

import io.anuke.arc.collection.*;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.LegacyColorMapper;

import static io.anuke.arc.Core.files;

/**
 * Loads all game content.
 * Call load() before doing anything with content.
 */
@SuppressWarnings("unchecked")
public class ContentLoader{
    private boolean loaded = false;
    private boolean verbose = false;

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

        //these are not really content classes, but this makes initialization easier
        new LegacyColorMapper(),
    };

    public void setVerbose(){
        verbose = true;
    }

    /** Creates all content types. */
    public void load(){
        if(loaded){
            Log.info("Content already loaded, skipping.");
            return;
        }

        registerTypes();

        for(ContentType type : ContentType.values()){
            contentMap[type.ordinal()] = new Array<>();
            contentNameMap[type.ordinal()] = new ObjectMap<>();
        }

        for(ContentList list : content){
            list.load();
        }

        int total = 0;

        for(ContentType type : ContentType.values()){

            for(Content c : contentMap[type.ordinal()]){
                if(c instanceof MappableContent){
                    String name = ((MappableContent)c).name;
                    if(contentNameMap[type.ordinal()].containsKey(name)){
                        throw new IllegalArgumentException("Two content objects cannot have the same name! (issue: '" + name + "')");
                    }
                    contentNameMap[type.ordinal()].put(name, (MappableContent)c);
                }
                total++;
            }
        }

        //set up ID mapping
        for(Array<Content> arr : contentMap){
            for(int i = 0; i < arr.size; i++){
                int id = arr.get(i).id;
                if(id != i){
                    throw new IllegalArgumentException("Out-of-order IDs for content '" + arr.get(i) + "' (expected " + i + " but got " + id + ")");
                }
            }
        }

        if(verbose){
            Log.info("--- CONTENT INFO ---");
            for(int k = 0; k < contentMap.length; k++){
                Log.info("[{0}]: loaded {1}", ContentType.values()[k].name(), contentMap[k].size);
            }
            Log.info("Total content loaded: {0}", total);
            Log.info("-------------------");
        }

        loaded = true;
    }

    public void initialize(Consumer<Content> callable){
        initialize(callable, false);
    }

    /** Initializes all content with the specified function. */
    public void initialize(Consumer<Content> callable, boolean override){
        if(initialization.contains(callable) && !override) return;

        for(ContentType type : ContentType.values()){
            for(Content content : contentMap[type.ordinal()]){
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

    public void verbose(boolean verbose){
        this.verbose = verbose;
    }

    public void dispose(){
        //clear all content, currently not needed
    }

    public void handleContent(Content content){
        contentMap[content.getContentType().ordinal()].add(content);
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
                return getByID(type, 0); //default value is always ID 0
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

    /**
     * Registers sync IDs for all types of sync entities.
     * Do not register units here!
     */
    private void registerTypes(){
        TypeTrait.registerType(Player.class, Player::new);
        TypeTrait.registerType(Fire.class, Fire::new);
        TypeTrait.registerType(Puddle.class, Puddle::new);
        TypeTrait.registerType(Bullet.class, Bullet::new);
        TypeTrait.registerType(Lightning.class, Lightning::new);
    }
}
