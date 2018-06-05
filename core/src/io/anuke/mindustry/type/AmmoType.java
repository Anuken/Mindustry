package io.anuke.mindustry.type;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.game.Content;
import io.anuke.ucore.core.Effects.Effect;

public class AmmoType implements Content{
    private static int lastID = 0;
    private static Array<AmmoType> allTypes = new Array<>();

    public final byte id;
    /**The item used. Always null if liquid isn't.*/
    public final Item item;
    /**The liquid used. Always null if item isn't.*/
    public final Liquid liquid;
    /**The resulting bullet. Never null.*/
    public final BulletType bullet;
    /**For item ammo, this is amount given per ammo item.
     * For liquid ammo, this is amount used per shot.*/
    public final float quantityMultiplier;
    /**Reload speed multiplier.*/
    public float speedMultiplier = 1f;
    /**Bullet recoil strength.*/
    public float recoil = 0f;
    /**Effect created when shooting.*/
    public Effect shootEffect = Fx.none;
    /**Extra smoke effect created when shooting.*/
    public Effect smokeEffect = Fx.none;

    {
        this.id = (byte)(lastID++);
        allTypes.add(this);
    }

    /**Creates an AmmoType with no liquid or item. Used for power-based ammo.*/
    public AmmoType(BulletType result){
        this.item = null;
        this.liquid = null;
        this.bullet = result;
        this.quantityMultiplier = 1f;
        this.speedMultiplier = 1f;
    }

    /**Creates an AmmoType with an item.*/
    public AmmoType(Item item, BulletType result, float multiplier){
        this.item = item;
        this.liquid = null;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
    }

    /**Creates an AmmoType with a liquid.*/
    public AmmoType(Liquid liquid, BulletType result, float multiplier){
        this.item = null;
        this.liquid = liquid;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
    }

    /**Returns maximum distance the bullet this ammo type has can travel.*/
    public float getRange(){
        return bullet.speed * bullet.lifetime;
    }

    @Override
    public String getContentTypeName() {
        return "ammotype";
    }

    @Override
    public Array<? extends Content> getAll() {
        return allTypes;
    }

    public static Array<AmmoType> all() {
        return allTypes;
    }

    public static AmmoType getByID(int id){
        return allTypes.get(id);
    }
}
