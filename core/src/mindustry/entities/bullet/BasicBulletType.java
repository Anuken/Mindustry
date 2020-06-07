package mindustry.entities.bullet;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.*;
import mindustry.graphics.Pal;

/** An extended BulletType for most ammo-based bullets shot from turrets and units. */
public class BasicBulletType extends BulletType{
    public Color backColor = Pal.bulletYellowBack, frontColor = Pal.bulletYellow;
    public float bulletWidth = 5f, bulletHeight = 7f;
    public float bulletShrink = 0.5f, bulletSquish = 0f;
    public float spinSpeed = 0f;
    public String bulletSprite;

    public TextureRegion backRegion;
    public TextureRegion frontRegion;

    public BasicBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage);
        this.bulletSprite = bulletSprite;
    }


    public BasicBulletType(float speed, float damage){
        this(speed, damage, "bullet");
    }

    /** For mods. */
    public BasicBulletType(){
        this(1f, 1f, "bullet");
    }

    @Override
    public void load(){
        backRegion = Core.atlas.find(bulletSprite + "-back");
        frontRegion = Core.atlas.find(bulletSprite);
    }

    @Override
    public void draw(Bulletc b){
        float height = bulletHeight * ((1f - bulletShrink) + bulletShrink * b.fout());
        float width = bulletWidth * ((1f - bulletSquish) + bulletSquish * b.fout());
        float spin = b.time() * spinSpeed + b.id();

        Draw.color(backColor);
        Draw.rect(backRegion, b.x(), b.y(), width, height, b.rotation() - 90 + spin);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x(), b.y(), width, height, b.rotation() - 90 + spin);
        Draw.color();
    }
}
