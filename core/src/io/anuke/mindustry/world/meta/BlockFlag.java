package io.anuke.mindustry.world.meta;

public enum BlockFlag{
    /**General important target for all types of units.*/
    target(0),
    /**Point to resupply resources.*/
    resupplyPoint(Float.MAX_VALUE),
    /**Point to drop off resources.*/
    dropPoint(Float.MAX_VALUE),
    /**Producer of important goods.*/
    producer(Float.MAX_VALUE),
    /**Just a turret.*/
    turret(Float.MAX_VALUE),
    /**Producer or storage unit of volatile materials.*/
    explosive(Float.MAX_VALUE),
    /**Repair point.*/
    repair(Float.MAX_VALUE),
    /**Special flag for command center blocks.*/
    comandCenter(Float.MAX_VALUE);

    public final static BlockFlag[] all = values();

    public final float cost;

    BlockFlag(float cost){
        if(cost < 0) throw new RuntimeException("Block flag costs cannot be < 0!");
        this.cost = cost;
    }
}
