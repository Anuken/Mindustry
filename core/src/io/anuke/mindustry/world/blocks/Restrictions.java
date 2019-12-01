package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.meta.*;

public class Restrictions{
    private float[] array = new float[Restriction.values().length];

    public boolean get(Restriction attr){
        return array[attr.ordinal()] == 1;
    }

    public void add(Restriction attr){
        array[attr.ordinal()] = 1;
    }
}
