package mindustry.world.blocks.production;

import arc.util.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class DraugFactory extends UnitFactory{

    protected final int timerEnable = timers++;

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
        int amount = core.block.acceptStack(ore, 25, core.tile, null);
        if(amount > 0) Call.transferItemTo(ore, amount, vein.drawx(), vein.drawy(), core.tile);
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        if(!tile.entity.timer.get(timerEnable, 120)) return;

        Tile core = tile.getTeam().core().tile;
        tile.<DraugFactoryEntity>ent().spawned(core.block.acceptStack(Items.copper, 25, core, null) >= 25 || core.block.acceptStack(Items.lead, 25, core, null) >= 25 ? 0 : 1);
    }

    class DraugFactoryEntity extends UnitFactoryEntity{
        private int spawnedSync;

        public void spawned(int spawned){
            this.spawned = spawned;
            if(this.spawned != spawnedSync) netServer.titanic.add(tile);
            spawnedSync = spawned;
        }
    }
}
