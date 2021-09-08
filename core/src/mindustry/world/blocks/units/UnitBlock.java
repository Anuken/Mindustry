package mindustry.world.blocks.units;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

public class UnitBlock extends PayloadBlock{

    public UnitBlock(String name){
        super(name);
        group = BlockGroup.units;
        outputsPayload = true;
        rotate = true;
        update = true;
        solid = true;
    }

    @Remote(called = Loc.server)
    public static void unitBlockSpawn(Tile tile){
        if(tile == null || !(tile.build instanceof UnitBuild build)) return;
        build.spawned();
    }

    public class UnitBuild extends PayloadBlockBuild<UnitPayload>{
        public float progress, time, speedScl;

        public void spawned(){
            progress = 0f;
            payload = null;
        }

        @Override
        public void dumpPayload(){
            if(payload.dump()){
                Call.unitBlockSpawn(tile);
            }
        }
    }
}
