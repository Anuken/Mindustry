package io.anuke.mindustry.world.blocks;

import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;

public class OreBlock extends Floor{

    public OreBlock(Item ore){
        super("ore-" + ore.name);
        this.localizedName = ore.localizedName();
        this.itemDrop = ore;
        this.variants = 3;
        this.color.set(ore.color);
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
    }
}
