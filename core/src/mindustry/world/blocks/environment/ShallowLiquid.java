package mindustry.world.blocks.environment;

import arc.util.*;
import mindustry.world.*;

/**
 * Do not use in mods. This class provides no new functionality, and is only used for the Mindustry sprite generator.
 * Use the standard Floor class instead.
 * */
public class ShallowLiquid extends Floor{
    public @Nullable Floor liquidBase, floorBase;
    public float liquidOpacity = 0.35f;

    public ShallowLiquid(String name){
        super(name);
    }

    public void set(Block liquid, Block floor){
        this.liquidBase = liquid.asFloor();
        this.floorBase = floor.asFloor();

        isLiquid = true;
        variants = floorBase.variants;
        status = liquidBase.status;
        liquidDrop = liquidBase.liquidDrop;
        cacheLayer = liquidBase.cacheLayer;
    }
}
