package mindustry.world.blocks.sandbox;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ItemVoid extends Block{

    public ItemVoid(String name){
        super(name);
        group = BlockGroup.transportation;
        update = solid = acceptsItems = true;
        envEnabled = Env.any;
    }

    public class ItemVoidBuild extends Building{
        @Override
        public void handleItem(Building source, Item item){}

        @Override
        public boolean acceptItem(Building source, Item item){
            return enabled;
        }
    }
}
