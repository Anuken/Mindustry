package io.anuke.mindustry.world.meta;

import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

/**Hold and organizes a list of block stats.*/
public class BlockStats {
    //TODO change back to true
    private static final boolean errorWhenMissing = false;

    private OrderedMap<StatCategory, OrderedMap<BlockStat, String>> map = new OrderedMap<>();
    private boolean dirty;

    /**Adds a single integer value with this stat.*/
    public void add(BlockStat stat, int value){
        add(stat, "" + value);
    }

    /**Adds a single float value with this stat, formatted to 2 decimal places.*/
    public void add(BlockStat stat, float value){
        add(stat, Strings.toFixed(value, 2));
    }

    /**Adds a formatted string with this stat.*/
    public void add(BlockStat stat, String format, Object... arguments){
        if(!Bundles.has("text.blocks." + stat.name().toLowerCase())){
            if(!errorWhenMissing){
                Log.err("Warning: No bundle entry for stat type \"" + stat + "\"!");
            }else{
                throw new RuntimeException("No bundle entry for stat type \"" + stat + "\"!");
            }
        }

        if(map.containsKey(stat.category) && map.get(stat.category).containsKey(stat)){
            throw new RuntimeException("Duplicate stat entry: \"" +stat + "\"");
        }

        if(!map.containsKey(stat.category)){
            map.put(stat.category, new OrderedMap<>());
        }

        map.get(stat.category).put(stat, Strings.formatArgs(format, arguments));

        dirty = true;
    }

    public void remove(BlockStat stat){
        if(!map.containsKey(stat.category) || !map.get(stat.category).containsKey(stat)){
            throw new RuntimeException("No stat entry found: \"" + stat + "\"!");
        }

        map.get(stat.category).remove(stat);

        dirty = true;
    }

    public OrderedMap<StatCategory, OrderedMap<BlockStat, String>> toMap() {
        //sort stats by index if they've been modified
        if(dirty) {
            map.orderedKeys().sort();
            for (Entry<StatCategory, OrderedMap<BlockStat, String>> entry : map.entries()) {
                entry.value.orderedKeys().sort();
            }

            dirty = false;
        }
        return map;
    }
}
