package mindustry.world.blocks.legacy;

import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

public class LegacyUnitFactory extends LegacyBlock{
    public Block replacement = Blocks.air;

    public LegacyUnitFactory(String name){
        super(name);
        update = true;
        hasPower = true;
        hasItems = true;
        solid = false;
    }

    @Override
    public void removeSelf(Tile tile){
        int rot = tile.build == null ? 0 : tile.build.rotation;
        tile.setBlock(replacement, tile.team(), rot);
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
