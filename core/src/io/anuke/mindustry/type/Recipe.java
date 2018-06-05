package io.anuke.mindustry.type;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.Vars.headless;

public class Recipe implements UnlockableContent{
    private static int lastid;
    private static Array<Recipe> allRecipes = new Array<>();
    private static ObjectMap<Block, Recipe> recipeMap = new ObjectMap<>();

    public final int id;
    public final Block result;
    public final ItemStack[] requirements;
    public final Category category;
    public final float cost;

    public boolean desktopOnly = false, debugOnly = false;

    public Recipe(Category category, Block result, ItemStack... requirements){
        this.id = lastid ++;
        this.result = result;
        this.requirements = requirements;
        this.category = category;

        float timeToPlace = 0f;
        for(ItemStack stack : requirements){
            timeToPlace += stack.amount * stack.item.cost;
        }

        this.cost = timeToPlace;

        allRecipes.add(this);
        recipeMap.put(result, this);
    }

    public Recipe setDesktop(){
        desktopOnly = true;
        return this;
    }

    public Recipe setDebug(){
        debugOnly = true;
        return this;
    }

    @Override
    public String getContentName() {
        return result.name;
    }

    @Override
    public String getContentTypeName() {
        return "recipe";
    }

    @Override
    public Array<? extends Content> getAll() {
        return allRecipes;
    }

    /**Returns unlocked recipes in a category.
     * Do not call on the server backend, as unlocking does not exist!*/
    public static void getUnlockedByCategory(Category category, Array<Recipe> r){
        if(headless){
            throw new RuntimeException("Not enabled on the headless backend!");
        }

        r.clear();
        for(Recipe recipe : allRecipes){
            if(recipe.category == category && Vars.control.database().isUnlocked(recipe)) {
                r.add(recipe);
            }
        }
    }

    /**Returns all recipes in a category.*/
    public static void getByCategory(Category category, Array<Recipe> r){
        r.clear();
        for(Recipe recipe : allRecipes){
            if(recipe.category == category) {
                r.add(recipe);
            }
        }
    }

    public static Array<Recipe> all(){
        return allRecipes;
    }

    public static Recipe getByResult(Block block){
        return recipeMap.get(block);
    }

    public static Recipe getByID(int id){
        if(id < 0 || id >= allRecipes.size){
            return null;
        }else{
            return allRecipes.get(id);
        }
    }
}
