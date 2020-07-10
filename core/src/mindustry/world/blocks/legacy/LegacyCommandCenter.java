package mindustry.world.blocks.legacy;

import arc.util.io.*;
import mindustry.gen.*;

public class LegacyCommandCenter extends LegacyBlock{

    public LegacyCommandCenter(String name){
        super(name);
        update = true;
    }

    public class LegacyCommandCenterEntity extends Building{
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            read.b();
        }
    }
}
