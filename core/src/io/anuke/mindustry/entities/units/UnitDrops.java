package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.util.Mathf;

public class UnitDrops {
    private static final int maxItems = 200;
    private static Item[] dropTable;

    public static void dropItems(BaseUnit unit){
        if(Vars.itemGroup.size() > maxItems){
            return;
        }

        if(dropTable == null){
            dropTable = new Item[]{Items.tungsten, Items.lead, Items.carbide};
        }

        for (int i = 0; i < 3; i++) {
            for(Item item : dropTable){
                if(Mathf.chance(0.03)){
                    int amount = Mathf.random(20, 40);
                    ItemDrop.create(item, amount, unit.x + Mathf.range(2f), unit.y + Mathf.range(2f),
                            unit.getVelocity().x + Mathf.range(3f), unit.getVelocity().y + Mathf.range(3f));
                }
            }
        }
    }
}
