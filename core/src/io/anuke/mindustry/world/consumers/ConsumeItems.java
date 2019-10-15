package io.anuke.mindustry.world.consumers;

import io.anuke.arc.collection.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;
import io.anuke.mindustry.world.meta.values.*;

public class ConsumeItems extends Consume{
    public final @NonNull ItemStack[] items;

    public ConsumeItems(ItemStack[] items){
        this.items = items;
    }

    /** Mods.*/
    protected ConsumeItems(){
        this(new ItemStack[]{});
    }

    @Override
    public void applyItemFilter(Bits filter){
        for(ItemStack stack : items){
            filter.set(stack.item.id);
        }
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.item;
    }

    @Override
    public void build(Tile tile, Table table){
        for(ItemStack stack : items){
            table.add(new ReqImage(new ItemImage(stack.item.icon(Cicon.medium), stack.amount), () -> tile.entity != null && tile.entity.items != null && tile.entity.items.has(stack.item, stack.amount))).size(8 * 4).padRight(5);
        }
    }

    @Override
    public String getIcon(){
        return "icon-item";
    }

    @Override
    public void update(TileEntity entity){

    }

    @Override
    public void trigger(TileEntity entity){
        for(ItemStack stack : items){
            entity.items.remove(stack);
        }
    }

    @Override
    public boolean valid(TileEntity entity){
        return entity.items != null && entity.items.has(items);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(booster ? BlockStat.booster : BlockStat.input, new ItemListValue(items));
    }
}
