package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

//TODO
public class ConsumeItemDynamic extends Consume{
    public final @NonNull Func<Tilec, ItemStack[]> items;

    public ConsumeItemDynamic(Func<Tilec, ItemStack[]> items){
        this.items = items;
    }

    @Override
    public void applyItemFilter(Bits filter){
        //this must be done dynamically
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.item;
    }

    @Override
    public void build(Tilec tile, Table table){
        for(ItemStack stack : items.get(tile)){
            table.add(new ReqImage(new ItemImage(stack.item.icon(Cicon.medium), stack.amount),
            () -> tile.items() != null && tile.items().has(stack.item, stack.amount))).size(8 * 4).padRight(5);
        }
    }

    @Override
    public String getIcon(){
        return "icon-item";
    }

    @Override
    public void update(Tilec entity){

    }

    @Override
    public void trigger(Tilec entity){
        for(ItemStack stack : items.get(entity)){
            entity.items().remove(stack);
        }
    }

    @Override
    public boolean valid(Tilec entity){
        return entity.items() != null && entity.items().has(items.get(entity));
    }

    @Override
    public void display(BlockStats stats){
        //TODO
        //stats.add(booster ? BlockStat.booster : BlockStat.input, new ItemListValue(items));
    }
}
