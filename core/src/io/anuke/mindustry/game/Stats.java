package io.anuke.mindustry.game;

import io.anuke.annotations.Annotations.Serialize;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.mindustry.type.Item;

@Serialize
public class Stats{
    /**Items delivered to global resoure counter. Zones only.*/
    public ObjectIntMap<Item> itemsDelivered = new ObjectIntMap<>();
    /**Enemy (red team) units destroyed.*/
    public int enemyUnitsDestroyed;
    /**Total waves lasted.*/
    public int wavesLasted;
    /**Total (ms) time lasted in this save/zone.*/
    public long timeLasted;
    /**Friendly buildings fully built.*/
    public int buildingsBuilt;
    /**Friendly buildings fully deconstructed.*/
    public int buildingsDeconstructed;
    /**Friendly buildings destroyed.*/
    public int buildingsDestroyed;

    public Rank calculateRank(boolean launched){
        return Rank.F;
    }
}
