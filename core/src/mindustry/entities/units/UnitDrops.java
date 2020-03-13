package mindustry.entities.units;

import mindustry.*;
import mindustry.entities.type.*;

import static mindustry.Vars.state;

public class UnitDrops{

    public static void dropItems(BaseUnit unit){
        //items only dropped in waves for enemy team
        if(unit.getTeam() != state.rules.waveTeam || !Vars.state.rules.unitDrops){
            return;
        }

        TileEntity core = unit.getClosestEnemyCore();

        if(core == null){
            return;
        }

        if(unit.item().amount > 0 && unit.item().item != null){
            if(!Vars.headless && !Vars.data.isUnlocked(unit.item().item)) return;
            core.items.add(unit.item().item, unit.item().amount);
        }
    }
}
