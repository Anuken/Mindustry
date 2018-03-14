package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.util.Bundles;

public abstract class Upgrade {
    private static Array<Upgrade> upgrades = new Array<>();
    private static byte lastid;

    public final byte id;
    public final String name;
    public final String description;

    public Upgrade(String name){
        this.id = lastid ++;
        this.name = name;
        this.description = Bundles.getNotNull("upgrade."+name+".description");

        upgrades.add(this);
    }

    public String localized(){
        return Bundles.get("upgrade." + name + ".name");
    }

    public static <T extends Upgrade> T getByID(byte id){
        return (T)upgrades.get(id);
    }

    public static Array<Upgrade> getAllUpgrades() {
        return upgrades;
    }
}
