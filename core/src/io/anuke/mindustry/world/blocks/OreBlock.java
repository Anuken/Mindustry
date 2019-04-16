package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;

/**An overlay ore for a specific item type.*/
public class OreBlock extends OverlayFloor{

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
}
