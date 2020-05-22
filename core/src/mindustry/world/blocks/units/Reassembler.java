package mindustry.world.blocks.units;

import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.world.*;

public class Reassembler extends Block{

    public Reassembler(String name){
        super(name);
    }

    public class ReassemblerEntity extends TileEntity{
        public @Nullable Unitc unit;

        @Override
        public void updateTile(){

        }
    }
}
