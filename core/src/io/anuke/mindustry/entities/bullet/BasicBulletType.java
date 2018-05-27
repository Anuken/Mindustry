package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

/**A BulletType for most ammo-based bullets shot from turrets and units.*/
public class BasicBulletType extends BulletType {
    public Color backColor = Palette.bulletYellowBack, frontColor = Palette.bulletYellow;
    public String bulletSprite = "bullet";
    public float bulletWidth = 5f, bulletHeight = 7f;
    public float bulletShrink = 0.5f;

    public int fragBullets = 9;
    public float fragVelocityMin = 0.2f, fragVelocityMax = 1f;
    public BulletType fragBullet = null;

    public BasicBulletType(float speed, float damage) {
        super(speed, damage);
    }

    @Override
    public void draw(Bullet b) {
        float height = bulletHeight * ((1f - bulletShrink) + bulletShrink * b.fout());

        Draw.color(backColor);
        Draw.rect(bulletSprite + "-back", b.x, b.y, bulletWidth, height, b.angle() - 90);
        Draw.color(frontColor);
        Draw.rect(bulletSprite, b.x, b.y, bulletWidth, height, b.angle() - 90);
        Draw.color();
    }

    @Override
    public void hit(Bullet b, float x, float y) {
        super.hit(b, x, y);

        if(fragBullet != null) {
            for (int i = 0; i < fragBullets; i++) {
                float len = Mathf.random(1f, 7f);
                float a = Mathf.random(360f);
                Bullet bullet = Bullet.create(fragBullet, b,
                        x + Angles.trnsx(a, len), y + Angles.trnsy(a, len), a);
                bullet.velocity.scl(Mathf.random(fragVelocityMin, fragVelocityMax));
            }
        }
    }

    @Override
    public void despawned(Bullet b) {
        if(fragBullet != null){
            hit(b);
        }
    }
}
