package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.*;
import mindustry.graphics.*;
import mindustry.graphics.Shaders.*;
import mindustry.type.*;

public class SunMesh extends ShaderSphereMesh{
    public int octaves = 5;
    public float falloff = 0.5f, scale = 1f, power = 1.3f, magnitude = 0.6f, speed = 99999999999f, spread = 1.3f, seed = Mathf.random(9999f);
    public float[] colorValues;

    public SunMesh(Planet planet, int divisions){
        super(planet, Shaders.sun, divisions);
    }

    public void setColors(Color... colors){
        colorValues = new float[colors.length*4];

        for(int i = 0; i < colors.length; i ++){
            colorValues[i*4] = colors[i].r;
            colorValues[i*4 + 1] = colors[i].g;
            colorValues[i*4 + 2] = colors[i].b;
            colorValues[i*4 + 3] = colors[i].a;
        }
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
        s.colorValues = colorValues;
    }
}
