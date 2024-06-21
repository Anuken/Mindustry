package mindustry.world.consumers;

import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;

public class ConsumeItemList extends ConsumeItemFilter{
    public ObjectFloatMap<Item> itemMultipliers = new ObjectFloatMap<>();

    public ConsumeItemList(Item... items){
        this();
        for(Item i : items){
            itemMultipliers.put(i, 1f);
        }
    }

    public ConsumeItemList(){
        filter = item -> itemMultipliers.containsKey(item);
    }

    /** Initializes item efficiency multiplier map. Format: [item1, mult1, item2, mult2...] */
    public void setMultipliers(Object... objects){
        for(int i = 0; i < objects.length; i += 2){
            itemMultipliers.put((Item)objects[i], (Float)objects[i + 1]);
        }
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var item = getConsumed(build);
        return itemMultipliers.get(item, 1f);
    }
}
