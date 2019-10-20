package io.anuke.mindustry.type;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.blocks.*;

import static io.anuke.mindustry.Vars.content;

public class Item extends UnlockableContent{
    public final Color color;

    /** type of the item; used for tabs and core acceptance. default value is {@link ItemType#resource}. */
    public ItemType type = ItemType.resource;
    /** how explosive this item is. */
    public float explosiveness = 0f;
    /** flammability above 0.3 makes this eleigible for item burners. */
    public float flammability = 0f;
    /** how radioactive this item is. 0=none, 1=chernobyl ground zero */
    public float radioactivity;
    /** drill hardness of the item */
    public int hardness = 0;
    /**
     * base material cost of this item, used for calculating place times
     * 1 cost = 1 tick added to build time
     */
    public float cost = 1f;
    /** If true, item is always unlocked. */
    public boolean alwaysUnlocked = false;

    public Item(String name, Color color){
        super(name);
        this.color = color;
        this.description = Core.bundle.getOrNull("item." + this.name + ".description");
    }

    public Item(String name){
        this(name, new Color(Color.black));
    }

    @Override
    public boolean alwaysUnlocked(){
        return alwaysUnlocked;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayItem(table, this);
    }

    @Override
    public String localizedName(){
        return Core.bundle.get("item." + this.name + ".name");
    }

    @Override
    public String toString(){
        return localizedName();
    }

    @Override
    public ContentType getContentType(){
        return ContentType.item;
    }

    /** Allocates a new array containing all items that generate ores. */
    public static Array<Item> getAllOres(){
        return content.blocks().select(b -> b instanceof OreBlock).map(b -> ((Floor)b).itemDrop);
    }
}
