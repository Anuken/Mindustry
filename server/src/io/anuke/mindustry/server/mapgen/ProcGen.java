package io.anuke.mindustry.server.mapgen;

import io.anuke.mindustry.world.Map;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;

public class ProcGen {
    public RidgedPerlin rid = new RidgedPerlin(1, 1, 1);
    public Simplex sim = new Simplex();

    public Map generate(GenProperties props){
        return null;
    }
}
