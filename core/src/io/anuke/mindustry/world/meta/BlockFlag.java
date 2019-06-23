package io.anuke.mindustry.world.meta;

public enum BlockFlag{
    /** General important target for all types of units. */
    target(0),
    /** Producer of important goods. */
    producer(Float.MAX_VALUE),
    /** A turret. */
    turret(Float.MAX_VALUE),
    /** Repair point. */
    repair(Float.MAX_VALUE);

    public final static BlockFlag[] all = values();
    public final float cost;

    BlockFlag(float cost){
        if(cost < 0) throw new RuntimeException("Block flag costs cannot be < 0!");
        this.cost = cost;
    }
}
