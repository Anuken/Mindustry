package io.anuke.mindustry.core;

import io.anuke.mindustry.content.*;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.entities.StatusEffect;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.resource.Mech;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.util.Log;

/**Loads all game content by creating class instances.
 * Call load() before doing anything with content.*/
public class ContentLoader {

    public static void load(){

        Object[] content = {
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

            //items
            new Items(),

            //liquids
            new Liquids(),

            //mechs
            new Mechs(),

            //weapons
            new Weapons(),

            //units
            new UnitTypes(),

            //ammotypes
            new AmmoTypes(),

            //status effects
            new StatusEffects(),
        };

        for(Block block : Block.getAllBlocks()){
            block.init();
        }

        //TODO 128 blocks!

        Log.info("--- CONTENT INFO ---");
        Log.info("Blocks loaded: {0}\nItems loaded: {1}\nLiquids loaded: {2}\nUpgrades loaded: {3}\nUnits loaded: {4}\nAmmo types loaded: {5}\nStatus effects loaded: {6}\nTotal content classes: {7}",
                Block.getAllBlocks().size, Item.getAllItems().size, Liquid.getAllLiquids().size,
                Mech.getAllUpgrades().size, UnitType.getAllTypes().size, AmmoType.getAllTypes().size, StatusEffect.getAllEffects().size, content.length);

        Log.info("-------------------");
    }
}
