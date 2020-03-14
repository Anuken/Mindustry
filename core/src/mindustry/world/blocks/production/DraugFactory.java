package mindustry.world.blocks.production;

import arc.util.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class DraugFactory extends UnitFactory{

    public DraugFactory(String name){
        super(name);
    }

    public void trigger(Tile tile){
        TileEntity core = state.teams.closestCore(tile.drawx(), tile.drawy(), tile.getTeam());
        if(core == null) return;
        Item ore = Structs.findMin(unitType.toMine, indexer::hasOre, (a, b) -> -Integer.compare(core.items.get(a), core.items.get(b)));
        if(ore  == null) return;
        Tile vein = indexer.findClosestOre(core.x, core.y, ore);
        if(vein == null) return;
        int amount = core.block.acceptStack(ore, 25, core.tile, null);
        if(amount > 0) Call.transferItemTo(ore, amount, vein.drawx(), vein.drawy(), core.tile);
    }
}
