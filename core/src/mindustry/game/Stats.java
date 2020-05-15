package mindustry.game;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;

import java.util.*;

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

    /** Item production means. Holds means per period. */
    private transient WindowedMean[] itemExport = new WindowedMean[content.items().size];
    /** Counters of items recieved this period. */
    private transient float[] itemCounters = new float[content.items().size];
    /** Counter refresh state. */
    private transient Interval time = new Interval();

    public Stats(){
        for(int i = 0; i < itemExport.length; i++){
            itemExport[i] = new WindowedMean(exportWindow);
        }
    }

    /** Updates export statistics. */
    public void handleItemExport(ItemStack stack){
        itemCounters[stack.item.id] += stack.amount;
    }

    public void update(){

        //refresh throughput
        if(time.get(refreshPeriod)){
            for(int i = 0; i < itemCounters.length; i++){
                itemExport[i].add(itemCounters[i]);
            }

            Arrays.fill(itemCounters, 0);
        }
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
}
