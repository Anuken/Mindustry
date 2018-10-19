package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.EnumSet;

public class SolarGenerator extends PowerGenerator{
    /**
     * power generated per frame
     */
    protected float generation = 0.005f;

    public SolarGenerator(String name){
        super(name);
        flags = EnumSet.of();
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.basePowerGeneration, generation * 60f, StatUnit.powerSecond);
    }

    @Override
    public void update(Tile tile){
        addPower(tile, generation * Timers.delta());

        tile.entity.power.graph.update();
    }

}
