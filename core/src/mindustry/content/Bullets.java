package mindustry.content;

import arc.graphics.*;
import mindustry.entities.bullet.*;

/**
 * Class for holding special internal bullets.
 * Formerly used to define preset bullets for turrets; as of v7, these have been inlined at the source.
 * */
public class Bullets{
    public static BulletType

    placeholder, spaceLiquid, damageLightning, damageLightningGround, fireball;

    public static void load(){

        //not allowed in weapons - used only to prevent NullPointerExceptions
        placeholder = new BasicBulletType(2.5f, 9, "ohno"){{
            width = 7f;
            height = 9f;
            lifetime = 60f;
            ammoMultiplier = 2;
        }};

        //lightning bullets need to be initialized first.
        damageLightning = new BulletType(0.0001f, 0f){{
            lifetime = Fx.lightning.lifetime;
            hitEffect = Fx.hitLancer;
            despawnEffect = Fx.none;
            status = StatusEffects.shocked;
            statusDuration = 10f;
            hittable = false;
            lightColor = Color.white;
        }};

        //this is just a copy of the damage lightning bullet that doesn't damage air units
        damageLightningGround = damageLightning.copy();
        damageLightningGround.collidesAir = false;

        fireball = new FireBulletType(1f, 4);

        spaceLiquid = new SpaceLiquidBulletType(){{
            knockback = 0.7f;
            drag = 0.01f;
        }};
    }
}
