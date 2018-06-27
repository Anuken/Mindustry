package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.OrderedSet;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.*;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Log;

/**Loads all game content.
 * Call load() before doing anything with content.*/
public class ContentLoader {
    private static boolean loaded = false;
    private static ObjectSet<Array<? extends Content>> contentSet = new OrderedSet<>();
    private static ContentList[] content = {
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

        //recipes
        new Recipes(),
    };

    /**Creates all content types.*/
    public static void load(){
        if(loaded){
            Log.info("Content already loaded, skipping.");
            return;
        }

        registerTypes();

        for (ContentList list : content){
            list.load();
        }

        for (ContentList list : content){
            contentSet.add(list.getAll());
        }

        if(Block.all().size >= 256){
            throw new IllegalArgumentException("THE TIME HAS COME. More than 256 blocks have been created.");
        }

        Log.info("--- CONTENT INFO ---");
        Log.info("Blocks loaded: {0}\nItems loaded: {1}\nLiquids loaded: {2}\nUpgrades loaded: {3}\nUnits loaded: {4}\nAmmo types loaded: {5}\nBullet types loaded: {6}\nStatus effects loaded: {7}\nRecipes loaded: {8}\nEffects loaded: {9}\nTotal content classes: {10}",
                Block.all().size, Item.all().size, Liquid.all().size, Mech.all().size, UnitType.all().size,
                AmmoType.all().size, BulletType.all().size, StatusEffect.all().size, Recipe.all().size, Effects.all().size, content.length);

        Log.info("-------------------");

        loaded = true;
    }

    /**Initializes all content with the specified function.*/
    public static void initialize(Consumer<Content> callable){
        for(Array<? extends Content> arr : contentSet){
            for(Content content : arr){
                callable.accept(content);
            }
        }
    }

    public static void dispose(){
        //TODO clear all content.
    }

    /**Registers sync IDs for all types of sync entities.
     * Do not register units here!*/
    private static void registerTypes(){
        TypeTrait.registerType(Player.class, Player::new);
        TypeTrait.registerType(ItemDrop.class, ItemDrop::new);
        TypeTrait.registerType(Fire.class, Fire::new);
        TypeTrait.registerType(Puddle.class, Puddle::new);
        TypeTrait.registerType(Bullet.class, Bullet::new);
    }
}
