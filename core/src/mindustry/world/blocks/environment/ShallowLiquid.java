package mindustry.world.blocks.environment;

import mindustry.world.*;

//do not use in mods!
public class ShallowLiquid extends Floor{
    public final Floor liquidBase, floorBase;
    public float liquidOpacity = 0.35f;

    public ShallowLiquid(String name, Block liquid, Block floor){
        super(name);

        this.liquidBase = liquid.asFloor();
        this.floorBase = floor.asFloor();

        isLiquid = true;
        variants = floorBase.variants;
        status = liquidBase.status;
        liquidDrop = liquidBase.liquidDrop;
        cacheLayer = liquidBase.cacheLayer;
    }
}
