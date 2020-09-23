package mindustry.world.blocks.legacy;

import arc.util.io.*;
import mindustry.gen.*;

public class LegacyUnitFactory extends LegacyBlock{

    public LegacyUnitFactory(String name){
        super(name);
        update = true;
        hasPower = true;
        hasItems = true;
        solid = false;
    }

    public class LegacyUnitFactoryBuild extends Building{

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            //build time
            read.f();

            if(revision == 0){
                //spawn count
                read.i();
            }
        }
    }
}
