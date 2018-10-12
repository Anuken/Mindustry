package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;

public class Missions{

    /**Returns an array of missions to obtain the items needed to make this block.
     * This array includes a mission to place this block.*/
    public static Array<Mission> blockRecipe(Block block){
        Recipe recipe = Recipe.getByResult(block);

        Array<Mission> out = new Array<>();
        for(ItemStack stack : recipe.requirements){
            out.add(new ItemMission(stack.item, stack.amount));
        }
        out.add(new BlockMission(block));
        return out;
    }
}
