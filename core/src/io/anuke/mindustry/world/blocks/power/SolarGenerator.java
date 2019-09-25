package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.collection.EnumSet;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.meta.StatUnit;

public class SolarGenerator extends PowerGenerator{

    public SolarGenerator(String name){
        super(name);
        // Remove the BlockFlag.producer flag to make this a lower priority target than other generators.
        flags = EnumSet.of();
    }

    @Override
    public void setStats(){
        super.setStats();
        // Solar Generators don't really have an efficiency (yet), so for them 100% = 1.0f
        stats.remove(generationType);
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    @Override
    public TileEntity newEntity(){
        return new PowerGenerator.GeneratorEntity(){{
            productionEfficiency = 1.0f;
        }};
    }

}
