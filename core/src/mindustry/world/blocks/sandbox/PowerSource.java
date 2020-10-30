package mindustry.world.blocks.sandbox;

import mindustry.world.blocks.power.*;

public class PowerSource extends PowerNode{

    public PowerSource(String name){
        super(name);
        maxNodes = 100;
        outputsPower = true;
        consumesPower = false;
    }

    public class PowerSourceBuild extends PowerNodeBuild{
        @Override
        public float getPowerProduction(){
            return enabled ? 10000f : 0f;
        }
    }

}
