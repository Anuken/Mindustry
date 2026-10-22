package mindustry.graphics.shaders;

import arc.graphics.*;
import arc.math.geom.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class CloudShader extends LoadShader{
    public Vec3 lightDir = new Vec3(1, 1, 1).nor();
    public Color ambientColor = Color.white.cpy();
    public Vec3 camDir = new Vec3();
    public float alpha = 1f;
    public Planet planet;

    public CloudShader(){
        super("planet", "clouds");
    }

    @Override
    public void apply(){
        camDir.set(renderer.planets.cam.direction).rotate(Vec3.Y, planet.getRotation());

        setUniformf("u_alpha", alpha);
        setUniformf("u_emissive", 0f);
        setUniformf("u_lightdir", lightDir);
        setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);
    }
}
