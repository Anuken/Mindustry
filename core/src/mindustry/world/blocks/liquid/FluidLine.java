package mindustry.world.blocks.liquid;

import arc.struct.*;
import mindustry.gen.*;

/** A line of fluid blocks, usually conduits. Has exactly one output and any number of external inputs. */
public class FluidLine{
    private Seq<Building> builds = new Seq<>();

    public FluidLine(Building start){
        builds.add(start);
    }
}
