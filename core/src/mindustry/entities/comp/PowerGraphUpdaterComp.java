package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.power.*;

@EntityDef(value = PowerGraphUpdaterc.class, serialize = false, genio = false)
@Component
abstract class PowerGraphUpdaterComp implements Entityc{
    public transient PowerGraph graph;

    @Override
    public void update(){
        graph.update();
    }
}
