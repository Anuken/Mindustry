package mindustry.entities.bullet;

import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class FlakBulletType extends BasicBulletType{
    public float explodeRange = 30f;

    public FlakBulletType(float speed, float damage){
        super(speed, damage, "shell");
        splashDamage = 15f;
        splashDamageRadius = 34f;
        hitEffect = Fx.flakExplosionBig;
        width = 8f;
        height = 10f;
        collidesGround = false;
    }

    public FlakBulletType(){
        this(1f, 1f);
    }

    @Override
    public void update(Bullet b){
        super.update(b);
        if(b.data() instanceof Integer) return;

        if(b.timer(2, 6)){
            Units.nearbyEnemies(b.team, Tmp.r1.setSize(explodeRange * 2f).setCenter(b.x, b.y), unit -> {
                if(b.data() instanceof Float || !unit.checkTarget(collidesAir, collidesGround)) return;

                if(unit.dst(b) < explodeRange){
                    b.data(0);
                    Time.run(5f, () -> {
                        if(b.data() instanceof Integer){
                            b.time(b.lifetime());
                        }
                    });
                }
            });
        }
    }
}
