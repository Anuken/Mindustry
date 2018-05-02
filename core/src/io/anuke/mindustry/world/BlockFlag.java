package io.anuke.mindustry.world;

public enum BlockFlag {
    resupplyPoint(0),
    producer(Float.MAX_VALUE),
    repair(Float.MAX_VALUE);

    public final float cost;

    BlockFlag(float cost){
        if(cost < 0) throw new RuntimeException("Block flag costs cannot be < 0!");
        this.cost = cost;
    }
}
