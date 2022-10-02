package mindustry.world.blocks.sandbox;

import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

public class PowerVoid extends PowerBlock{

    public PowerVoid(String name){
        super(name);
        consumePower(Float.MAX_VALUE);
        envEnabled = Env.any;
        enableDrawStatus = false;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.powerUse);
    }
}
