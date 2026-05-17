package mindustry.ai;

import arc.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;

public class ItemUnitStance extends UnitStance{
    private static ObjectMap<Item, ItemUnitStance> itemToStance = new ObjectMap<>();
    private static Seq<ItemUnitStance> all = new Seq<>();

    public final Item item;

    public ItemUnitStance(Item item){
        super("item-" + item.name, "item-" + item.name, null);
        this.item = item;

        incompatibleStances.add(UnitStance.mineAuto).addAll(UnitStance.mineAuto.incompatibleStances);

        itemToStance.put(item, this);
        all.add(this);
    }

    public static @Nullable ItemUnitStance getByItem(Item item){
        return item == null ? null : itemToStance.get(item);
    }

    public static Seq<ItemUnitStance> all(){
        return all;
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
