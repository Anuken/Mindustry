package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.graphics.CacheLayer;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

public class Blocks extends BlockList implements ContentList{
    public static Block air, blockpart, spawn, space, metalfloor, deepwater, water, lava, tar, stone,
    blackstone, dirt, sand, ice, snow, grass, shrub, rock, icerock, blackrock;


    @Override
    public void load(){
        air = new Floor("air"){
            {
                blend = false;
                alwaysReplace = true;
            }

            public void draw(Tile tile){}
            public void load(){}
            public void init(){}
        };

        blockpart = new BlockPart();

        spawn = new Block("spawn"){

            public void drawShadow(Tile tile){}

            public void draw(Tile tile){
                Draw.color(Color.SCARLET);
                Lines.circle(tile.worldx(), tile.worldy(), 4f +Mathf.absin(Timers.time(), 6f, 6f));
                Draw.color();
            }
        };

        //Registers build blocks from size 1-6
        //no reference is needed here since they can be looked up by name later
        for(int i = 1; i <= 6; i++){
            new BuildBlock("build" + i);
        }

        space = new Floor("space"){{
            placeableOn = false;
            variants = 0;
            cacheLayer = CacheLayer.space;
            solid = true;
            blend = false;
            minimapColor = Color.valueOf("000001");
        }};

        metalfloor = new Floor("metalfloor"){{
            variants = 6;
        }};

        deepwater = new Floor("deepwater"){{
            liquidColor = Color.valueOf("546bb3");
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.water;
            isLiquid = true;
            status = StatusEffects.wet;
            statusIntensity = 1f;
            drownTime = 140f;
            cacheLayer = CacheLayer.water;
            minimapColor = Color.valueOf("465a96");
        }};

        water = new Floor("water"){{
            liquidColor = Color.valueOf("546bb3");
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusIntensity = 0.9f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            minimapColor = Color.valueOf("506eb4");
        }};

        lava = new Floor("lava"){{
            drownTime = 100f;
            liquidColor = Color.valueOf("ed5334");
            speedMultiplier = 0.2f;
            damageTaken = 0.5f;
            status = StatusEffects.melting;
            statusIntensity = 0.8f;
            variants = 0;
            liquidDrop = Liquids.lava;
            isLiquid = true;
            cacheLayer = CacheLayer.lava;
            minimapColor = Color.valueOf("ed5334");
        }};

        tar = new Floor("tar"){{
            drownTime = 150f;
            liquidColor = Color.valueOf("292929");
            status = StatusEffects.tarred;
            statusIntensity = 1f;
            speedMultiplier = 0.19f;
            variants = 0;
            liquidDrop = Liquids.oil;
            isLiquid = true;
            cacheLayer = CacheLayer.oil;
            minimapColor = Color.valueOf("292929");
        }};

        stone = new Floor("stone"){{
            hasOres = true;
            drops = new ItemStack(Items.stone, 1);
            blends = block -> block != this && !(block instanceof OreBlock);
            minimapColor = Color.valueOf("323232");
            playerUnmineable = true;
        }};

        blackstone = new Floor("blackstone"){{
            drops = new ItemStack(Items.stone, 1);
            minimapColor = Color.valueOf("252525");
            playerUnmineable = true;
            hasOres = true;
        }};

        dirt = new Floor("dirt"){{
            minimapColor = Color.valueOf("6e501e");
        }};

        sand = new Floor("sand"){{
            drops = new ItemStack(Items.sand, 1);
            minimapColor = Color.valueOf("988a67");
            hasOres = true;
            playerUnmineable = true;
        }};

        ice = new Floor("ice"){{
            dragMultiplier = 0.2f;
            speedMultiplier = 0.4f;
            minimapColor = Color.valueOf("b8eef8");
            hasOres = true;
        }};

        snow = new Floor("snow"){{
            minimapColor = Color.valueOf("c2d1d2");
            hasOres = true;
        }};

        grass = new Floor("grass"){{
            hasOres = true;
            minimapColor = Color.valueOf("549d5b");
        }};

        shrub = new Rock("shrub"){{
            shadow = "shrubshadow";
        }};

        rock = new Rock("rock"){{
            variants = 2;
        }};

        icerock = new Rock("icerock"){{
            variants = 2;
        }};

        blackrock = new Rock("blackrock"){{
            variants = 1;
        }};
    }
}
