package mindustry.world.blocks.legacy;

import arc.util.io.*;
import mindustry.gen.*;

public class LegacyCommandCenter extends LegacyBlock{

    public LegacyCommandCenter(String name){
        super(name);
        update = true;
    }

    public class LegacyCommandCenterEntity extends TileEntity{
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            read.b();
        }
    }
}
