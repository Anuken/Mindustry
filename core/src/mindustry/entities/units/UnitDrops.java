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
        if (type == UnitTypes.crawler) return Items.graphite;
        if (type == UnitTypes.titan) return Items.lead;

        if (type == UnitTypes.fortress) return Items.titanium;
        if (type == UnitTypes.eruptor) return Items.plastanium;
        if (type == UnitTypes.chaosArray) return Items.thorium;

        if (type == UnitTypes.wraith) return Items.silicon;
        if (type == UnitTypes.ghoul) return Items.metaglass;
        if (type == UnitTypes.revenant) return Items.surgealloy;
        if (type == UnitTypes.lich) return Items.phasefabric;

        return null;
    }
}
