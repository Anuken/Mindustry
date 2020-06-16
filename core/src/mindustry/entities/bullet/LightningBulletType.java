package mindustry.entities.bullet;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class LightningBulletType extends BulletType{
    protected Color lightningColor = Pal.lancerLaser;
    protected int lightningLength = 25;

    public LightningBulletType(){
        super(0.0001f, 1f);

        lifetime = 1;
        despawnEffect = Fx.none;
        hitEffect = Fx.hitLancer;
        keepVelocity = false;
        hittable = false;
    }

    @Override
    public float range(){
        return lightningLength * 2.33f;
    }

    @Override
    public void draw(Bulletc b){
    }

    @Override
    public void init(Bulletc b){
        Lightning.create(b.team(), lightningColor, damage, b.x(), b.y(), b.rotation(), lightningLength);
    }
}
