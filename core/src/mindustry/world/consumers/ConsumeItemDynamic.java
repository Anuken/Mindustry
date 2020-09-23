package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class ConsumeItemDynamic extends Consume{
    public final @NonNull Func<Building, ItemStack[]> items;

    public <T extends Building> ConsumeItemDynamic(Func<T, ItemStack[]> items){
        this.items = (Func<Building, ItemStack[]>)items;
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
    public void build(Building tile, Table table){
        ItemStack[][] current = {items.get(tile)};

        table.table(cont -> {
            table.update(() -> {
                if(current[0] != items.get(tile)){
                    rebuild(tile, cont);
                    current[0] = items.get(tile);
                }
            });

            rebuild(tile, cont);
        });
    }

    private void rebuild(Building tile, Table table){
        table.clear();

        for(ItemStack stack : items.get(tile)){
            table.add(new ReqImage(new ItemImage(stack.item.icon(Cicon.medium), stack.amount),
            () -> tile.items != null && tile.items.has(stack.item, stack.amount))).padRight(8);
        }
    }

    @Override
    public String getIcon(){
        return "icon-item";
    }

    @Override
    public void update(Building entity){

    }

    @Override
    public void trigger(Building entity){
        for(ItemStack stack : items.get(entity)){
            entity.items.remove(stack);
        }
    }

    @Override
    public boolean valid(Building entity){
        return entity.items != null && entity.items.has(items.get(entity));
    }

    @Override
    public void display(BlockStats stats){
        //should be handled by the block
    }
}
