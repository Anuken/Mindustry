package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.graphics.DrawLayer;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.*;

public class Blocks {
    public static final Block

    air = new Block("air") {
        //don't draw
        public void draw(Tile tile) {}
    },

    //player/enemy spawnpoint?
    spawn = new Block("spawn"),

    blockpart = new BlockPart(),

    space = new Floor("space") {{
        variants = 0;
        drawLayer = DrawLayer.space;
        solid = true;
    }},

    deepwater = new Floor("deepwater") {{
        variants = 0;
        solid = true;
        liquidDrop = Liquids.water;
        liquid = true;
        drawLayer = DrawLayer.water;
    }},

    water = new Floor("water") {{
        variants = 0;
        solid = true;
        liquidDrop = Liquids.water;
        liquid = true;
        drawLayer = DrawLayer.water;
    }},

    lava = new Floor("lava") {{
        variants = 0;
        solid = true;
        liquidDrop = Liquids.lava;
        liquid = true;
        drawLayer = DrawLayer.lava;
    }},

    oil = new Floor("oil") {{
        variants = 0;
        solid = true;
        liquidDrop = Liquids.oil;
        liquid = true;
        drawLayer = DrawLayer.oil;
    }},

    stone = new Floor("stone") {{
        drops = new ItemStack(Items.stone, 1);
        blends = block -> block != this && !(block instanceof Ore);
    }},

    blackstone = new Floor("blackstone") {{
        drops = new ItemStack(Items.stone, 1);
    }},

    iron = new Ore("iron") {{
        drops = new ItemStack(Items.iron, 1);
    }},

    lead = new Ore("lead") {{
        drops = new ItemStack(Items.lead, 1);
    }},

    coal = new Ore("coal") {{
        drops = new ItemStack(Items.coal, 1);
    }},

    titanium = new Ore("titanium") {{
        drops = new ItemStack(Items.titanium, 1);
    }},

    thorium = new Ore("thorium") {{
        drops = new ItemStack(Items.thorium, 1);
    }},

    dirt = new Floor("dirt") {
    },

    sand = new Floor("sand") {{
        drops = new ItemStack(Items.sand, 1);
    }},

    ice = new Floor("ice") {
    },

    snow = new Floor("snow") {
    },

    grass = new Floor("grass") {
    },

    sandblock = new StaticBlock("sandblock") {{
        solid = true;
        variants = 3;
    }},

    snowblock = new StaticBlock("snowblock") {{
        solid = true;
        variants = 3;
    }},

    stoneblock = new StaticBlock("stoneblock") {{
        solid = true;
        variants = 3;
    }},

    blackstoneblock = new StaticBlock("blackstoneblock") {{
        solid = true;
        variants = 3;
    }},

    grassblock = new StaticBlock("grassblock") {{
        solid = true;
        variants = 2;
    }},

    mossblock = new StaticBlock("mossblock") {{
        solid = true;
    }},

    shrub = new Rock("shrub"),

    rock = new Rock("rock") {{
        variants = 2;
        varyShadow = true;
    }},

    icerock = new Rock("icerock") {{
        variants = 2;
        varyShadow = true;
    }},

    blackrock = new Rock("blackrock") {{
        variants = 1;
        varyShadow = true;
    }},

    dirtblock = new StaticBlock("dirtblock") {{
        solid = true;
    }};
}
