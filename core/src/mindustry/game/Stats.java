package mindustry.game;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;

import static mindustry.Vars.content;

public class Stats{
    /** export window size in seconds */
    private static final int exportWindow = 60;
    /** refresh period of export in ticks */
    private static final float refreshPeriod = 60;

    /** Items delivered to global resoure counter. Zones only. */
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

    /** Counter refresh state. */
    private transient Interval time = new Interval();
    /** Export statistics. */
    public ObjectMap<Item, ProductionStat> production = new ObjectMap<>();

    /** Updates export statistics. */
    public void handleItemExport(ItemStack stack){
        production.getOr(stack.item, ProductionStat::new).counter += stack.amount;
    }

    public float getExport(Item item){
        return production.getOr(item, ProductionStat::new).mean;
    }

    public void update(){

        //refresh throughput
        if(time.get(refreshPeriod)){
            for(ProductionStat stat : production.values()){
                //initialize stat after loading
                if(!stat.loaded){
                    stat.means.fill(stat.mean);
                    stat.loaded = true;
                }

                stat.means.add(stat.counter);
                stat.counter = 0;
                stat.mean = stat.means.rawMean();
            }
        }
    }

    public ObjectFloatMap<Item> productionRates(){
        ObjectFloatMap<Item> map = new ObjectFloatMap<>();
        production.each((item, value) -> map.put(item, value.mean));
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

    public static class ProductionStat{
        public transient float counter;
        public transient WindowedMean means = new WindowedMean(content.items().size);
        public transient boolean loaded;
        public float mean;
    }
}
