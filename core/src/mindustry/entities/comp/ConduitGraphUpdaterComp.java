package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.liquid.Conduit.*;

@EntityDef(value = ConduitGraphUpdaterc.class, serialize = false, genio = false)
@Component
abstract class ConduitGraphUpdaterComp implements Entityc{
    public transient ConduitGraph graph;

    @Override
    public void update(){
        graph.update();
    }
}
