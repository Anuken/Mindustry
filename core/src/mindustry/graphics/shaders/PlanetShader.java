package mindustry.graphics.shaders;

import arc.graphics.*;
import arc.math.geom.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class PlanetShader extends LoadShader{
    public Vec3 lightDir = new Vec3(1, 1, 1).nor();
    public Color ambientColor = Color.white.cpy();
    public Vec3 camDir = new Vec3();
    public Vec3 camPos = new Vec3();
    public boolean emissive;
    public Planet planet;

    public PlanetShader(){
        super("planet", "planet");
    }

    @Override
    public void apply(){
        camDir.set(renderer.planets.cam.direction).rotate(Vec3.Y, planet.getRotation());

        setUniformf("u_lightdir", lightDir);
        setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);
        setUniformf("u_camdir", camDir);
        setUniformf("u_campos", renderer.planets.cam.position);
        setUniformf("u_emissive", emissive ? 1f : 0f);
    }
}
