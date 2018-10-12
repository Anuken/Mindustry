package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.bullet.MissileBulletType;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class MissileBullets extends BulletList implements ContentList{
    public static BulletType explosive, incindiary, surge, javelin, swarm;

    @Override
    public void load(){

        explosive = new MissileBulletType(1.8f, 10, "missile"){
            {
                bulletWidth = 8f;
                bulletHeight = 8f;
                bulletShrink = 0f;
                drag = -0.01f;
                splashDamageRadius = 30f;
                splashDamage = 30f;
                lifetime = 150f;
                hiteffect = BulletFx.blastExplosion;
                despawneffect = BulletFx.blastExplosion;
            }
        };

        incindiary = new MissileBulletType(2f, 12, "missile"){
            {
                frontColor = Palette.lightishOrange;
                backColor = Palette.lightOrange;
                bulletWidth = 7f;
                bulletHeight = 8f;
                bulletShrink = 0f;
                drag = -0.01f;
                homingPower = 7f;
                splashDamageRadius = 10f;
                splashDamage = 10f;
                lifetime = 160f;
                hiteffect = BulletFx.blastExplosion;
                incendSpread = 10f;
                incendAmount = 3;
            }
        };

        surge = new MissileBulletType(3.5f, 15, "bullet"){
            {
                bulletWidth = 8f;
                bulletHeight = 8f;
                bulletShrink = 0f;
                drag = -0.01f;
                splashDamageRadius = 30f;
                splashDamage = 22f;
                lifetime = 150f;
                hiteffect = BulletFx.blastExplosion;
                despawneffect = BulletFx.blastExplosion;
            }

            @Override
            public void hit(Bullet b) {
                super.hit(b);

                for (int i = 0; i < 2; i++) {
                    Lightning.create(b.getTeam(), Palette.surge, damage, b.x, b.y, Mathf.random(360f), 14);
                }
            }
        };

        javelin = new MissileBulletType(5f, 10.5f, "missile"){
            {
                bulletWidth = 8f;
                bulletHeight = 8f;
                bulletShrink = 0f;
                drag = -0.003f;
                keepVelocity = false;
                splashDamageRadius = 20f;
                splashDamage = 1f;
                lifetime = 90f;
                trailColor = Color.valueOf("b6c6fd");
                hiteffect = BulletFx.blastExplosion;
                despawneffect = BulletFx.blastExplosion;
                backColor = Palette.bulletYellowBack;
                frontColor = Palette.bulletYellow;
            }

            @Override
            public void update(Bullet b){
                super.update(b);
                b.getVelocity().rotate(Mathf.sin(Timers.time() + b.id * 4422, 8f, 2f));
            }
        };

        swarm = new MissileBulletType(2.7f, 12, "missile"){
            {
                bulletWidth = 8f;
                bulletHeight = 8f;
                bulletShrink = 0f;
                drag = -0.003f;
                homingRange = 60f;
                keepVelocity = false;
                splashDamageRadius = 25f;
                splashDamage = 10f;
                lifetime = 120f;
                trailColor = Color.GRAY;
                backColor = Palette.bulletYellowBack;
                frontColor = Palette.bulletYellow;
                hiteffect = BulletFx.blastExplosion;
                despawneffect = BulletFx.blastExplosion;
            }

            @Override
            public void update(Bullet b){
                super.update(b);
                b.getVelocity().rotate(Mathf.sin(Timers.time() + b.id * 4422, 8f, 2f));
            }
        };
    }
}
