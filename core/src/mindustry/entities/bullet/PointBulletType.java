package mindustry.entities.bullet;

import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class PointBulletType extends BulletType{
     private static float cdist = 0f;
     private static Unit result;

     public float trailSpacing = 10f;

     public PointBulletType(){
         scaleLife = true;
         lifetime = 100f;
         collides = false;
         keepVelocity = false;
         backMove = false;
     }

    @Override
    public void init(Bullet b){
        super.init(b);

        float rot = b.rotation();

        Geometry.iterateLine(0f, b.x, b.y, b.aimX, b.aimY, trailSpacing, (x, y) -> {
            trailEffect.at(x, y, rot);
        });

        b.time = b.lifetime;
        b.set(b.aimX, b.aimY);

        //calculate hit entity

        cdist = 0f;
        result = null;
        float range = 1f;

        Units.nearbyEnemies(b.team, b.aimX - range, b.aimY - range, range * 2f, range * 2f, e -> {
            if(e.dead()) return;

            e.hitbox(Tmp.r1);
            if(!Tmp.r1.contains(b.aimX, b.aimY)) return;

            float dst = e.dst(b.aimX, b.aimY) - e.hitSize;
            if((result == null || dst < cdist)){
                result = e;
                cdist = dst;
            }
        });

        if(result != null){
            b.collision(result, b.aimX, b.aimY);
        }else{
            Building build = Vars.world.buildWorld(b.aimX, b.aimY);
            if(build != null && build.team != b.team){
                build.collision(b);
            }
        }

        b.remove();

        b.vel.setZero();
    }
}
