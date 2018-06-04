package io.anuke.mindustry.world;

public enum BlockFlag {
    /**General important target for all types of units.*/
    target(0),
    /**Point to resupply resources.*/
    resupplyPoint(Float.MAX_VALUE),
    /**Point to drop off resources.*/
    dropPoint(Float.MAX_VALUE),
    /**Producer of important goods.*/
    producer(20),
    /**Producer or storage unit of volatile materials.*/
    explosive(10),
    /**Repair point.*/
    repair(Float.MAX_VALUE);

    public final float cost;

    BlockFlag(float cost){
        if(cost < 0) throw new RuntimeException("Block flag costs cannot be < 0!");
        this.cost = cost;
    }
}
