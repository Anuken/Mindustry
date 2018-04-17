package io.anuke.mindustry.world.blocks.types.power;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class SolarGenerator extends io.anuke.mindustry.world.blocks.types.power.PowerGenerator {
    /**power generated per frame*/
    protected float generation = 0.005f;

    public SolarGenerator(String name){
        super(name);
        hasItems = false;
    }

    @Override
    public void update(Tile tile){
        addPower(tile, generation * Timers.delta());

        distributePower(tile);
    }

}
