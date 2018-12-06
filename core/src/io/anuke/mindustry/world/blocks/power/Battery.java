package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.scene.ui.layout.Table;

public class Battery extends PowerDistributor{

    public Battery(String name){
        super(name);
        outputsPower = true;
        consumesPower = true;
    }

    @Override
    public boolean buildLogic(Tile tile, Table table, boolean update){
        return false;
    }
}
