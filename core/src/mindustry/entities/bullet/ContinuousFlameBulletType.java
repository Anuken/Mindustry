package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;

public class ContinuousFlameBulletType extends ContinuousBulletType{
    public float lightStroke = 40f;
    public float width = 3.7f, oscScl = 1.2f, oscMag = 0.02f;
    public int divisions = 25;

    public boolean drawFlare = true;
    public Color flareColor = Color.valueOf("e189f5");
    public float flareWidth = 3f, flareInnerScl = 0.5f, flareLength = 40f, flareInnerLenScl = 0.5f, flareLayer = Layer.bullet - 0.0001f, flareRotSpeed = 1.2f;
    public boolean rotateFlare = false;
    public Interp lengthInterp = Interp.slope;

    /** Lengths, widths, ellipse panning, and offsets, all as fractions of the base width and length. Stored as an 'interleaved' array of values: LWPO1 LWPO2 LWPO3... */
    public float[] lengthWidthPans = {
        1.12f, 1.3f, 0.32f,
        1f, 1f, 0.3f,
        0.8f, 0.9f, 0.2f,
        0.5f, 0.8f, 0.15f,
        0.25f, 0.7f, 0.1f,
    };

    public Color[] colors = {Color.valueOf("eb7abe").a(0.55f), Color.valueOf("e189f5").a(0.7f), Color.valueOf("907ef7").a(0.8f), Color.valueOf("91a4ff"), Color.white.cpy()};

    public ContinuousFlameBulletType(float damage){
        this.damage = damage;
    }

    public ContinuousFlameBulletType(){
    }

    {
        optimalLifeFract = 0.5f;
        length = 120f;
        hitEffect = Fx.hitFlameBeam;
        hitSize = 4;
        drawSize = 420f;
        lifetime = 16f;
        hitColor = colors[1].cpy().a(1f);
        lightColor = hitColor;
        laserAbsorb = false;
        ammoMultiplier = 1f;
        pierceArmor = true;
    }

    @Override
    public void draw(Bullet b){
        float mult = b.fin(lengthInterp);
        float realLength = (pierceCap <= 0 ? length : Damage.findPierceLength(b, pierceCap, length)) * mult;

        float sin = Mathf.sin(Time.time, oscScl, oscMag);

        for(int i = 0; i < colors.length; i++){
            Draw.color(colors[i].write(Tmp.c1).mul(0.9f).mul(1f + Mathf.absin(Time.time, 1f, 0.1f)));
            Drawf.flame(b.x, b.y, divisions, b.rotation(),
                realLength * lengthWidthPans[i * 3] * (1f - sin),
                width * lengthWidthPans[i * 3 + 1] * mult * (1f + sin),
                lengthWidthPans[i * 3 + 2]
            );
        }

        if(drawFlare){
            color(flareColor);
            Draw.z(flareLayer);

            float angle = Time.time * flareRotSpeed + (rotateFlare ? b.rotation() : 0f);

            for(int i = 0; i < 4; i++){
                Drawf.tri(b.x, b.y, flareWidth, flareLength * (mult + sin), i*90 + 45 + angle);
            }

            color();
            for(int i = 0; i < 4; i++){
                Drawf.tri(b.x, b.y, flareWidth * flareInnerScl, flareLength * flareInnerLenScl * (mult + sin), i*90 + 45 + angle);
            }
        }

        Tmp.v1.trns(b.rotation(), realLength * 1.1f);
        Drawf.light(b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, lightStroke, lightColor, 0.7f);
        Draw.reset();
    }

    @Override
    public float currentLength(Bullet b){
        return length * b.fin(lengthInterp);
    }

    @Override
    public void drawLight(Bullet b){
        //no light drawn here
    }

}
