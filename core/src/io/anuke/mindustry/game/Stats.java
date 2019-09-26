package io.anuke.mindustry.game;

import io.anuke.annotations.Annotations.Serialize;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.type.*;

@Serialize
public class Stats{
    /** Items delivered to global resoure counter. Zones only. */
    public transient ObjectIntMap<Item> itemsDelivered = new ObjectIntMap<>();
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

    public RankResult calculateRank(Zone zone, boolean launched){
        float score = 0;

        if(launched && zone.getRules().attackMode){
            score += 3f;
        }else if(wavesLasted >= zone.conditionWave){
            //each new launch period adds onto the rank 'points'
            score += (float)((wavesLasted - zone.conditionWave) / zone.launchPeriod + 1) * 1.2f;
        }

        int capacity = zone.loadout.core().itemCapacity;

        //weigh used fractions
        float frac = 0f;
        Array<Item> obtainable = Array.with(zone.resources).select(i -> i.type == ItemType.material);
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
