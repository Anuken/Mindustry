package io.anuke.ucore.util;

import com.badlogic.gdx.math.RandomXS128;

public class SeedRandom extends RandomXS128{

    public SeedRandom(long seed){
        super(seed);
    }

    public float range(float amount){
        return nextFloat() * amount * 2 - amount;
    }

    public float random(float min, float max){
        return min + (max - min) * nextFloat();
    }

    public int range(int amount){
        return nextInt(amount * 2 + 1) - amount;
    }

    public int random(int min, int max){
        return min + nextInt(max - min + 1);
    }
}
