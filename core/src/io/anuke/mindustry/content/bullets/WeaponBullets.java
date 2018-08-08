package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BombBulletType;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class WeaponBullets extends BulletList{
    public static BulletType tungstenShotgun, bombExplosive, bombIncendiary, bombOil, shellCarbide;

    @Override
    public void load(){
        tungstenShotgun = new BasicBulletType(5f, 8, "bullet"){
            {
                bulletWidth = 8f;
                bulletHeight = 9f;
                bulletShrink = 0.5f;
                lifetime = 50f;
                drag = 0.04f;
            }
        };

        bombExplosive = new BombBulletType(10f, 20f, "shell"){
            {
                bulletWidth = 9f;
                bulletHeight = 13f;
                hiteffect = BulletFx.flakExplosion;
            }
        };

        bombIncendiary = new BombBulletType(7f, 10f, "shell"){
            {
                bulletWidth = 8f;
                bulletHeight = 12f;
                hiteffect = BulletFx.flakExplosion;
                backColor = Palette.lightOrange;
                frontColor = Palette.lightishOrange;
            }

            @Override
            public void hit(Bullet b, float x, float y){
                super.hit(b, x, y);

                for(int i = 0; i < 3; i++){
                    float cx = x + Mathf.range(10f);
                    float cy = y + Mathf.range(10f);
                    Tile tile = world.tileWorld(cx, cy);
                    if(tile != null){
                        Fire.create(tile);
                    }
                }
            }
        };

        bombOil = new BombBulletType(2f, 3f, "shell"){
            {
                bulletWidth = 8f;
                bulletHeight = 12f;
                hiteffect = BlockFx.pulverize;
                backColor = new Color(0x4f4f4fff);
                frontColor = Color.GRAY;
            }

            @Override
            public void hit(Bullet b, float x, float y){
                super.hit(b, x, y);

                for(int i = 0; i < 3; i++){
                    Tile tile = world.tileWorld(x + Mathf.range(8f), y + Mathf.range(8f));
                    Puddle.deposit(tile, Liquids.oil, 5f);
                }
            }
        };

        shellCarbide = new BasicBulletType(3.4f, 20, "bullet"){
            {
                bulletWidth = 10f;
                bulletHeight = 12f;
                bulletShrink = 0.4f;
                lifetime = 40f;
                drag = 0.025f;
                fragBullets = 5;
                hiteffect = BulletFx.flakExplosion;
                fragBullet = tungstenShotgun;
            }
        };
    }
}
