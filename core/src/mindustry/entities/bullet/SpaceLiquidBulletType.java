package mindustry.entities.bullet;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;

//this should probably be an effect?
public class SpaceLiquidBulletType extends BulletType{
    public float orbSize = 5.5f;

    public SpaceLiquidBulletType(){
        super(3.5f, 0);

        collides = false;
        lifetime = 90f;
        despawnEffect = Fx.none;
        hitEffect = Fx.none;
        smokeEffect = Fx.none;
        shootEffect = Fx.none;
        drag = 0.002f;
        hittable = false;
    }

    @Override
    public void draw(Bullet b){
        super.draw(b);

        if(!(b.data instanceof Liquid liquid)) return;

        Draw.color(liquid.color);
        Fill.circle(b.x, b.y, Interp.pow3Out.apply(b.fslope()) * orbSize);

        Draw.reset();
    }
}
