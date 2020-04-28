package mindustry.graphics;

import arc.graphics.*;
import arc.math.*;
import arc.util.noise.*;

/** Generates a scorch pixmap based on parameters. Thread safe, unless multiple scorch generators are running in parallel. */
public class ScorchGenerator{
    private static final Simplex sim = new Simplex();

    public int size = 80, seed = 0, color = Color.white.rgba();
    public double scale = 18, pow = 2, octaves = 4, pers = 0.4, add = 2, nscl = 4.5f;

    public Pixmap generate(){
        Pixmap pix = new Pixmap(size, size);
        sim.setSeed(seed);

        pix.each((x, y) -> {
            double dst = Mathf.dst(x, y, size/2, size/2) / (size / 2f);
            double scaled = Math.abs(dst - 0.5f) * 5f + add;
            scaled -= noise(Angles.angle(x, y, size/2, size/2))*nscl;
            if(scaled < 1.5f) pix.draw(x, y, color);
        });

        return pix;
    }

    private double noise(float angle){
        return Math.pow(sim.octaveNoise2D(octaves, pers, 1 / scale, Angles.trnsx(angle, size/2f) + size/2f, Angles.trnsy(angle, size/2f) + size/2f), pow);
    }
}
