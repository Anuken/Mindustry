package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ContinuousLaserBulletType extends BulletType{
    public float length = 220f;
    public float shake = 1f;
    public float fadeTime = 16f;
    public float lightStroke = 40f;
    public float spaceMag = 35f;
    public Color[] colors = {Color.valueOf("ec745855"), Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.white};
    public float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
    public float[] strokes = {2f, 1.5f, 1f, 0.3f};
    public float[] lenscales = {1f, 1.12f, 1.15f, 1.17f};
    public float width = 9f, oscScl = 0.8f, oscMag = 1.5f;
    public boolean largeHit = true;

    public ContinuousLaserBulletType(float damage){
        super(0.001f, damage);

        hitEffect = Fx.hitMeltdown;
        despawnEffect = Fx.none;
        hitSize = 4;
        drawSize = 420f;
        lifetime = 16f;
        hitColor = colors[2];
        incendAmount = 1;
        incendSpread = 5;
        incendChance = 0.4f;
        lightColor = Color.orange;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
    }

    protected ContinuousLaserBulletType(){
        this(0);
    }

    @Override
    public float estimateDPS(){
        //assume firing duration is about 100 by default, may not be accurate there's no way of knowing in this method
        //assume it pierces 3 blocks/units
        return damage * 100f / 5f * 3f;
    }

    @Override
    public float range(){
        return length;
    }

    @Override
    public void init(){
        super.init();

        drawSize = Math.max(drawSize, length*2f);
    }

    @Override
    public void update(Bullet b){

        //damage every 5 ticks
        if(b.timer(1, 5f)){
            Damage.collideLine(b, b.team, hitEffect, b.x, b.y, b.rotation(), length, largeHit);
        }

        if(shake > 0){
            Effect.shake(shake, shake, b);
        }
    }

    @Override
    public void draw(Bullet b){
        float realLength = Damage.findLaserLength(b, length);
        float fout = Mathf.clamp(b.time > b.lifetime - fadeTime ? 1f - (b.time - (lifetime - fadeTime)) / fadeTime : 1f);
        float baseLen = realLength * fout;

        Lines.lineAngle(b.x, b.y, b.rotation(), baseLen);
        for(int s = 0; s < colors.length; s++){
            Draw.color(Tmp.c1.set(colors[s]).mul(1f + Mathf.absin(Time.time, 1f, 0.1f)));
            for(int i = 0; i < tscales.length; i++){
                Tmp.v1.trns(b.rotation() + 180f, (lenscales[i] - 1f) * spaceMag);
                Lines.stroke((width + Mathf.absin(Time.time, oscScl, oscMag)) * fout * strokes[s] * tscales[i]);
                Lines.lineAngle(b.x + Tmp.v1.x, b.y + Tmp.v1.y, b.rotation(), baseLen * lenscales[i], false);
            }
        }

        Tmp.v1.trns(b.rotation(), baseLen * 1.1f);

        Drawf.light(b.team, b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, lightStroke, lightColor, 0.7f);
        Draw.reset();
    }

    @Override
    public void drawLight(Bullet b){
        //no light drawn here
    }

}
