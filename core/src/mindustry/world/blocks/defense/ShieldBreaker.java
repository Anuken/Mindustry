package mindustry.world.blocks.defense;

import mindustry.gen.*;
import mindustry.world.*;

public class ShieldBreaker extends Block{

    public ShieldBreaker(String name){
        super(name);

        solid = update = true;
    }

    public class ShieldBreakerBuild extends Building{

        @Override
        public void updateTile(){

        }
    }
}
