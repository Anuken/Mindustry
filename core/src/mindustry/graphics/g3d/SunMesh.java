package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class SunMesh extends HexMesh{

    public SunMesh(Planet planet, int divisions, double octaves, double persistence, double scl, double pow, double mag, float colorScale, Color... colors){
        super(planet, new HexMesher(){

            @Override
            public float getHeight(Vec3 position){
                return 0;
            }

            @Override
            public Color getColor(Vec3 position){
                double height = Math.pow(Simplex.noise3d(0, octaves, persistence, scl, position.x, position.y, position.z), pow) * mag;
                return Tmp.c1.set(colors[Mathf.clamp((int)(height * colors.length), 0, colors.length - 1)]).mul(colorScale);
            }
        }, divisions, Shaders.unlit);
    }

    @Override
    public void preRender(){
        //do absolutely nothing
    }
}
