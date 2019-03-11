package io.anuke.mindustry.world.blocks;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class OreBlock extends Floor{
    private static final ObjectMap<Item, ObjectMap<Block, Floor>> oreBlockMap = new ObjectMap<>();

    public Floor base;

    public OreBlock(Item ore, Floor base){
        super("ore-" + ore.name + "-" + base.name);
        this.localizedName = ore.localizedName() + " " + base.localizedName;
        this.itemDrop = ore;
        this.base = base;
        this.variants = 3;
        this.edge = base.name;
        this.blendGroup = base.blendGroup;
        this.color.set(ore.color);

        oreBlockMap.getOr(ore, ObjectMap::new).put(base, this);
    }

    @Override
    public void init(){
        super.init();
    }

    @Override
    public String getDisplayName(Tile tile){
        return itemDrop.localizedName();
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        drawEdges(tile);
    }

    @Override
    public boolean doEdge(Floor floor, boolean f){
        return floor != base && super.doEdge(floor, f);
    }

    @Override
    protected boolean edgeOnto(Floor other){
        return other != base;
    }

    public static Floor get(Block floor, Item item){
        if(!oreBlockMap.containsKey(item) || !oreBlockMap.get(item).containsKey(floor)) return null;
        return oreBlockMap.get(item).get(floor);
    }
}
