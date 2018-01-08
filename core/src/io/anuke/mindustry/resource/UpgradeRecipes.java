package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entries;
import io.anuke.ucore.util.Mathf;

public class UpgradeRecipes {
    private static final ObjectMap<Upgrade, ItemStack[]> recipes = Mathf.map(
            Weapon.triblaster, list(stack(Item.iron, 40), stack(Item.steel, 40)),
            Weapon.clustergun, list(stack(Item.iron, 60), stack(Item.steel, 80)),
            Weapon.vulcan, list(stack(Item.iron, 60), stack(Item.steel, 120), stack(Item.titanium, 60)),
            Weapon.beam, list(stack(Item.steel, 240), stack(Item.titanium, 120), stack(Item.dirium, 80)),
            Weapon.shockgun, list(stack(Item.steel, 120), stack(Item.titanium, 120), stack(Item.dirium, 120))
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
