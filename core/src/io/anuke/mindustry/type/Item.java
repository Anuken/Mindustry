package io.anuke.mindustry.type;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.blocks.*;

import static io.anuke.mindustry.Vars.content;

public class Item extends UnlockableContent implements Comparable<Item>{
    public final Color color;
    private TextureRegion[] regions;

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

    @Override
    public void load(){
        regions = new TextureRegion[Icon.values().length];
        for(int i = 0; i < regions.length; i++){
            Icon icon = Icon.values()[i];
            regions[i] = Core.atlas.find(icon == Icon.large ? "item-" + name : "item-" + name + "-" + icon.name());
        }
    }

    public TextureRegion icon(Icon icon){
        return regions[icon.ordinal()];
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
    public TextureRegion getContentIcon(){
        return icon(Icon.large);
    }

    @Override
    public String toString(){
        return localizedName();
    }

    @Override
    public int compareTo(Item item){
        return Integer.compare(id, item.id);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.item;
    }

    public enum Icon{
        small(8 * 2),
        medium(8 * 3),
        large(8 * 4),
        xlarge(8 * 5),
        xxlarge(8 * 6);

        public final int size;

        Icon(int size){
            this.size = size;
        }
    }

    /** Allocates a new array containing all items that generate ores. */
    public static Array<Item> getAllOres(){
        return content.blocks().select(b -> b instanceof OreBlock).map(b -> ((Floor)b).itemDrop);
    }
}
