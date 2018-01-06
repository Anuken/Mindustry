package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entries;
import io.anuke.ucore.util.Mathf;

public class UpgradeRecipes {
    private static final ObjectMap<Upgrade, ItemStack[]> recipes = Mathf.map(
            Weapon.triblaster, list(stack(Item.iron, 40)),
            Weapon.multigun, list(stack(Item.iron, 60), stack(Item.steel, 20)),
            Weapon.flamer, list(stack(Item.steel, 60), stack(Item.iron, 120)),
            Weapon.railgun, list(stack(Item.iron, 60), stack(Item.steel, 60)),
            Weapon.mortar, list(stack(Item.titanium, 40), stack(Item.steel, 60))
    );

    private static final ItemStack[] empty = {};

    public static ItemStack[] get(Upgrade upgrade){
        return recipes.get(upgrade, empty);
    }

    public static Entries<Upgrade, ItemStack[]> getAllRecipes(){
        return recipes.entries();
    }

    private static ItemStack[] list(ItemStack... stacks){
        return stacks;
    }

    private static ItemStack stack(Item item, int amount){
        return new ItemStack(item, amount);
    }
}
