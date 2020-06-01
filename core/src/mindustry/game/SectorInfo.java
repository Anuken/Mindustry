package mindustry.game;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class SectorInfo{
    /** export window size in seconds */
    private static final int exportWindow = 60;
    /** refresh period of export in ticks */
    private static final float refreshPeriod = 60;
    /** Export statistics. */
    public ObjectMap<Item, ExportStat> export = new ObjectMap<>();
    /** Items stored in all cores. */
    public ObjectIntMap<Item> coreItems = new ObjectIntMap<>();
    /** The best available core type. */
    public Block bestCoreType = Blocks.air;
    /** Max storage capacity. */
    public int storageCapacity = 0;
    /** Whether a core is available here. */
    public boolean hasCore = true;

    /** Counter refresh state. */
    private transient Interval time = new Interval();
    /** Core item storage to prevent spoofing. */
    private transient int[] lastCoreItems;

    /** Updates export statistics. */
    public void handleItemExport(ItemStack stack){
        handleItemExport(stack.item, stack.amount);
    }

    /** Updates export statistics. */
    public void handleItemExport(Item item, int amount){
        export.get(item, ExportStat::new).counter += amount;
    }

    /** Subtracts from export statistics. */
    public void handleItemImport(Item item, int amount){
        export.get(item, ExportStat::new).counter -= amount;
    }

    public float getExport(Item item){
        return export.get(item, ExportStat::new).mean;
    }

    /** Prepare data for writing to a save. */
    public void prepare(){
        //update core items
        coreItems.clear();

        CoreEntity entity = state.rules.defaultTeam.core();

        if(entity != null){
            ItemModule items = entity.items;
            for(int i = 0; i < items.length(); i++){
                coreItems.put(content.item(i), items.get(i));
            }
        }

        hasCore = entity != null;
        bestCoreType = !hasCore ? Blocks.air : state.rules.defaultTeam.cores().max(e -> e.block.size).block;
        storageCapacity = entity != null ? entity.storageCapacity : 0;
    }

    /** Update averages of various stats. */
    public void update(){
        //create last stored core items
        if(lastCoreItems == null){
            lastCoreItems = new int[content.items().size];
            updateCoreDeltas();
        }

        //refresh throughput
        if(time.get(refreshPeriod)){
            CoreEntity ent = state.rules.defaultTeam.core();

            export.each((item, stat) -> {
                //initialize stat after loading
                if(!stat.loaded){
                    stat.means.fill(stat.mean);
                    stat.loaded = true;
                }

                //how the resources changed - only interested in negative deltas, since that's what happens during spoofing
                int coreDelta = Math.min(ent == null ? 0 : ent.items.get(item) - lastCoreItems[item.id], 0);

                //add counter, subtract how many items were taken from the core during this time
                stat.means.add(Math.max(stat.counter + coreDelta, 0));
                stat.counter = 0;
                stat.mean = stat.means.rawMean();
            });

            updateCoreDeltas();
        }
    }

    private void updateCoreDeltas(){
        CoreEntity ent = state.rules.defaultTeam.core();
        for(int i = 0; i < lastCoreItems.length; i++){
            lastCoreItems[i] = ent == null ? 0 : ent.items.get(i);
        }
    }

    public ObjectFloatMap<Item> exportRates(){
        ObjectFloatMap<Item> map = new ObjectFloatMap<>();
        export.each((item, value) -> map.put(item, value.mean));
        return map;
    }

    public static class ExportStat{
        public transient float counter;
        public transient WindowedMean means = new WindowedMean(exportWindow);
        public transient boolean loaded;
        public float mean;
    }
}
