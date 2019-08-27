package io.anuke.mindustry.entities.units;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;

public class UnitDrops{
    private static Item[] dropTable;

    public static void dropItems(BaseUnit unit){
        //items only dropped in waves for enemy team
        if(unit.getTeam() != Vars.waveTeam || !Vars.state.rules.unitDrops){
            return;
        }

        TileEntity core = unit.getClosestEnemyCore();

        if(core == null || core.dst(unit) > Vars.mineTransferRange){
            return;
        }

        if(dropTable == null){
            dropTable = new Item[]{Items.titanium, Items.silicon, Items.lead, Items.copper};
        }

        for(int i = 0; i < 3; i++){
            for(Item item : dropTable){
                //only drop unlocked items
                if(!Vars.headless && !Vars.data.isUnlocked(item)){
                    continue;
                }

                if(Mathf.chance(0.03)){
                    int amount = Mathf.random(20, 40);
                    amount = core.tile.block().acceptStack(item, amount, core.tile, null);
                    if(amount > 0){
                        Call.transferItemTo(item, amount, unit.x + Mathf.range(2f), unit.y + Mathf.range(2f), core.tile);
                    }
                }
            }
        }
    }
}
