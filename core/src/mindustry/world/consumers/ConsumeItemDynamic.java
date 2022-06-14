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
            table.add(new ReqImage(new ItemImage(stack.item.uiIcon, Math.round(stack.amount * consumeMultiplier.get())),
            () -> tile.items != null && tile.items.has(stack.item, Math.round(stack.amount * consumeMultiplier.get())))).padRight(8).left();
            if(++i % 4 == 0) table.row();
        }
    }

    @Override
    public void trigger(Building build){
        for(ItemStack stack : items.get(build)){
            build.items.remove(stack.item, Math.round(stack.amount * consumeMultiplier.get()));
        }
    }

    @Override
    public float efficiency(Building build){
        if(build.consumeTriggerValid()){
            for(ItemStack stack : items.get(build)){
                if(!build.items.has(stack.item, Math.round(stack.amount * consumeMultiplier.get()))){
                    return 0f;
                }
            }
            return 1f;
        }
        return 0f;
    }
}
