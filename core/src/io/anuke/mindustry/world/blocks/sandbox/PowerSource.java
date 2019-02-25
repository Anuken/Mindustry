package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.PowerNode;

public class PowerSource extends PowerNode{

    public PowerSource(String name){
        super(name);
        maxNodes = 100;
        outputsPower = true;
        consumesPower = false;
    }

    @Override
    public float getPowerProduction(Tile tile){
        return 10000f;
    }

}
