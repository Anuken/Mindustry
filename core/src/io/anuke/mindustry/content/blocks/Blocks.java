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
    public static Block air, spawn, blockpart, build1, build2, build3, build4, build5, build6, defaultFloor, space, metalfloor, deepwater, water, lava, oil, stone, blackstone, iron, lead, coal, titanium, thorium, dirt, sand, ice, snow, grass, sandblock, snowblock, stoneblock, blackstoneblock, grassblock, mossblock, shrub, rock, icerock, blackrock, dirtblock;

    @Override
    public void load() {
        air = new Floor("air") {
            //don't draw
            public void draw(Tile tile) {}
        };

        blockpart = new BlockPart();

        build1 = new BuildBlock("build1");

        build2 = new BuildBlock("build2");

        build3 = new BuildBlock("build3");

        build4 = new BuildBlock("build4");

        build5 = new BuildBlock("build5");

        build6 = new BuildBlock("build6");

        defaultFloor = new Floor("defaultfloor");

        space = new Floor("space") {{
            placeableOn = false;
            variants = 0;
            cacheLayer = CacheLayer.space;
            solid = true;
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
            liquid = true;
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
            liquid = true;
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
            liquid = true;
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
            liquid = true;
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

        sandblock = new StaticBlock("sandblock") {{
            solid = true;
            variants = 3;
        }};

        snowblock = new StaticBlock("snowblock") {{
            solid = true;
            variants = 3;
        }};

        stoneblock = new StaticBlock("stoneblock") {{
            solid = true;
            variants = 3;
        }};

        blackstoneblock = new StaticBlock("blackstoneblock") {{
            solid = true;
            variants = 3;
        }};

        grassblock = new StaticBlock("grassblock") {{
            solid = true;
            variants = 2;
        }};

        mossblock = new StaticBlock("mossblock") {{
            solid = true;
        }};

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

        dirtblock = new StaticBlock("dirtblock") {{
            solid = true;
        }};
    }
}
