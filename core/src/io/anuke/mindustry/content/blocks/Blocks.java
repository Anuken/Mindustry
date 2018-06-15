package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.graphics.CacheLayer;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.*;

public class Blocks extends BlockList implements ContentList{
    public static Block air, spawn, blockpart, space, metalfloor, deepwater, water, lava, oil, stone, blackstone, iron, lead, coal, titanium, thorium, dirt, sand, ice, snow, grass, shrub, rock, icerock, blackrock;

    @Override
    public void load() {
        air = new Floor("air") {
            {
                blend = false;
            }
            //don't draw
            public void draw(Tile tile) {}
            public void load() {}
            public void init() {}
        };

        blockpart = new BlockPart();

        for(int i = 1; i <= 6; i ++){
            new BuildBlock("build" + i);
            new BreakBlock("break" + i);
        }

        space = new Floor("space") {{
            placeableOn = false;
            variants = 0;
            cacheLayer = CacheLayer.space;
            solid = true;
            blend = false;
        }};

        metalfloor = new Floor("metalfloor") {{
            variants = 6;
        }};

        deepwater = new Floor("deepwater") {{
            placeableOn = false;
            liquidColor = Color.valueOf("546bb3");
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.water;
            isLiquid = true;
            status = StatusEffects.wet;
            statusIntensity = 1f;
            drownTime = 140f;
            cacheLayer = CacheLayer.water;
        }};

        water = new Floor("water") {{
            placeableOn = false;
            liquidColor = Color.valueOf("546bb3");
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusIntensity = 0.9f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
        }};

        lava = new Floor("lava") {{
            placeableOn = false;
            liquidColor = Color.valueOf("ed5334");
            speedMultiplier = 0.2f;
            damageTaken = 0.1f;
            status = StatusEffects.melting;
            statusIntensity = 0.8f;
            variants = 0;
            liquidDrop = Liquids.lava;
            isLiquid = true;
            cacheLayer = CacheLayer.lava;
        }};

        oil = new Floor("oil") {{
            placeableOn = false;
            liquidColor = Color.valueOf("292929");
            status = StatusEffects.oiled;
            statusIntensity = 1f;
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.oil;
            isLiquid = true;
            cacheLayer = CacheLayer.oil;
        }};

        stone = new Floor("stone") {{
            drops = new ItemStack(Items.stone, 1);
            blends = block -> block != this && !(block instanceof Ore);
        }};

        blackstone = new Floor("blackstone") {{
            drops = new ItemStack(Items.stone, 1);
        }};

        iron = new Ore("iron") {{
            drops = new ItemStack(Items.iron, 1);
        }};

        lead = new Ore("lead") {{
            drops = new ItemStack(Items.lead, 1);
        }};

        coal = new Ore("coal") {{
            drops = new ItemStack(Items.coal, 1);
        }};

        titanium = new Ore("titanium") {{
            drops = new ItemStack(Items.titanium, 1);
        }};

        thorium = new Ore("thorium") {{
            drops = new ItemStack(Items.thorium, 1);
        }};

        dirt = new Floor("dirt");

        sand = new Floor("sand") {{
            drops = new ItemStack(Items.sand, 1);
        }};

        ice = new Floor("ice") {{
            dragMultiplier = 0.2f;
        }};

        snow = new Floor("snow");

        grass = new Floor("grass");

        shrub = new Rock("shrub");

        rock = new Rock("rock") {{
            variants = 2;
            varyShadow = true;
        }};

        icerock = new Rock("icerock") {{
            variants = 2;
            varyShadow = true;
        }};

        blackrock = new Rock("blackrock") {{
            variants = 1;
            varyShadow = true;
        }};
    }
}
