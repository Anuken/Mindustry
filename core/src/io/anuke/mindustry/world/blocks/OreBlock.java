package io.anuke.mindustry.world.blocks;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class OreBlock extends Floor{
    private static final ObjectMap<Item, ObjectMap<Block, Block>> oreBlockMap = new ObjectMap<>();

    public Floor base;

    public OreBlock(Item ore, Floor base){
        super("ore-" + ore.name + "-" + base.name);
        this.formalName = ore.localizedName() + " " + base.formalName;
        this.itemDrop = ore;
        this.base = base;
        this.variants = 3;
        this.minimapColor = ore.color;
        this.edge = base.name;

        oreBlockMap.getOr(ore, ObjectMap::new).put(base, this);
    }

    @Override
    public String getDisplayName(Tile tile){
        return itemDrop.localizedName();
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());
    }

    public static Block get(Block floor, Item item){
        if(!oreBlockMap.containsKey(item)) throw new IllegalArgumentException("Item '" + item + "' is not an ore!");
        if(!oreBlockMap.get(item).containsKey(floor)) throw new IllegalArgumentException("Block '" + floor.name + "' does not support ores!");
        return oreBlockMap.get(item).get(floor);
    }
}
