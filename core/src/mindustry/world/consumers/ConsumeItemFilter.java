package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ConsumeItemFilter extends Consume{
    public final Boolf<Item> filter;

    public ConsumeItemFilter(Boolf<Item> item){
        this.filter = item;
    }

    @Override
    public void apply(Block block){
        block.hasItems = true;
        block.acceptsItems = true;
        content.items().each(filter, item -> block.itemFilter[item.id] = true);
    }

    @Override
    public void build(Building build, Table table){
        MultiReqImage image = new MultiReqImage();
        content.items().each(i -> filter.get(i) && i.unlockedNow(), item -> image.add(new ReqImage(new ItemImage(item.uiIcon, 1),
            () -> build.items != null && build.items.has(item))));

        table.add(image).size(8 * 4);
    }

    @Override
    public void update(Building build){
    }

    @Override
    public void trigger(Building build){
        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(build.items != null && build.items.has(item) && this.filter.get(item)){
                build.items.remove(item, 1);
                build.filterConsItem = item;
                break;
            }
        }
    }

    @Override
    public boolean valid(Building build){
        if(build.consumeTriggerValid()) return true;

        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(this.filter.get(item) && build.items.has(item)){
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
