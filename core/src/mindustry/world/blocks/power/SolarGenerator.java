package mindustry.world.blocks.power;

import arc.struct.*;
import mindustry.world.meta.*;

import static mindustry.Vars.state;

public class SolarGenerator extends PowerGenerator{

    public SolarGenerator(String name){
        super(name);
        //remove the BlockFlag.producer flag to make this a lower priority target than other generators.
        flags = EnumSet.of();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(generationType);
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    public class SolarGeneratorEntity extends GeneratorEntity{
        @Override
        public void updateTile(){
            productionEfficiency = state.rules.solarPowerMultiplier < 0 ? (state.rules.lighting ? 1f - state.rules.ambientLight.a : 1f) : state.rules.solarPowerMultiplier;
        }
    }
}
