package mindustry.world.blocks.sandbox;

import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerNode;

public class PowerSource extends PowerNode{

    public PowerSource(String name){
        super(name);
        maxNodes = 100;
        outputsPower = true;
        consumesPower = false;
        stopOnDisabled = true;
    }

    @Override
    public float getPowerProduction(Tile tile){
        return tile.entity.enabled() ? 10000f : 0f;
    }

}
