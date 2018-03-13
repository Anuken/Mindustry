package io.anuke.mindustry.world.blocks.types.generation;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class SolarGenerator extends Generator {
    /**power generated per frame*/
    protected float generation = 0.005f;

    public SolarGenerator(String name){
        super(name);
        hasInventory = false;
    }

    @Override
    public void update(Tile tile){
        addPower(tile, generation * Timers.delta());

        distributeLaserPower(tile);
    }

}
