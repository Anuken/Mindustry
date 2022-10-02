package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

public class ConsumeItemDynamic extends Consume{
    public final Func<Building, ItemStack[]> items;

    @SuppressWarnings("unchecked")
    public <T extends Building> ConsumeItemDynamic(Func<T, ItemStack[]> items){
        this.items = (Func<Building, ItemStack[]>)items;
    }

    @Override
    public void apply(Block block){
        block.hasItems = true;
        block.acceptsItems = true;
    }

    @Override
    public void build(Building build, Table table){
        ItemStack[][] current = {items.get(build)};

        table.table(cont -> {
            table.update(() -> {
                if(current[0] != items.get(build)){
                    rebuild(build, cont);
                    current[0] = items.get(build);
                }
            });

            rebuild(build, cont);
        });
    }

    private void rebuild(Building tile, Table table){
        table.clear();
        int i = 0;

        for(ItemStack stack : items.get(tile)){
            table.add(new ReqImage(new ItemImage(stack.item.uiIcon, stack.amount),
            () -> tile.items != null && tile.items.has(stack.item, stack.amount))).padRight(8).left();
            if(++i % 4 == 0) table.row();
        }
    }

    @Override
    public void trigger(Building build){
        for(ItemStack stack : items.get(build)){
            build.items.remove(stack);
        }
    }

    @Override
    public float efficiency(Building build){
        return build.consumeTriggerValid() || build.items.has(items.get(build)) ? 1f : 0f;
    }
}
