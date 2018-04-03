package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.graphics.Draw;

public class TurretBullets {
    public static final BulletType

    basicIron = new BulletType(2f, 0) {
        @Override
        public void draw(Bullet b) {
            Draw.color(Color.valueOf("f6e096"));
            Draw.rect("bullet", b.x, b.y, b.angle() - 90);
            Draw.color();
        }
    };
}
