package io.anuke.mindustry.type;

import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.game.Content;
import io.anuke.ucore.core.Effects.Effect;

public class AmmoType extends Content {
    /**The item used. Always null if liquid isn't.*/
    public final Item item;
    /**The liquid used. Always null if item isn't.*/
    public final Liquid liquid;
    /**The resulting bullet. Never null.*/
    public final BulletType bullet;
    /**
     * For item ammo, this is amount given per ammo item.
     * For liquid ammo, this is amount used per shot.
     */
    public final float quantityMultiplier;
    /**Reload speed multiplier.*/
    public float reloadMultiplier = 1f;
    /**Bullet recoil strength.*/
    public float recoil = 0f;
    /**Additional inaccuracy in degrees.*/
    public float inaccuracy;
    /**Effect created when shooting.*/
    public Effect shootEffect = Fx.none;
    /**Extra smoke effect created when shooting.*/
    public Effect smokeEffect = Fx.none;
    /**Range. Use a value < 0 to calculate from bullet.*/
    public float range = -1f;

    /**
     * Creates an AmmoType with no liquid or item. Used for power-based ammo.
     */
    public AmmoType(BulletType result){
        this.item = null;
        this.liquid = null;
        this.bullet = result;
        this.quantityMultiplier = 1f;
        this.reloadMultiplier = 1f;
    }

    /**
     * Creates an AmmoType with an item.
     */
    public AmmoType(Item item, BulletType result, float multiplier){
        this.item = item;
        this.liquid = null;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
    }

    /**
     * Creates an AmmoType with a liquid.
     */
    public AmmoType(Liquid liquid, BulletType result, float multiplier){
        this.item = null;
        this.liquid = liquid;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
    }

    /**
     * Returns maximum distance the bullet this ammo type has can travel.
     */
    public float getRange(){
        return range < 0 ? bullet.speed * bullet.lifetime : range;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.ammo;
    }
}
