package mindustry.entities.bullet;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;

/** An extended BulletType for most ammo-based bullets shot from turrets and units. */
public class BasicBulletType extends BulletType{
    public Color backColor = Pal.bulletYellowBack, frontColor = Pal.bulletYellow;
    public float width = 5f, height = 7f;
    public float shrinkX = 0f, shrinkY = 0.5f;
    public float spin = 0;
    public String sprite;

    public TextureRegion backRegion;
    public TextureRegion frontRegion;

    public BasicBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage);
        this.sprite = bulletSprite;
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
        backRegion = Core.atlas.find(sprite + "-back");
        frontRegion = Core.atlas.find(sprite);
    }

    @Override
    public void draw(Bullet b){
        float height = this.height * ((1f - shrinkY) + shrinkY * b.fout());
        float width = this.width * ((1f - shrinkX) + shrinkX * b.fout());
        float offset = -90 + (spin != 0 ? Mathf.randomSeed(b.id, 360f) + b.time * spin : 0f);

        Draw.color(backColor);
        Draw.rect(backRegion, b.x, b.y, width, height, b.rotation() + offset);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x, b.y, width, height, b.rotation() + offset);
        Draw.color();
    }
}
