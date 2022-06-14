package mindustry.world.consumers;

import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ConsumeItems extends Consume{
    public final ItemStack[] items;

    public ConsumeItems(ItemStack[] items){
        this.items = items;
    }

    /** Mods.*/
    protected ConsumeItems(){
        this(ItemStack.empty);
    }

    @Override
    public void apply(Block block){
        block.hasItems = true;
        block.acceptsItems = true;
        for(var stack : items){
            block.itemFilter[stack.item.id] = true;
        }
    }

    @Override
    public void build(Building build, Table table){
        table.table(c -> {
            int i = 0;
            for(var stack : items){
                c.add(new ReqImage(new ItemImage(stack.item.uiIcon, Math.round(stack.amount * consumeMultiplier.get())),
                () -> build.items.has(stack.item, Math.round(stack.amount * consumeMultiplier.get())))).padRight(8);
                if(++i % 4 == 0) c.row();
            }
        }).left();
    }

    @Override
    public void trigger(Building build){
        for(var stack : items){
            build.items.remove(stack.item, Math.round(stack.amount * consumeMultiplier.get()));
        }
    }

    @Override
    public float efficiency(Building build){
        if(build.consumeTriggerValid()){
            for(ItemStack stack : items){
                if(!build.items.has(stack.item, Math.round(stack.amount * consumeMultiplier.get()))){
                    return 0f;
                }
            }
            return 1f;
        }
        return 0f;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, stats.timePeriod < 0 ? StatValues.items(items) : StatValues.items(stats.timePeriod, items));
    }
}
