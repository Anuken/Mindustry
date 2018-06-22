package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.util.Mathf;

public class UnitDrops {
    private static Item[] dropTable;

    public static void dropItems(BaseUnit unit){
        if(dropTable == null){
            dropTable = new Item[]{Items.tungsten, Items.lead, Items.carbide};
        }

        for(Item item : dropTable){
            if(Mathf.chance(0.2)){
                int amount = Mathf.random(1, 30);
                CallEntity.createItemDrop(item, amount, unit.x + Mathf.range(2f), unit.y + Mathf.range(2f),
                        unit.getVelocity().x + Mathf.range(0.5f), unit.getVelocity().y + Mathf.range(0.5f));
            }
        }
    }
}
