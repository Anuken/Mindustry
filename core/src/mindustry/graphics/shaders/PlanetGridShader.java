package mindustry.graphics.shaders;

import arc.math.geom.*;

public class PlanetGridShader extends LoadShader{
    public Vec3 mouse = new Vec3();

    public PlanetGridShader(){
        super("planetgrid", "planetgrid");
    }

    @Override
    public void apply(){
        setUniformf("u_mouse", mouse);
    }
}
