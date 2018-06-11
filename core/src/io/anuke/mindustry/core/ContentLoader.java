package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.OrderedSet;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.*;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.entities.units.types.Drone;
import io.anuke.mindustry.entities.units.types.Scout;
import io.anuke.mindustry.entities.units.types.Vtol;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Block;
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
        new ShellBullets(),
        new StandardBullets(),
        new TurretBullets(),

        //ammotypes
        new AmmoTypes(),

        //mechs
        new Mechs(),

        //weapons
        new Weapons(),

        //units
        new UnitTypes(),

        //blocks
        new Blocks(),
        new DefenseBlocks(),
        new DistributionBlocks(),
        new ProductionBlocks(),
        new WeaponBlocks(),
        new DebugBlocks(),
        new LiquidBlocks(),
        new StorageBlocks(),
        new UnitBlocks(),
        new PowerBlocks(),
        new CraftingBlocks(),

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

        Log.info("--- CONTENT INFO ---");
        Log.info("Blocks loaded: {0}\nItems loaded: {1}\nLiquids loaded: {2}\nUpgrades loaded: {3}\nUnits loaded: {4}\nAmmo types loaded: {5}\nBullet types loaded: {6}\nStatus effects loaded: {7}\nRecipes loaded: {8}\nEffects loaded: {9}\nTotal content classes: {10}",
                Block.all().size, io.anuke.mindustry.type.Item.all().size, Liquid.all().size,
                io.anuke.mindustry.type.Mech.all().size, UnitType.getAllTypes().size, io.anuke.mindustry.type.AmmoType.all().size, BulletType.all().size, StatusEffect.all().size, io.anuke.mindustry.type.Recipe.all().size, Effects.all().size, content.length);

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

    /**Registers sync IDs for all types of sync entities.*/
    private static void registerTypes(){
        Player.typeID = TypeTrait.registerType(Player::new);
        Drone.typeID = TypeTrait.registerType(Drone::new);
        Vtol.typeID = TypeTrait.registerType(Vtol::new);
        Scout.typeID = TypeTrait.registerType(Scout::new);
        ItemDrop.typeID = TypeTrait.registerType(ItemDrop::new);
        Fire.typeID = TypeTrait.registerType(Fire::new);
        Puddle.typeID = TypeTrait.registerType(Puddle::new);
    }
}
