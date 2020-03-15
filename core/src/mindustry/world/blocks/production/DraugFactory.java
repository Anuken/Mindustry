package mindustry.world.blocks.production;

import arc.util.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class DraugFactory extends UnitFactory{

    protected final int timerMine = timers++;

    protected final int amount = 25;

    public DraugFactory(String name){
        super(name);
    }

    public void update(Tile tile){
        super.update(tile);

        if(tile.<UnitFactoryEntity>ent().spawned == 0) return;
        if(!tile.entity.timer.get(timerMine, 60 * 10)) return;
        TileEntity core = state.teams.closestCore(tile.drawx(), tile.drawy(), tile.getTeam());
        if(core == null) return;
        Item ore = Structs.findMin(unitType.toMine, indexer::hasOre, (a, b) -> -Integer.compare(core.items.get(a), core.items.get(b)));
        if(ore  == null) return;
        Tile vein = indexer.findClosestOre(core.x, core.y, ore);
        if(vein == null) return;
        int max = core.block.acceptStack(ore, amount, core.tile, null);
        if(max == 0) return;
        Call.transferItemTo(ore, max, vein.drawx(), vein.drawy(), core.tile);
    }
}
