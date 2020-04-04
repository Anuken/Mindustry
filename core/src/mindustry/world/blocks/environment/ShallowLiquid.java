package mindustry.world.blocks.environment;

import mindustry.world.*;

//do not use in mods!
public class ShallowLiquid extends Floor{
    public final Floor liquidBase, floorBase;

    public ShallowLiquid(String name, Block liquid, Block floor){
        super(name);

        this.liquidBase = liquid.asFloor();
        this.floorBase = floor.asFloor();

        isLiquid = true;
        variants = floor.asFloor().variants;
        status = liquid.asFloor().status;
        liquidDrop = liquid.asFloor().liquidDrop;
        cacheLayer = liquid.asFloor().cacheLayer;
    }
}
