package mindustry.ai;

import arc.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;

public class ItemUnitStance extends UnitStance{
    private static ObjectMap<Item, ItemUnitStance> itemToStance = new ObjectMap<>();

    public final Item item;

    public ItemUnitStance(Item item){
        super("item-" + item.name, "item-" + item.name, null);
        this.item = item;
        itemToStance.put(item, this);
    }

    public static @Nullable ItemUnitStance getByItem(Item item){
        return itemToStance.get(item);
    }

    @Override
    public String localized(){
        return Core.bundle.format("stance.mine", item.localizedName);
    }

    @Override
    public TextureRegionDrawable getIcon(){
        return new TextureRegionDrawable(item.uiIcon);
    }
}
