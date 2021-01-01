package mindustry.entities.bullet;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.content.*;

public class LaserBoltBulletType extends BasicBulletType{
    public float width = 2f, height = 7f;

    public LaserBoltBulletType(float speed, float damage){
        super(speed, damage);

        smokeEffect = Fx.hitLaser;
        hitEffect = Fx.hitLaser;
        despawnEffect = Fx.hitLaser;
        hittable = false;
        reflectable = false;
    }

    public LaserBoltBulletType(){
        this(1f, 1f);
    }

    @Override
    public void draw(Bullet b){
        Draw.color(backColor);
        Lines.stroke(width);
        Lines.lineAngleCenter(b.x, b.y, b.rotation(), height);
        Draw.color(frontColor);
        Lines.lineAngleCenter(b.x, b.y, b.rotation(), height / 2f);
        Draw.reset();
    }
}
