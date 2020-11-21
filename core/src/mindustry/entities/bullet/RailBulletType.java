package mindustry.entities.bullet;

import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

//TODO this class is bad for multiple reasons, remove/replace it.
//- effects unreliable
//- not really hitscan but works like it
//- buggy trails
//- looks bad
//- generally unreliable
public class RailBulletType extends BulletType{
    public Effect pierceEffect = Fx.hitBulletSmall, updateEffect = Fx.none;
    /** Multiplier of damage decreased per health pierced. */
    public float pierceDamageFactor = 1f;

    public RailBulletType(){
        pierceBuilding = true;
        pierce = true;
        reflectable = false;
        hitEffect = Fx.none;
        despawnEffect = Fx.none;
    }

    void handle(Bullet b, Posc pos, float initialHealth){
        float sub = initialHealth*pierceDamageFactor;

        if(sub >= b.damage){
            //cause a despawn
            b.remove();
        }

        //subtract health from each consecutive pierce
        b.damage -= Math.min(b.damage, sub);

        if(b.damage > 0){
            pierceEffect.at(pos.getX(), pos.getY(), b.rotation());
        }

        hitEffect.at(pos.getX(), pos.getY());
    }

    @Override
    public void update(Bullet b){
        if(b.timer(1, 0.9f)){
            updateEffect.at(b.x, b.y, b.rotation());
        }
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float initialHealth){
        handle(b, entity, initialHealth);
    }

    @Override
    public void hitTile(Bullet b, Building build, float initialHealth, boolean direct){
        handle(b, build, initialHealth);
    }
}
