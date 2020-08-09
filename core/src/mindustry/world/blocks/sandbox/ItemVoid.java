package mindustry.world.blocks.sandbox;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class ItemVoid extends Block{

    public ItemVoid(String name){
        super(name);
        update = solid = acceptsItems = true;
    }

    public class ItemVoidEntity extends Building{
        @Override
        public void handleItem(Building source, Item item){}

        @Override
        public boolean acceptItem(Building source, Item item){
            return true;
        }
    }
}
