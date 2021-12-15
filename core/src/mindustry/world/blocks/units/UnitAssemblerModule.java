package mindustry.world.blocks.units;

import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.UnitAssembler.*;

public class UnitAssemblerModule extends PayloadBlock{

    public UnitAssemblerModule(String name){
        super(name);
    }

    //TODO how does it link?
    public class UnitAssemblerModuleBuild extends PayloadBlockBuild<BuildPayload>{
        public UnitAssemblerBuild link;

        @Override
        public void updateTile(){

        }

    }
}
