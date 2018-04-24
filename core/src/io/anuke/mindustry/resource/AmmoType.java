package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.core.Effects.Effect;

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
    /**Reload speed multiplier.*/
    public float speedMultiplier = 1f;
    /**Effect created when shooting.*/
    public Effect shootEffect = Fx.none;
    /**Extra smoke effect created when shooting.*/
    public Effect smokeEffect = Fx.none;

    {
        this.id = (byte)(lastID++);
        allTypes.add(this);
    }

    public AmmoType(BulletType result){
        this.item = null;
        this.liquid = null;
        this.bullet = result;
        this.quantityMultiplier = 1f;
        this.speedMultiplier = 1f;
    }

    public AmmoType(Item item, BulletType result, float multiplier){
        this.item = item;
        this.liquid = null;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
    }

    public AmmoType(Liquid liquid, BulletType result, float multiplier){
        this.item = null;
        this.liquid = liquid;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
    }

    public static Array<AmmoType> getAllTypes() {
        return allTypes;
    }

    public static AmmoType getByID(int id){
        return allTypes.get(id);
    }
}
