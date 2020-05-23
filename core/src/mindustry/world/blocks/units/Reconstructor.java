package mindustry.world.blocks.units;

import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.world.*;

public class Reconstructor extends Block{

    public Reconstructor(String name){
        super(name);
    }

    public class ReassemblerEntity extends TileEntity{
        public @Nullable Unitc unit;

        @Override
        public void updateTile(){

        }
    }
}
