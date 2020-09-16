package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ShrapnelBulletType extends BulletType{
    public float length = 100f;
    public float width = 20f;
    public Color fromColor = Color.white, toColor = Pal.lancerLaser;

    public int serrations = 7;
    public float serrationLenScl = 10f, serrationWidth = 4f, serrationSpacing = 8f, serrationSpaceOffset = 80f, serrationFadeOffset = 0.5f;

    public ShrapnelBulletType(){
        speed = 0.01f;
        hitEffect = Fx.hitLancer;
        shootEffect = smokeEffect = Fx.lightningShoot;
        lifetime = 10f;
        despawnEffect = Fx.none;
        pierce = true;
        keepVelocity = false;
        hittable = false;
    }

    @Override
    public void init(Bullet b){
        Damage.collideLaser(b, length);
    }

    @Override
    public float range(){
        return length;
    }

    @Override
    public void draw(Bullet b){
        float realLength = b.fdata;

        Draw.color(fromColor, toColor, b.fin());
        for(int i = 0; i < (int)(serrations * realLength / length); i++){
            Tmp.v1.trns(b.rotation(), i * serrationSpacing);
            float sl = Mathf.clamp(b.fout() - serrationFadeOffset) * (serrationSpaceOffset - i * serrationLenScl);
            Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, serrationWidth, sl, b.rotation() + 90);
            Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, serrationWidth, sl, b.rotation() - 90);
        }
        Drawf.tri(b.x, b.y, width * b.fout(), (realLength + 50), b.rotation());
        Drawf.tri(b.x, b.y, width * b.fout(), 10f, b.rotation() + 180f);
        Draw.reset();
    }
}
