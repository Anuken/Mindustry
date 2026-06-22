package mindustry.entities.effect;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.*;

/** Effect that renders noise layers over the camera view. */
public class NoiseEffect extends Effect{
    public String noisePath = "sprites/distortAlpha.png";
    public @Nullable Color color;
    public float noiseScl = 1000f;
    public float opacity = 0.3f;
    public float baseSpeed = 0.4f;
    public float intensity = 1f;
    public float windX = 1f;
    public float windY = 0f;
    public int layers = 4;
    public float layerSpeedMul = -1.3f;
    public float layerAlphaMul = 0.7f;
    public float layerSclMul = 0.8f;
    public float layerColorMul = 0.9f;
    public Texture tex = Core.assets.getOrNull(noisePath, Texture.class);

    @Override
    public void render(EffectContainer e){
        if(tex == null) return;

        Color col = Tmp.c2.set(color != null ? color : e.color).mul(1f, 1f, 1f, e.fout());
        drawNoiseLayers(tex, col, noiseScl, opacity, baseSpeed, intensity, windX, windY, layers, layerSpeedMul, layerAlphaMul, layerSclMul, layerColorMul);
        Draw.reset();
    }

    public static void drawNoiseLayers(Texture noise, Color color, float noisescl, float opacity, float baseSpeed, float intensity, float vwindx, float vwindy,
                                       int layers, float layerSpeedM, float layerAlphaM, float layerSclM, float layerColorM){
        float sspeed = 1f, sscl = 1f, salpha = 1f, offset = 0f;
        Color col = Tmp.c1.set(color);
        for(int i = 0; i < layers; i++){
            drawNoise(noise, col, noisescl * sscl, salpha * opacity, sspeed * baseSpeed, intensity, vwindx, vwindy, offset);
            sspeed *= layerSpeedM;
            salpha *= layerAlphaM;
            sscl *= layerSclM;
            offset += 0.29f;
            col.mul(layerColorM);
        }
    }

    public static void drawNoise(Texture noise, Color color, float noisescl, float opacity, float baseSpeed, float intensity, float vwindx, float vwindy, float offset){
        Draw.alpha(opacity);
        Draw.tint(color);

        float speed = baseSpeed * intensity;
        float windx = vwindx * speed, windy = vwindy * speed;

        float scale  = 1f / noisescl;
        float scroll = Time.time * scale + offset;
        Tmp.tr1.texture = noise;
        Core.camera.bounds(Tmp.r1);
        Tmp.tr1.set(Tmp.r1.x * scale, Tmp.r1.y * scale, (Tmp.r1.x + Tmp.r1.width) * scale, (Tmp.r1.y + Tmp.r1.height) * scale);
        Tmp.tr1.scroll(-windx * scroll, -windy * scroll);
        Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, -Core.camera.height);
    }
}