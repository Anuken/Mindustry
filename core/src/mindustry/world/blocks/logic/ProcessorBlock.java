package mindustry.world.blocks.logic;

import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.world.*;

public class ProcessorBlock extends Block{

    public ProcessorBlock(String name){
        super(name);
        configurable = true;
    }

    public class ProcessorEntity extends TileEntity{
        //all tiles in the block network - does not include itself
        Array<Tilec> network = new Array<>();

        @Override
        public boolean onConfigureTileTapped(Tilec other){
            if(other == this) return true;

            if(!network.contains(other)){
                network.add(other);
            }else{
                network.remove(other);
            }
            return false;
        }

        @Override
        public void buildConfiguration(Table table){
            super.buildConfiguration(table);
        }
    }

}
