package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class LaserBulletType extends BulletType{
    protected static Tile furthest;

    protected Color[] colors = {Pal.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Pal.lancerLaser, Color.white};
    protected Effect laserEffect = Fx.lancerLaserShootSmoke;
    protected float length = 160f;
    protected float width = 15f;
    protected float lengthFalloff = 0.5f;
    protected float sideLength = 29f, sideWidth = 0.7f;
    protected float sideAngle = 90f;

    public LaserBulletType(float damage){
        super(0.01f, damage);

        keepVelocity = false;
        hitEffect = Fx.hitLancer;
        despawnEffect = Fx.none;
        shootEffect = Fx.hitLancer;
        smokeEffect = Fx.none;
        collides = false;
        hitSize = 4;
        lifetime = 16f;
        pierce = true;
        hittable = false;
    }

    public LaserBulletType(){
        this(1f);
    }

    @Override
    public float range(){
        return length;
    }

    @Override
    public void init(Bullet b){
        float resultLength = Damage.collideLaser(b, length);
        laserEffect.at(b.x, b.y, b.rotation(), resultLength * 0.75f);
    }

    @Override
    public void draw(Bullet b){
        float realLength = b.fdata;

        float f = Mathf.curve(b.fin(), 0f, 0.2f);
        float baseLen = realLength * f;
        float cwidth = width;
        float compound = 1f;

        Lines.lineAngle(b.x, b.y, b.rotation(), baseLen);
        for(Color color : colors){
            Draw.color(color);
            Lines.stroke((cwidth *= lengthFalloff) * b.fout());
            Lines.lineAngle(b.x, b.y, b.rotation(), baseLen, false);
            Tmp.v1.trns(b.rotation(), baseLen);
            Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Lines.getStroke() * 1.22f, cwidth * 2f + width / 2f, b.rotation());

            Fill.circle(b.x, b.y, 1f * cwidth * b.fout());
            for(int i : Mathf.signs){
                Drawf.tri(b.x, b.y, sideWidth * b.fout() * cwidth, sideLength * compound, b.rotation() + sideAngle * i);
            }

            compound *= lengthFalloff;
        }
        Draw.reset();

        Tmp.v1.trns(b.rotation(), baseLen * 1.1f);
        Drawf.light(b.team, b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, width * 1.4f * b.fout(), colors[0], 0.6f);
    }

    @Override
    public void drawLight(Bullet b){
        //no light drawn here
    }
}
