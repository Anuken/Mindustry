package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.graphics.Shaders.*;
import mindustry.type.*;

public class SunMesh extends ShaderSphereMesh{
    public int octaves = 5;
    public float falloff = 0.5f, scale = 1f, power = 1.3f, magnitude = 0.6f, speed = 99999999999f, spread = 1.3f, seed = Mathf.random(9999f);
    public Texture colors;

    public SunMesh(Planet planet, int divisions){
        super(planet, Shaders.sun, divisions);
    }

    public void setColors(float scl, Color... colors){
        Pixmap pix = new Pixmap(colors.length, 1);
        for(int i = 0; i < colors.length; i++){
            pix.draw(i, 0, Tmp.c1.set(colors[i]).mul(scl));
        }
        this.colors = new Texture(pix);
        pix.dispose();
    }

    @Override
    public void preRender(){
        SunShader s = (SunShader)shader;
        s.octaves = octaves;
        s.falloff = falloff;
        s.scale = scale;
        s.power = power;
        s.magnitude = magnitude;
        s.speed = speed;
        s.seed = seed;
        s.colors = colors;
    }

    @Override
    public void dispose(){
        super.dispose();
        colors.dispose();
    }
}
