package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ConsumeItemFilter extends Consume{
    public Boolf<Item> filter = i -> false;

    public ConsumeItemFilter(Boolf<Item> item){
        this.filter = item;
    }

    public ConsumeItemFilter(){
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
            () -> build.items.has(item))));

        table.add(image).size(8 * 4);
    }

    @Override
    public void update(Building build){
    }

    @Override
    public void trigger(Building build){
        Item item = getConsumed(build);
        if(item != null){
            build.items.remove(item, 1);
        }
    }

    @Override
    public float efficiency(Building build){
        return build.consumeTriggerValid() || getConsumed(build) != null ? 1f : 0f;
    }

    public @Nullable Item getConsumed(Building build){
        for(int i = 0; i < content.items().size; i++){
            Item item = content.item(i);
            if(build.items.has(item) && this.filter.get(item)){
                return item;
            }
        }
        return null;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, stats.timePeriod < 0 ? StatValues.items(filter) : StatValues.items(stats.timePeriod, filter));
    }
}
