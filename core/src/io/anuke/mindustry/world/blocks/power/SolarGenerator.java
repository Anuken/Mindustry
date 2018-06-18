package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class SolarGenerator extends PowerGenerator {
    /**power generated per frame*/
    protected float generation = 0.005f;

    public SolarGenerator(String name){
        super(name);
    }

    @Override
    public void update(Tile tile){
        addPower(tile, generation * Timers.delta());

        distributePower(tile);
    }

}
