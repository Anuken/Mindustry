package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.BulletType;

public class AmmoType {
    private static int lastID = 0;
    private static Array<AmmoType> allTypes = new Array<>();

    public final byte id;
    public final Item item;
    public final BulletType bullet;
    public final int quantityMultiplier;
    public final float speedMultiplier;

    public AmmoType(Item item, BulletType result, int multiplier, float speedMultiplier){
        this.item = item;
        this.bullet = result;
        this.quantityMultiplier = multiplier;
        this.speedMultiplier = speedMultiplier;
        this.id = (byte)(lastID++);
        allTypes.add(this);
    }

    public static Array<AmmoType> getAllTypes() {
        return allTypes;
    }

    public static AmmoType getByID(byte id){
        return allTypes.get(id);
    }
}
