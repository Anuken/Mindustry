package mindustry.entities.bullet;

import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class FlakBulletType extends BasicBulletType{
    public float explodeRange = 30f, explodeDelay = 5f, flakInterval = 6f;

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

        //don't check for targets if primed to explode
        if(b.fdata >= 0 && b.timer(2, flakInterval)){
            Units.nearbyEnemies(b.team, Tmp.r1.setSize(explodeRange * 2f).setCenter(b.x, b.y), unit -> {
                //fadata < 0 means it's primed to explode
                if(b.fdata < 0f || !unit.checkTarget(collidesAir, collidesGround)) return;

                if(unit.within(b, explodeRange + unit.hitSize/2f)){
                    //mark as primed
                    b.fdata = -1f;
                    Time.run(explodeDelay, () -> {
                        //explode
                        if(b.fdata < 0){
                            b.time = b.lifetime;
                        }
                    });
                }
            });
        }
    }
}
