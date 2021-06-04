package mindustry.world.blocks.production;

import arc.util.*;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;

public class SingleBlockProducer extends BlockProducer{
    public Block result = Blocks.router;

    public SingleBlockProducer(String name){
        super(name);
    }

    public class SingleBlockProducerBuild extends BlockProducerBuild{

        @Nullable
        @Override
        public Block recipe(){
            return result;
        }
    }
}
