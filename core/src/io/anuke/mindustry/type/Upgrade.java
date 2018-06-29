package io.anuke.mindustry.type;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.util.Bundles;

public abstract class Upgrade implements Content{
    private static Array<Upgrade> upgrades = new Array<>();
    private static byte lastid;

    public final byte id;
    public final String name;
    public final String description;

    public Upgrade(String name){
        this.id = lastid ++;
        this.name = name;
        this.description = Bundles.get("upgrade."+name+".description");

        upgrades.add(this);
    }

    public String localizedName(){
        return Bundles.get("upgrade." + name + ".name");
    }

    @Override
    public String toString(){
        return localizedName();
    }

    @Override
    public Array<? extends Content> getAll() {
        return all();
    }

    public static <T extends Upgrade> void forEach(Consumer<T> type, Predicate<Upgrade> pred){
        for(Upgrade u : upgrades){
            if(pred.test(u)){
                type.accept((T)u);
            }
        }
    }

    public static Array<Upgrade> all() {
        return upgrades;
    }

    public static <T extends Upgrade> T getByID(byte id){
        return (T)upgrades.get(id);
    }
}
