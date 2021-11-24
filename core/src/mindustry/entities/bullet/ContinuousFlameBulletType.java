package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

//TODO implement
public class ContinuousFlameBulletType extends ContinuousBulletType{
    public float fadeTime = 16f;
    public float lightStroke = 40f;
    public float width = 3.7f, oscScl = 1.2f, oscMag = 0.02f;
    public int divisions = 25;

    /** Lengths, widths, ellipse panning, and offsets, all as fractions of the base width and length. Stored as an 'interleaved' array of values: LWPO1 LWPO2 LWPO3... */
    public float[] lengthWidthPanOffsets = {
        1.12f, 1.3f, 0.32f, 0f,
        1f, 1f, 0.3f, 0f,
        0.8f, 0.9f, 0.2f, 0.01f,
        0.5f, 0.8f, 0.15f, 0.02f,
        0.25f, 0.7f, 0.1f, 0.03f
    };

    public Color[] colors = {Color.valueOf("eb7abe").a(0.55f), Color.valueOf("e189f5").a(0.7f), Color.valueOf("907ef7").a(0.8f), Color.valueOf("91a4ff"), Color.white};

    public ContinuousFlameBulletType(float damage){
        this.damage = damage;
    }

    public ContinuousFlameBulletType(){
    }

    {
        length = 120f;
        hitEffect = Fx.hitBeam;
        hitSize = 4;
        drawSize = 420f;
        lifetime = 16f;
        hitColor = colors[3];
        lightColor = colors[3];
    }

    @Override
    public void draw(Bullet b){
        float realLength = Damage.findLaserLength(b, length);
        float fout = Mathf.clamp(b.time > b.lifetime - fadeTime ? 1f - (b.time - (lifetime - fadeTime)) / fadeTime : 1f);
        float baseLen = realLength * fout;

        float sin = Mathf.sin(Time.time, oscScl, oscMag);

        for(int i = 0; i < colors.length; i++){
            Draw.color(colors[i].write(Tmp.c1).mul(0.9f).mul(1f + Mathf.absin(Time.time, 1f, 0.1f)));
            Drawf.flame(b.x, b.y, divisions, b.rotation(),
                baseLen * lengthWidthPanOffsets[i * 4] * (1f - sin),
                width * lengthWidthPanOffsets[i * 4 + 1] * fout * (1f + sin),
                lengthWidthPanOffsets[i * 4 + 2],
                baseLen * lengthWidthPanOffsets[i * 4 + 3]
            );
        }

        Drawf.light(b.team, b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, lightStroke, lightColor, 0.7f);
        Draw.reset();
    }

    @Override
    public void drawLight(Bullet b){
        //no light drawn here
    }

}
