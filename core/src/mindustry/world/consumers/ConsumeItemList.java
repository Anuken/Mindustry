package mindustry.world.consumers;

import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;

public class ConsumeItemList extends ConsumeItemFilter{
    public Seq<Item> items = new Seq<>();
    public ObjectFloatMap<Item> efficiencyMultipliers = new ObjectFloatMap<>();

    public ConsumeItemList(Item... items){
        this();
        this.items = Seq.with(items);
    }

    public ConsumeItemList(){
        filter = item -> this.items.contains(item);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var item = getConsumed(build);
        return efficiencyMultipliers.get(item, 1f);
    }
}
