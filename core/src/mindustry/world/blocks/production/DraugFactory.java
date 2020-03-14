package mindustry.world.blocks.production;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class DraugFactory extends UnitFactory{

    protected final int timerEnable = timers++;

    protected final int amount = 25;

    public DraugFactory(String name){
        super(name);
        entityType = DraugFactoryEntity::new;
    }

    public void trigger(Tile tile){
        TileEntity core = state.teams.closestCore(tile.drawx(), tile.drawy(), tile.getTeam());
        if(core == null) return;
        Item ore = Structs.findMin(unitType.toMine, indexer::hasOre, (a, b) -> -Integer.compare(core.items.get(a), core.items.get(b)));
        if(ore  == null) return;
        Tile vein = indexer.findClosestOre(core.x, core.y, ore);
        if(vein == null) return;
        Call.transferItemTo(ore, amount, vein.drawx(), vein.drawy(), core.tile);
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        if(!tile.entity.timer.get(timerEnable, 60)) return;

        tile.<DraugFactoryEntity>ent().spawned(accepts(tile.getTeam().core(), unitType.toMine) ? 0 : 1);
    }

    class DraugFactoryEntity extends UnitFactoryEntity{
        private int spawnedSync;

        public void spawned(int spawned){
            this.spawned = spawned;
            if(this.spawned != spawnedSync) netServer.titanic.add(tile);
            spawnedSync = spawned;
        }
    }

    protected boolean accepts(CoreEntity core, ObjectSet<Item> ores){
        return ores.asArray().count(item -> core.block.acceptItem(item, core.tile, null)) > 0;
    }
}
