package mindustry.entities.units;

import arc.math.Mathf;
import mindustry.Vars;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.TileEntity;
import mindustry.gen.Call;

import static mindustry.Vars.*;

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
            Call.transferItemTo(unit.item().item, unit.item().amount, unit.x + Mathf.range(2f), unit.y + Mathf.range(2f), core.tile);
        }
    }
}
