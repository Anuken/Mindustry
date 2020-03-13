package mindustry.entities.units;

import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.type.*;

import static mindustry.Vars.state;

public class UnitDrops{

    public static ObjectMap<Item, Integer> table = new ObjectMap<Item, Integer>(){{
        put(Items.metaglass,  150);
        put(Items.surgealloy, 100);
        put(Items.silicon,     75);
        put(Items.titanium,    50);
    }};

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
            unit.item().amount = core.tile.block().acceptStack(unit.item().item, unit.item().amount, core.tile, null);
            core.items.add(unit.item().item, unit.item().amount);
        }
    }

    public static void seed(BaseUnit unit){
        if(Mathf.chance(0.75f)) return;

        unit.item().item = table.keys().toArray().random();
        unit.item().amount = Mathf.floor(unit.maxHealth() / table.get(unit.item().item));
    }
}
