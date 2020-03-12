package mindustry.entities.units;

import arc.math.Mathf;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.TileEntity;
import mindustry.gen.Call;
import mindustry.type.*;

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

        Item item = Items.surgealloy;

        if(!Vars.headless && !Vars.data.isUnlocked(item)) return;

        int amount = Mathf.ceil(unit.maxHealth() / 100f);
        amount = core.tile.block().acceptStack(item, amount, core.tile, null);
        if(amount > 0){
            Call.transferItemTo(item, amount, unit.x + Mathf.range(2f), unit.y + Mathf.range(2f), core.tile);
        }
    }
}
