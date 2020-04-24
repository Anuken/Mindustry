package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.renderer;

public class LaserBulletType extends BulletType{
    protected Color[] colors = {Pal.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Pal.lancerLaser, Color.white};
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
        smokeEffect = Fx.lancerLaserShootSmoke;
        hitSize = 4;
        lifetime = 16f;
        pierce = true;
    }

    public LaserBulletType(){
        this(1f);
    }

    @Override
    public float range(){
        return length;
    }

    @Override
    public void init(Bulletc b){
        Damage.collideLine(b, b.team(), hitEffect, b.x(), b.y(), b.rotation(), length);
    }

    @Override
    public void draw(Bulletc b){
        float f = Mathf.curve(b.fin(), 0f, 0.2f);
        float baseLen = length * f;
        float cwidth = width;
        float compound = 1f;

        Lines.lineAngle(b.x(), b.y(), b.rotation(), baseLen);
        Lines.precise(true);
        for(Color color : colors){
            Draw.color(color);
            Lines.stroke((cwidth *= lengthFalloff) * b.fout());
            Lines.lineAngle(b.x(), b.y(), b.rotation(), baseLen, CapStyle.none);
            Tmp.v1.trns(b.rotation(), baseLen);
            Drawf.tri(b.x() + Tmp.v1.x, b.y() + Tmp.v1.y, Lines.getStroke() * 1.22f, cwidth * 2f + width / 2f, b.rotation());

            Fill.circle(b.x(), b.y(), 1f * cwidth * b.fout());
            for(int i : Mathf.signs){
                Drawf.tri(b.x(), b.y(), sideWidth * b.fout() * cwidth, sideLength * compound, b.rotation() + sideAngle * i);
            }

            compound *= lengthFalloff;
        }
        Lines.precise(false);
        Draw.reset();

        Tmp.v1.trns(b.rotation(), baseLen * 1.1f);
        renderer.lights.line(b.x(), b.y(), b.x() + Tmp.v1.x, b.y() + Tmp.v1.y, width * 1.4f * b.fout(), colors[0], 0.6f);
    }
}
