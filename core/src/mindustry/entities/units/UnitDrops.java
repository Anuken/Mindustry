package mindustry.entities.units;

import arc.math.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.content.Items.*;

public class UnitDrops{

    public static void dropItems(BaseUnit unit){
        TileEntity core = unit.getClosestEnemyCore();

        if(unit.item().amount == 0 || unit.item().item == null) return;

        if(core == null || core.dst(unit) > Vars.mineTransferRange){
            Player nearby = (Player)Units.closestEnemy(unit.getTeam(), unit.x, unit.y, Vars.mineTransferRange, u -> u instanceof Player && u.acceptsItem(unit.item().item));
            if(nearby != null) nearby.addItem(unit.item().item, unit.item().amount-1);
            if(nearby != null) Call.transferItemToUnit(unit.item().item, unit.x, unit.y, nearby);
            if(nearby != null) nearby.dropoff = true;
        }else{
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

        if (type == UnitTypes.dagger) return copper;
        if (type == UnitTypes.crawler) return graphite;
        if (type == UnitTypes.titan) return lead;

        if (type == UnitTypes.fortress) return titanium;
        if (type == UnitTypes.eruptor) return plastanium;
        if (type == UnitTypes.chaosArray) return thorium;

        if (type == UnitTypes.wraith) return silicon;
        if (type == UnitTypes.ghoul) return metaglass;
        if (type == UnitTypes.revenant) return surgealloy;
        if (type == UnitTypes.lich) return phasefabric;

        return null;
    }
}
