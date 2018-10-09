package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.bullet.FlakBulletType;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;
import io.anuke.ucore.util.Mathf;

public class FlakBullets extends BulletList implements ContentList{
    public static BulletType plastic, explosive, surge;

    @Override
    public void load(){


        plastic = new FlakBulletType(4f, 5){
            {
                splashDamageRadius = 40f;
                fragBullet = ArtilleryBullets.plasticFrag;
                fragBullets = 4;
                hiteffect = BulletFx.plasticExplosion;
                frontColor = Palette.plastaniumFront;
                backColor = Palette.plastaniumBack;
            }
        };

        explosive = new FlakBulletType(4f, 5){
            {
                //default bullet type, no changes
            }
        };

        surge = new FlakBulletType(4f, 7){
            {
                splashDamage = 33f;
            }

            @Override
            public void despawned(Bullet b) {
                super.despawned(b);

                for (int i = 0; i < 2; i++) {
                    Lightning.create(b.getTeam(), Palette.surge, damage, b.x, b.y, Mathf.random(360f), 12);
                }
            }
        };
    }
}
