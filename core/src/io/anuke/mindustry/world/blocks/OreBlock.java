package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

/**An overlay ore for a specific item type.*/
public class OreBlock extends OverlayFloor{

    public OreBlock(Item ore){
        super("ore-" + ore.name);
        this.localizedName = ore.localizedName();
        this.itemDrop = ore;
        this.variants = 3;
        this.color.set(ore.color);
    }

    /** For mod use only!*/
    public OreBlock(String name){
        super(name);
    }

    public void setup(Item ore){
        this.localizedName = ore.localizedName();
        this.itemDrop = ore;
        this.variants = 3;
        this.color.set(ore.color);
    }

    @Override
    public void init(){
        super.init();

        if(itemDrop != null){
            setup(itemDrop);
        }else{
            throw new IllegalArgumentException(name + " must have an item drop!");
        }
    }

    @Override
    public String getDisplayName(Tile tile){
        return itemDrop.localizedName();
    }
}
