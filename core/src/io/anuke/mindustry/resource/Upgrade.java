package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;

public abstract class Upgrade {
    private static Array<Upgrade> upgrades = new Array<>();
    private static byte lastid;

    public final byte id;

    public Upgrade(){
        this.id = lastid ++;
        upgrades.add(this);
    }

    public static Upgrade getByID(byte id){
        return upgrades.get(id);
    }

    public static Array<Upgrade> getAllUpgrades() {
        return upgrades;
    }
}
