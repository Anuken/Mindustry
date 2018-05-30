package io.anuke.mindustry.core;

import io.anuke.mindustry.content.*;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.*;
import io.anuke.mindustry.entities.StatusEffect;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.util.Log;

/**Loads all game content.
 * Call load() before doing anything with content.*/
public class ContentLoader {

    public static void load(){

        ContentList[] content = {
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

            //liquids
            new Liquids(),

            //status effects
            new StatusEffects(),

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

            //units
            new UnitTypes(),
        };

        for (ContentList list : content){
            list.load();
        }

        for(Block block : Block.getAllBlocks()){
            block.init();
        }

        Log.info("--- CONTENT INFO ---");
        Log.info("Blocks loaded: {0}\nItems loaded: {1}\nLiquids loaded: {2}\nUpgrades loaded: {3}\nUnits loaded: {4}\nAmmo types loaded: {5}\nBullet types loaded: {6}\nStatus effects loaded: {7}\nRecipes loaded: {8}\nTotal content classes: {9}",
                Block.getAllBlocks().size, io.anuke.mindustry.type.Item.all().size, Liquid.all().size,
                io.anuke.mindustry.type.Mech.all().size, UnitType.getAllTypes().size, io.anuke.mindustry.type.AmmoType.all().size, BulletType.all().size, StatusEffect.getAllEffects().size, io.anuke.mindustry.type.Recipe.all().size, content.length);

        Log.info("-------------------");
    }
}
