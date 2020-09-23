package mindustry.game;

import arc.math.*;
import arc.struct.*;
import mindustry.type.*;

//TODO more stats:
//- units constructed
public class Stats{
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
        Seq<Item> obtainable = zone.save == null ? new Seq<>() : zone.save.meta.secinfo.resources.select(i -> i instanceof Item).as();
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
