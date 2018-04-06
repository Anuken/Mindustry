package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class TurretBullets {

    public static final BulletType

    basicIron = new BulletType(3f, 0) {
        @Override
        public void draw(Bullet b) {
            drawBullet(Palette.bulletYellow, Palette.bulletYellowBack,
                    "bullet", b.x, b.y, 9f, 5f + b.fout()*7f, b.angle() - 90);
        }
    },

    basicSteel = new BulletType(6f, 0) {
        {
            hiteffect = BulletFx.hitBulletBig;
            knockback = 0.5f;
        }

        @Override
        public void draw(Bullet b) {
            drawBullet(Palette.bulletYellow, Palette.bulletYellowBack,
                    "bullet", b.x, b.y, 11f, 9f + b.fout()*8f, b.angle() - 90);
        }
    },

    basicLeadFragShell = new BulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
        }

        @Override
        public void draw(Bullet b) {
            drawBullet(Palette.bulletYellow, Palette.bulletYellowBack,
                    "shell", b.x, b.y, 9f, 9f, b.angle() - 90);
        }

        @Override
        public void hit(Bullet b, float x, float y) {
            super.hit(b, x, y);
            for(int i = 0; i < 9; i ++){
                float len = Mathf.random(1f, 7f);
                float a = Mathf.random(360f);
                Bullet bullet = new Bullet(TurretBullets.basicLeadFrag, b,
                        x + Angles.trnsx(a, len), y + Angles.trnsy(a, len), a);
                bullet.velocity.scl(Mathf.random(0.2f, 1f));
                bullet.add();
            }
        }

        @Override
        public void despawned(Bullet b) {
            hit(b);
        }
    },

    basicLeadFrag = new BulletType(3f, 0) {
        {
            drag = 0.1f;
            hiteffect = Fx.none;
            despawneffect = Fx.none;
            hitsize = 4;
            lifetime = 20f;
        }

        @Override
        public void draw(Bullet b) {
            drawBullet(Palette.bulletYellow, Palette.bulletYellowBack,
                    "bullet", b.x, b.y, 7f + b.fout()*3f, 1f + b.fout()*11f, b.angle() - 90);
        }
    },

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
        public void draw(Bullet b) {}
    },

    lancerLaser = new BulletType(0.001f, 1) {
        Color[] colors = {Palette.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Palette.lancerLaser, Color.WHITE};
        float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
        float[] lenscales = {1f, 1.1f, 1.13f, 1.14f};
        float length = 70f;

        {
            hiteffect = BulletFx.hitLancer;
            despawneffect = Fx.none;
            hitsize = 4;
            lifetime = 20f;
            pierce = true;
        }

        @Override
        public void init(Bullet b) {
            DamageArea.collideLine(b, b.team, hiteffect, b.x, b.y, b.angle(), length);
        }

        @Override
        public void draw(Bullet b) {
            Lines.lineAngle(b.x, b.y, b.angle(), length);
            for(int s = 0; s < 3; s ++) {
                Draw.color(colors[s]);
                for (int i = 0; i < tscales.length; i++) {
                    Lines.stroke(7f * b.fout() * (s == 0 ? 1.5f : s == 1 ? 1f : 0.3f) * tscales[i]);
                    Lines.lineAngle(b.x, b.y, b.angle(), length * lenscales[i]);
                }
            }
            Draw.reset();
        }
    },

    waterShot = new LiquidShot(Liquids.water) {
        {
            status = StatusEffects.wet;
            statusIntensity = 0.5f;
            knockback = 0.65f;
        }
    },
    cryoShot = new LiquidShot(Liquids.cryofluid) {
        {
            status = StatusEffects.freezing;
            statusIntensity = 0.5f;
        }
    },
    lavaShot = new LiquidShot(Liquids.lava) {
        {
            damage = 4;
            speed = 1.9f;
            drag = 0.03f;
            status = StatusEffects.melting;
            statusIntensity = 0.5f;
        }
    },
    oilShot = new LiquidShot(Liquids.oil) {
        {
            speed = 2f;
            drag = 0.03f;
            status = StatusEffects.oiled;
            statusIntensity = 0.5f;
        }
    },
    lightning = new BulletType(0.001f, 5) {
        {
            lifetime = 1;
        }

        @Override
        public void draw(Bullet b) {

        }

        @Override
        public void init(Bullet b) {
            new Lightning(b.team, b, b.x, b.y, b.angle(), 30);
        }
    };

    private static void drawBullet(Color first, Color second, String name, float x, float y, float w, float h, float rot){
        Draw.color(second);
        Draw.rect(name + "-back", x, y, w, h, rot);
        Draw.color(first);
        Draw.rect(name, x, y, w, h, rot);
        Draw.color();
    }

    private abstract static class LiquidShot extends BulletType{
        Liquid liquid;

        public LiquidShot(Liquid liquid) {
            super(2.5f, 0);
            this.liquid = liquid;

            lifetime = 70f;
            despawneffect = Fx.none;
            hiteffect = BulletFx.hitLiquid;
            drag = 0.01f;
            knockback = 0.5f;
        }

        @Override
        public void draw(Bullet b) {
            Draw.color(liquid.color, Color.WHITE, b.fout() / 100f + Mathf.randomSeedRange(b.id, 0.1f));

            Fill.circle(b.x, b.y, 0.5f + b.fout()*2.5f);
        }

        @Override
        public void hit(Bullet b, float hitx, float hity) {
            Effects.effect(hiteffect, liquid.color, hitx, hity);
        }
    }
}
