package io.anuke.mindustry.type;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.ucore.util.Bundles;

public abstract class Upgrade implements UnlockableContent{
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

    @Override
    public String toString(){
        return localized();
    }

    @Override
    public String getContentName() {
        return name;
    }

    @Override
    public String getContentTypeName() {
        return "upgrade";
    }

    @Override
    public Array<? extends Content> getAll() {
        return all();
    }

    public static Array<Upgrade> all() {
        return upgrades;
    }

    public static <T extends Upgrade> T getByID(byte id){
        return (T)upgrades.get(id);
    }
}
