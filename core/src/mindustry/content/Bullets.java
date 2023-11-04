package mindustry.content;

import arc.graphics.Color;
import mindustry.entities.bullet.*;

/**
 * Class for holding special internal bullets.
 * This class defines various bullet types for in-game use.
 */
public class Bullets {
    // Define bullet types
    public static BulletType placeholder;
    public static BulletType spaceLiquid;
    public static BulletType damageLightning;
    public static BulletType damageLightningGround;
    public static BulletType fireball;

    /**
     * Load and initialize the predefined bullet types.
     */
    public static void load() {
        // Define placeholder bullet (used to prevent NullPointerExceptions)
        placeholder = new BasicBulletType(2.5f, 9, "ohno") {{
            width = 7f;
            height = 9f;
            lifetime = 60f;
            ammoMultiplier = 2;
        }};

        // Define lightning damage bullet
        damageLightning = new BulletType(0.0001f, 0f) {{
            lifetime = Fx.lightning.lifetime;
            hitEffect = Fx.hitLancer;
            despawnEffect = Fx.none;
            status = StatusEffects.shocked;
            statusDuration = 10f;
            hittable = false;
            lightColor = Color.white;
        }};

        // Define a ground version of the damage lightning bullet (doesn't damage air units)
        damageLightningGround = damageLightning.copy();
        damageLightningGround.collidesAir = false;

        // Define fireball bullet
        fireball = new FireBulletType(1f, 4);

        // Define space liquid bullet
        spaceLiquid = new SpaceLiquidBulletType() {{
            knockback = 0.7f;
            drag = 0.01f;
        }};
    }
}
