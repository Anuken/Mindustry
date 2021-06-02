package mindustry.game;

import arc.math.*;
import arc.struct.*;
import mindustry.type.*;

//TODO more stats:
//- units constructed
public class GameStats{
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

    //unused
    public RankResult calculateRank(Sector sector){
        float score = 0;

        int rankIndex = Mathf.clamp((int)score, 0, Rank.all.length - 1);
        Rank rank = Rank.all[rankIndex];
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
        F, D, C, B, A, S, SS;

        public static final Rank[] all = values();
    }
}
