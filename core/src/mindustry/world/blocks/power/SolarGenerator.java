package mindustry.world.blocks.power;

import arc.struct.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.state;

public class SolarGenerator extends PowerGenerator{

    public SolarGenerator(String name){
        super(name);
        // Remove the BlockFlag.producer flag to make this a lower priority target than other generators.
        flags = EnumSet.of();
        entityType = GeneratorEntity::new;
    }

    @Override
    public void update(Tile tile){
        tile.<GeneratorEntity>ent().productionEfficiency = state.rules.solarPowerMultiplier < 0 ? (state.rules.lighting ? 1f - state.rules.ambientLight.a : 1f) : state.rules.solarPowerMultiplier;
    }

    @Override
    public void setStats(){
        super.setStats();
        // Solar Generators don't really have an efficiency (yet), so for them 100% = 1.0f
        stats.remove(generationType);
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }
}
