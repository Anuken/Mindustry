package mindustry.game;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class Stats{
    /** export window size in seconds */
    private static final int exportWindow = 60;
    /** refresh period of export in ticks */
    private static final float refreshPeriod = 60;

    /** Total items delivered to global resoure counter. Campaign only. */
    public ObjectIntMap<Item> itemsDelivered = new ObjectIntMap<>();
    /** Enemy (red team) units destroyed. */
    public int enemyUnitsDestroyed;
    /** Total waves lasted. */
    public int wavesLasted;
    /** Total (ms) time lasted in this save/zone. */
    public long timeLasted;
    /** Friendly buildings fully built. */
    public int buildingsBuilt;
    /** Friendly buildings fully deconstructed. */
    public int buildingsDeconstructed;
    /** Friendly buildings destroyed. */
    public int buildingsDestroyed;
    /** Export statistics. */
    public ObjectMap<Item, ExportStat> export = new ObjectMap<>();

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
        export.getOr(item, ExportStat::new).counter += amount;
    }

    /** Subtracts from export statistics. */
    public void handleItemImport(Item item, int amount){
        export.getOr(item, ExportStat::new).counter -= amount;
    }

    public float getExport(Item item){
        return export.getOr(item, ExportStat::new).mean;
    }

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

    public RankResult calculateRank(Sector zone, boolean launched){
        float score = 0;

        //TODO implement wave/attack mode based score
        /*
        if(launched && zone.getRules().attackMode){
            score += 3f;
        }else if(wavesLasted >= zone.conditionWave){
            //each new launch period adds onto the rank 'points'
            score += (float)((wavesLasted - zone.conditionWave) / zone.launchPeriod + 1) * 1.2f;
        }*/

        //TODO implement
        int capacity = 3000;//zone.loadout.findCore().itemCapacity;

        //weigh used fractions
        float frac = 0f;
        Array<Item> obtainable = Array.with(zone.data.resources).select(i -> i instanceof Item && ((Item)i).type == ItemType.material).as(Item.class);
        for(Item item : obtainable){
            frac += Mathf.clamp((float)itemsDelivered.get(item, 0) / capacity) / (float)obtainable.size;
        }

        score += frac * 1.6f;

        if(!launched){
            score *= 0.5f;
        }

        int rankIndex = Mathf.clamp((int)(score), 0, Rank.values().length - 1);
        Rank rank = Rank.values()[rankIndex];
        String sign = Math.abs((rankIndex + 0.5f) - score) < 0.2f || rank.name().contains("S") ? "" : (rankIndex + 0.5f) < score ? "-" : "+";

        return new RankResult(rank, sign);
    }

    public static class RankResult{
        public final Rank rank;
        /** + or - */
        public final String modifier;

        public RankResult(Rank rank, String modifier){
            this.rank = rank;
            this.modifier = modifier;
        }
    }


    public enum Rank{
        F, D, C, B, A, S, SS
    }

    public static class ExportStat{
        public transient float counter;
        public transient WindowedMean means = new WindowedMean(exportWindow);
        public transient boolean loaded;
        public float mean;
    }
}
