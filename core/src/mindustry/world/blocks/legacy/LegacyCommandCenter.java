package mindustry.world.blocks.legacy;

import mindustry.gen.*;
import mindustry.io.*;

public class LegacyCommandCenter extends LegacyBlock{

    public LegacyCommandCenter(String name){
        super(name);

        update = true;
    }

    public class LegacyCommandBuild extends Building{

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(0);
        }

        @Override
        public void read(Reads read, byte version){
            super.read(read, version);
            read.b();
        }
    }
}
