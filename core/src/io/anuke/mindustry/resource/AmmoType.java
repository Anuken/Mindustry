package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.BulletType;

public class AmmoType {
    private static int lastID = 0;
    private static Array<AmmoType> allTypes = new Array<>();

    public final byte id;
    /**The item used. Always null if liquid isn't.*/
    public final Item item;
    /**The liquid used. Always null if item isn't.*/
    public final Liquid liquid;
    /**The resulting bullet.*/
    public final BulletType bullet;
    /**For item ammo, this is amount given per ammo item.
     * For liquid ammo, this is amount used per shot.*/
    public final float quantityMultiplier;
    /**Turret shoot speed multiplier.*/
    public final float speedMultiplier;

    {
        this.id = (byte)(lastID++);
        allTypes.add(this);
    }

    public AmmoType(Item item, BulletType result, float multiplier, float speedMultiplier){
        this.item = item;
        this.liquid = null;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
        this.speedMultiplier = speedMultiplier;
    }

    public AmmoType(Liquid liquid, BulletType result, float multiplier, float speedMultiplier){
        this.item = null;
        this.liquid = liquid;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
        this.speedMultiplier = speedMultiplier;
    }

    public static Array<AmmoType> getAllTypes() {
        return allTypes;
    }

    public static AmmoType getByID(byte id){
        return allTypes.get(id);
    }
}
