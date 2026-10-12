package math;

import arc.util.*;
import arc.util.noise.*;
import org.junit.*;

public class NoiseTest{

    @Test
    public void perfcomp(){

        int iterations = 1000000;

        for(int i = 0; i < iterations; i++){
            Simplex.raw2d(1, Math.random(), Math.random());
            Noise.rawNoise(Math.random(), Math.random());
        }

        Time.mark();

        for(int i = 0; i < iterations; i++){
            Noise.rawNoise(Math.random(), Math.random());
        }

        Log.info("Perlin: @", Time.elapsed());

        Time.mark();

        for(int i = 0; i < iterations; i++){
            Simplex.raw2d(1, Math.random(), Math.random());
        }

        Log.info("Simplex: @", Time.elapsed());
    }
}
