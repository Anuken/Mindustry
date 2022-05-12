package mindustry.world.blocks.legacy;

import arc.util.io.*;
import mindustry.gen.*;

public class LegacyCommandCenter extends LegacyBlock{

    public LegacyCommandCenter(String name){
        super(name);

        update = true;
    }

    public class CommandBuild extends Building{

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
