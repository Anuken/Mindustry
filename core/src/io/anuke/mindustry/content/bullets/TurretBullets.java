package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.bullet.LiquidBulletType;
import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class TurretBullets implements ContentList {
    public static BulletType fireball, basicFlame, lancerLaser, fuseShot, waterShot, cryoShot, lavaShot, oilShot, lightning;

    @Override
    public void load() {

        fireball = new BulletType(1f, 1) {
            {
                pierce = true;
                hitTiles = false;
                drag = 0.3f;
            }

            @Override
            public void init(Bullet b) {
                b.getVelocity().setLength(0.6f + Mathf.random(2f));
            }

            @Override
            public void draw(Bullet b) {
                //TODO add color to the bullet depending on the color of the flame it came from
                Draw.color(Palette.lightFlame, Palette.darkFlame, Color.GRAY, b.fin());
                Fill.circle(b.x, b.y, 3f * b.fout());
                Draw.reset();
            }

            @Override
            public void update(Bullet b) {
                if (Mathf.chance(0.04 * Timers.delta())) {
                    Tile tile = world.tileWorld(b.x, b.y);
                    if (tile != null) {
                        Fire.create(tile);
                    }
                }

                if (Mathf.chance(0.1 * Timers.delta())) {
                    Effects.effect(EnvironmentFx.fireballsmoke, b.x, b.y);
                }

                if (Mathf.chance(0.1 * Timers.delta())) {
                    Effects.effect(EnvironmentFx.ballfire, b.x, b.y);
                }
            }
        };

        basicFlame = new BulletType(2f, 0) {
            {
                hitsize = 7f;
                lifetime = 30f;
                pierce = true;
                drag = 0.07f;
                hiteffect = BulletFx.hitFlameSmall;
                despawneffect = Fx.none;
                status = StatusEffects.burning;
            }

            @Override
            public void draw(Bullet b) {
            }
        };

        lancerLaser = new BulletType(0.001f, 1) {
            Color[] colors = {Palette.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Palette.lancerLaser, Color.WHITE};
            float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
            float[] lenscales = {1f, 1.1f, 1.13f, 1.14f};
            float length = 70f;

            {
                hiteffect = BulletFx.hitLancer;
                despawneffect = Fx.none;
                hitsize = 4;
                lifetime = 16f;
                pierce = true;
            }

            @Override
            public void init(Bullet b) {
                DamageArea.collideLine(b, b.team, hiteffect, b.x, b.y, b.angle(), length);
            }

            @Override
            public void draw(Bullet b) {
                float f = Mathf.curve(b.fin(), 0f, 0.2f);
                float baseLen = length * f;

                Lines.lineAngle(b.x, b.y, b.angle(), baseLen);
                for (int s = 0; s < 3; s++) {
                    Draw.color(colors[s]);
                    for (int i = 0; i < tscales.length; i++) {
                        Lines.stroke(7f * b.fout() * (s == 0 ? 1.5f : s == 1 ? 1f : 0.3f) * tscales[i]);
                        Lines.lineAngle(b.x, b.y, b.angle(), baseLen * lenscales[i]);
                    }
                }
                Draw.reset();
            }
        };

        fuseShot = new BulletType(0.01f, 100) {
            //TODO
        };

        waterShot = new LiquidBulletType(Liquids.water) {
            {
                status = StatusEffects.wet;
                statusIntensity = 0.5f;
                knockback = 0.65f;
            }
        };
        cryoShot = new LiquidBulletType(Liquids.cryofluid) {
            {
                status = StatusEffects.freezing;
                statusIntensity = 0.5f;
            }
        };
        lavaShot = new LiquidBulletType(Liquids.lava) {
            {
                damage = 4;
                speed = 1.9f;
                drag = 0.03f;
                status = StatusEffects.melting;
                statusIntensity = 0.5f;
            }
        };
        oilShot = new LiquidBulletType(Liquids.oil) {
            {
                speed = 2f;
                drag = 0.03f;
                status = StatusEffects.oiled;
                statusIntensity = 0.5f;
            }
        };
        lightning = new BulletType(0.001f, 5) {
            {
                lifetime = 1;
                despawneffect = Fx.none;
                hiteffect = BulletFx.hitLancer;
            }

            @Override
            public void draw(Bullet b) {
            }

            @Override
            public void init(Bullet b) {
                Lightning.create(b.team, hiteffect, Palette.lancerLaser, damage, b.x, b.y, b.angle(), 30);
            }
        };
    }
}
