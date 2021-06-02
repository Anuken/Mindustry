package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ConsumeItemFilter extends Consume{
    public final Boolf<Item> filter;

    public ConsumeItemFilter(Boolf<Item> item){
        this.filter = item;
    }

    @Override
    public void applyItemFilter(Bits arr){
        content.items().each(filter, item -> arr.set(item.id));
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.item;
    }

    @Override
    public void build(Building tile, Table table){
        MultiReqImage image = new MultiReqImage();
        content.items().each(i -> filter.get(i) && i.unlockedNow(), item -> image.add(new ReqImage(new ItemImage(item.uiIcon, 1),
            () -> tile.items != null && tile.items.has(item))));

        table.add(image).size(8 * 4);
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
        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(entity.items != null && entity.items.has(item) && this.filter.get(item)){
                entity.items.remove(item, 1);
                break;
            }
        }
    }

    @Override
    public boolean valid(Building entity){
        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(entity.items != null && entity.items.has(item) && this.filter.get(item)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, stats.timePeriod < 0 ? StatValues.items(filter) : StatValues.items(stats.timePeriod, filter));
    }
}
