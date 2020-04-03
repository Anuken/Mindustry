package mindustry.entities.units;

import arc.math.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;

public class UnitDrops{

    public static void dropItems(BaseUnit unit){
        TileEntity core = unit.getClosestEnemyCore();

        if(core == null || core.dst(unit) > Vars.mineTransferRange){
            return;
        }

        if(unit.item().amount > 0 && unit.item().item != null){
            if(!Vars.headless && !Vars.data.isUnlocked(unit.item().item)) return;
            unit.item().amount = core.tile.block().acceptStack(unit.item().item, unit.item().amount, core.tile, null);
            Call.transferItemTo(unit.item().item, unit.item().amount, unit.x + Mathf.range(2f), unit.y + Mathf.range(2f), core.tile);
        }
    }

    public static void seed(BaseUnit unit){
        if(!Vars.state.rules.unitDrops) return;

        unit.item().item = item(unit.getType());
        unit.item().amount = 10;
    }

    private static Item item(UnitType type){

        if (type == UnitTypes.dagger) return Items.copper;
        if (type == UnitTypes.crawler) return Items.coal;
        if (type == UnitTypes.titan) return Items.lead;

        if (type == UnitTypes.fortress) return Items.titanium;
        if (type == UnitTypes.eruptor) return Items.pyratite;
        if (type == UnitTypes.chaosArray) return Items.sporePod;
        if (type == UnitTypes.eradicator) return Items.blastCompound;

        if (type == UnitTypes.wraith) return Items.sand;
        if (type == UnitTypes.ghoul) return Items.scrap;
        if (type == UnitTypes.revenant) return Items.graphite;
        if (type == UnitTypes.lich) return Items.metaglass;
        if (type == UnitTypes.reaper) return Items.phasefabric;

        return null;
    }
}
