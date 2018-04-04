package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.mindustry.graphics.fx.Fx;
import io.anuke.ucore.graphics.Draw;

public class TurretBullets {
    public static final BulletType

    basicIron = new BulletType(3f, 0) {
        @Override
        public void draw(Bullet b) {
            Draw.color(Color.valueOf("f3d47f"));
            Draw.rect("bullet", b.x, b.y, 9f, 5f + b.fract()*7f, b.angle() - 90);
            Draw.color();
        }
    },

    basicFlame = new BulletType(2f, 4) {
        {
            hitsize = 7f;
            lifetime = 30f;
            pierce = true;
            drag = 0.07f;
            hiteffect = BulletFx.hitFlameSmall;
            despawneffect = Fx.none;
        }

        @Override
        public void draw(Bullet b) {}
    };
}
