package mindustry.world.meta;

import arc.struct.*;
import arc.struct.ObjectMap.*;
import mindustry.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.values.*;

/** Hold and organizes a list of block stats. */
public class BlockStats{
    private final OrderedMap<StatCategory, OrderedMap<BlockStat, Seq<StatValue>>> map = new OrderedMap<>();
    private boolean dirty;

    /** Adds a single float value with this stat, formatted to 2 decimal places. */
    public void add(BlockStat stat, float value, StatUnit unit){
        add(stat, new NumberValue(value, unit));
    }

    /** Adds a single y/n boolean value. */
    public void add(BlockStat stat, boolean value){
        add(stat, new BooleanValue(value));
    }

    /** Adds an item value. */
    public void add(BlockStat stat, Item item){
        add(stat, new ItemListValue(new ItemStack(item, 1)));
    }

    /** Adds an item value. */
    public void add(BlockStat stat, ItemStack item){
        add(stat, new ItemListValue(item));
    }

    /** Adds an item value. */
    public void add(BlockStat stat, Liquid liquid, float amount, boolean perSecond){
        add(stat, new LiquidValue(liquid, amount, perSecond));
    }

    public void add(BlockStat stat, Attribute attr){
        add(stat, attr, 1f);
    }

    public void add(BlockStat stat, Attribute attr, float scale){
        for(Block block : Vars.content.blocks()){
            if(!block.isFloor() || block.asFloor().attributes.get(attr) == 0) continue;
            add(stat, new FloorEfficiencyValue(block.asFloor(), block.asFloor().attributes.get(attr) * scale));
        }
    }

    /** Adds a single string value with this stat. */
    public void add(BlockStat stat, String format, Object... args){
        add(stat, new StringValue(format, args));
    }

    /** Adds a stat value. */
    public void add(BlockStat stat, StatValue value){
        if(!map.containsKey(stat.category)){
            map.put(stat.category, new OrderedMap<>());
        }

        map.get(stat.category).get(stat, Seq::new).add(value);

        dirty = true;
    }

    /** Removes a stat, if it exists. */
    public void remove(BlockStat stat){
        if(!map.containsKey(stat.category) || !map.get(stat.category).containsKey(stat)){
            throw new RuntimeException("No stat entry found: \"" + stat + "\" in block.");
        }

        map.get(stat.category).remove(stat);

        dirty = true;
    }

    public OrderedMap<StatCategory, OrderedMap<BlockStat, Seq<StatValue>>> toMap(){
        //sort stats by index if they've been modified
        if(dirty){
            map.orderedKeys().sort();
            for(Entry<StatCategory, OrderedMap<BlockStat, Seq<StatValue>>> entry : map.entries()){
                entry.value.orderedKeys().sort();
            }

            dirty = false;
        }
        return map;
    }
}
