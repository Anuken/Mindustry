package mindustry.entities.bullet;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.effect.*;
import mindustry.entities.type.*;
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
    }

    @Override
    public void draw(Bullet b){
    }

    @Override
    public void init(Bullet b){
        Lightning.create(b.getTeam(), lightningColor, damage, b.x, b.y, b.rot(), lightningLength);
    }
}
