package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entries;
import io.anuke.ucore.util.Mathf;

public class UpgradeRecipes {
    private static final ObjectMap<Upgrade, ItemStack[]> recipes = Mathf.map(
            Weapon.triblaster, list(stack(Item.iron, 60), stack(Item.steel, 80)),
            Weapon.clustergun, list(stack(Item.iron, 300), stack(Item.steel, 80)),
            Weapon.vulcan, list(stack(Item.iron, 100), stack(Item.steel, 150), stack(Item.titanium, 80)),
            Weapon.beam, list(stack(Item.steel, 260), stack(Item.titanium, 160), stack(Item.dirium, 120)),
            Weapon.shockgun, list(stack(Item.steel, 240), stack(Item.titanium, 160), stack(Item.dirium, 160))
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
