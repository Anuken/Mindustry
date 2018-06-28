package io.anuke.mindustry.world.meta;

import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.meta.values.ItemValue;
import io.anuke.mindustry.world.meta.values.LiquidValue;
import io.anuke.mindustry.world.meta.values.NumberValue;
import io.anuke.mindustry.world.meta.values.StringValue;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;

/**Hold and organizes a list of block stats.*/
public class BlockStats {
    private static final boolean errorWhenMissing = true;

    private OrderedMap<StatCategory, OrderedMap<BlockStat, StatValue>> map = new OrderedMap<>();
    private boolean dirty;

    /**Adds a single float value with this stat, formatted to 2 decimal places.*/
    public void add(BlockStat stat, float value, StatUnit unit){
        add(stat, new NumberValue(value, unit));
    }

    /**Adds an item value.*/
    public void add(BlockStat stat, Item item){
        add(stat, new ItemValue(new ItemStack(item, 1)));
    }

    /**Adds a liquid value.*/
    public void add(BlockStat stat, Liquid liquid){
        add(stat, new LiquidValue(liquid));
    }


    /**Adds an item value.*/
    public void add(BlockStat stat, ItemStack item){
        add(stat, new ItemValue(item));
    }

    /**Adds a single string value with this stat.*/
    public void add(BlockStat stat, String format, Object... args){
        add(stat, new StringValue(format, args));
    }

    /**Adds a stat value.*/
    public void add(BlockStat stat, StatValue value){
        if(!Bundles.has("text.blocks." + stat.name().toLowerCase())){
            if(!errorWhenMissing){
                Log.err("Warning: No bundle entry for stat type \"" + stat + "\"!");
            }else{
                throw new RuntimeException("No bundle entry for stat type \"" + stat + "\"!");
            }
        }

        if(!Bundles.has("text.category." + stat.category.name().toLowerCase())){
            if(!errorWhenMissing){
                Log.err("Warning: No bundle entry for stat cateogry \"" + stat.category + "\"!");
            }else{
                throw new RuntimeException("No bundle entry for stat category \"" + stat.category + "\"!");
            }
        }

        if(map.containsKey(stat.category) && map.get(stat.category).containsKey(stat)){
            throw new RuntimeException("Duplicate stat entry: \"" +stat + "\"");
        }

        if(!map.containsKey(stat.category)){
            map.put(stat.category, new OrderedMap<>());
        }

        map.get(stat.category).put(stat, value);

        dirty = true;
    }

    public void remove(BlockStat stat){
        if(!map.containsKey(stat.category) || !map.get(stat.category).containsKey(stat)){
            throw new RuntimeException("No stat entry found: \"" + stat + "\"!");
        }

        map.get(stat.category).remove(stat);

        dirty = true;
    }

    public OrderedMap<StatCategory, OrderedMap<BlockStat, StatValue>> toMap() {
        //sort stats by index if they've been modified
        if(dirty) {
            map.orderedKeys().sort();
            for (Entry<StatCategory, OrderedMap<BlockStat, StatValue>> entry : map.entries()) {
                entry.value.orderedKeys().sort();
            }

            dirty = false;
        }
        return map;
    }
}
