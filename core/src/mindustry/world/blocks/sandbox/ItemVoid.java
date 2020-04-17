package mindustry.world.blocks.sandbox;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class ItemVoid extends Block{

    public ItemVoid(String name){
        super(name);
        update = solid = true;
    }

    public class ItemVoidEntity extends TileEntity{
        @Override
        public void handleItem(Tilec source, Item item){}

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return true;
        }
    }
}
