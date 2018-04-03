package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entries;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.ucore.util.Mathf;

public class UpgradeRecipes {
    private static final ObjectMap<Upgrade, ItemStack[]> recipes = Mathf.map(
            /*
        Weapons.triblaster, list(stack(Items.iron, 60), stack(Items.steel, 80)),
        Weapons.clustergun, list(stack(Items.iron, 300), stack(Items.steel, 80)),
        Weapons.vulcan, list(stack(Items.iron, 100), stack(Items.steel, 150), stack(Items.titanium, 80)),
        Weapons.beam, list(stack(Items.steel, 260), stack(Items.titanium, 160), stack(Items.densealloy, 120)),
        Weapons.shockgun, list(stack(Items.steel, 240), stack(Items.titanium, 160), stack(Items.densealloy, 160))*/
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
