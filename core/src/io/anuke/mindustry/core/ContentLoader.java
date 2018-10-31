package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.*;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.MappableContent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.LegacyColorMapper;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.ThreadArray;

/**
 * Loads all game content.
 * Call load() before doing anything with content.
 */
@SuppressWarnings("unchecked")
public class ContentLoader{
    private boolean loaded = false;
    private boolean verbose = true;

    private ObjectMap<String, MappableContent>[] contentNameMap = new ObjectMap[ContentType.values().length];
    private Array<Content>[] contentMap = new Array[ContentType.values().length];
    private MappableContent[][] temporaryMapper;
    private ObjectSet<Consumer<Content>> initialization = new ObjectSet<>();
    private ContentList[] content = {
        //effects
        new BlockFx(),
        new BulletFx(),
        new EnvironmentFx(),
        new ExplosionFx(),
        new Fx(),
        new ShootFx(),
        new UnitFx(),

        //items
        new Items(),

        //status effects
        new StatusEffects(),

        //liquids
        new Liquids(),

        //bullets
        new ArtilleryBullets(),
        new FlakBullets(),
        new MissileBullets(),
        new StandardBullets(),
        new TurretBullets(),
        new WeaponBullets(),


        //ammotypes
        new AmmoTypes(),

        //weapons
        new Weapons(),

        //mechs
        new Mechs(),

        //units
        new UnitTypes(),

        //blocks
        new Blocks(),
        new DefenseBlocks(),
        new DistributionBlocks(),
        new ProductionBlocks(),
        new TurretBlocks(),
        new DebugBlocks(),
        new LiquidBlocks(),
        new StorageBlocks(),
        new UnitBlocks(),
        new PowerBlocks(),
        new CraftingBlocks(),
        new UpgradeBlocks(),
        new OreBlocks(),

        //not really a content class, but this makes initialization easier
        new ColorMapper(),
        new LegacyColorMapper(),

        //recipes
        new Recipes(),
    };

    /**Creates all content types.*/
    public void load(){
        if(loaded){
            Log.info("Content already loaded, skipping.");
            return;
        }

        registerTypes();

        for(ContentType type : ContentType.values()){
            contentMap[type.ordinal()] = new ThreadArray<>();
            contentNameMap[type.ordinal()] =  new ObjectMap<>();
        }

        for(ContentList list : content){
            list.load();
        }

        int total = 0;

        for(ContentType type : ContentType.values()){

            for(Content c : contentMap[type.ordinal()]){
                if(c instanceof MappableContent){
                    String name = ((MappableContent) c).getContentName();
                    if(contentNameMap[type.ordinal()].containsKey(name)){
                        throw new IllegalArgumentException("Two content objects cannot have the same name! (issue: '" + name + "')");
                    }
                    contentNameMap[type.ordinal()].put(name, (MappableContent) c);
                }
                total ++;
            }
        }

        //set up ID mapping
        for(Array<Content> arr : contentMap){
            for(int i = 0; i < arr.size; i++){
                int id = arr.get(i).id;
                if(id < 0) id += 256;
                if(id != i){
                    throw new IllegalArgumentException("Out-of-order IDs for content '" + arr.get(i) + "' (expected " + i + " but got " + id + ")");
                }
            }
        }

        if(blocks().size >= 256){
            throw new ImpendingDoomException("THE TIME HAS COME. More than 256 blocks have been created.");
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

    /**Initializes all content with the specified function.*/
    public void initialize(Consumer<Content> callable){
        if(initialization.contains(callable)) return;

        for(ContentType type : ContentType.values()){
            for(Content content : contentMap[type.ordinal()]){
                callable.accept(content);
            }
        }

        initialization.add(callable);
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
        //offset negative values by 256, as they are probably a product of byte overflow
        if(id < 0) id += 256;

        if(temporaryMapper != null && temporaryMapper[type.ordinal()] != null && temporaryMapper[type.ordinal()].length != 0){
            if(temporaryMapper[type.ordinal()].length <= id || temporaryMapper[type.ordinal()][id] == null){
                return getByID(type, 0); //default value is always ID 0
            }
            return (T)temporaryMapper[type.ordinal()][id];
        }

        if(id >= contentMap[type.ordinal()].size || id < 0){
            throw new RuntimeException("No " + type.name() + " with ID '" + id + "' found!");
        }
        return (T)contentMap[type.ordinal()].get(id);
    }

    public <T extends Content> Array<T> getBy(ContentType type){
        return (Array<T>) contentMap[type.ordinal()];
    }

    //utility methods, just makes things a bit shorter

    public Array<Block> blocks(){
        return getBy(ContentType.block);
    }

    public Block block(int id){
        return (Block) getByID(ContentType.block, id);
    }

    public Array<Recipe> recipes(){
        return getBy(ContentType.recipe);
    }

    public Recipe recipe(int id){
        return (Recipe) getByID(ContentType.recipe, id);
    }

    public Array<Item> items(){
        return getBy(ContentType.item);
    }

    public Item item(int id){
        return (Item) getByID(ContentType.item, id);
    }

    public Array<Liquid> liquids(){
        return getBy(ContentType.liquid);
    }

    public Liquid liquid(int id){
        return (Liquid) getByID(ContentType.liquid, id);
    }

    public Array<BulletType> bullets(){
        return getBy(ContentType.bullet);
    }

    public BulletType bullet(int id){
        return (BulletType) getByID(ContentType.bullet, id);
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

    private class ImpendingDoomException extends RuntimeException{
        public ImpendingDoomException(String s){ super(s); }
    }
}
