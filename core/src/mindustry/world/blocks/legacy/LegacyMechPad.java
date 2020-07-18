package mindustry.world.blocks.legacy;

import arc.util.io.*;
import mindustry.gen.*;

public class LegacyMechPad extends LegacyBlock{

    public LegacyMechPad(String name){
        super(name);
        update = true;
        hasPower = true;
    }

    public class LegacyMechPadEntity extends Building{

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            //read 3 floats for pad data, and discard them
            read.f();
            read.f();
            read.f();
        }
    }
}
