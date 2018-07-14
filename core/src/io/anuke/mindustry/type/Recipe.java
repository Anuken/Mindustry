package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.ui.ContentDisplay;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.ContentStatValue;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

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

    private Block[] dependencies;
    private Recipe[] recipeDependencies;

    public Recipe(Category category, Block result, ItemStack... requirements){
        this.id = lastid++;
        this.result = result;
        this.requirements = requirements;
        this.category = category;

        Arrays.sort(requirements, (a, b) -> Integer.compare(a.item.id, b.item.id));

        float timeToPlace = 0f;
        for(ItemStack stack : requirements){
            timeToPlace += stack.amount * stack.item.cost;
        }

        this.cost = timeToPlace;

        allRecipes.add(this);
        recipeMap.put(result, this);
    }

    /**
     * Returns unlocked recipes in a category.
     * Do not call on the server backend, as unlocking does not exist!
     */
    public static void getUnlockedByCategory(Category category, Array<Recipe> r){
        if(headless){
            throw new RuntimeException("Not enabled on the headless backend!");
        }

        r.clear();
        for(Recipe recipe : allRecipes){
            if(recipe.category == category && (Vars.control.database().isUnlocked(recipe) || (debug && recipe.debugOnly))){
                r.add(recipe);
            }
        }
    }

    /**
     * Returns all recipes in a category.
     */
    public static void getByCategory(Category category, Array<Recipe> r){
        r.clear();
        for(Recipe recipe : allRecipes){
            if(recipe.category == category){
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

    public Recipe setDesktop(){
        desktopOnly = true;
        return this;
    }

    public Recipe setDebug(){
        debugOnly = true;
        return this;
    }

    @Override
    public boolean isHidden(){
        return debugOnly;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayRecipe(table, this);
    }

    @Override
    public String localizedName(){
        return result.formalName;
    }

    @Override
    public TextureRegion getContentIcon(){
        return result.getEditorIcon();
    }

    @Override
    public void init(){
        if(!Bundles.has("block." + result.name + ".name")){
            Log.err("WARNING: Recipe block '{0}' does not have a formal name defined. Add the following to bundle.properties:", result.name);
            Log.err("block.{0}.name={1}", result.name, Strings.capitalize(result.name.replace('-', '_')));
        }/*else if(result.fullDescription == null){
            Log.err("WARNING: Recipe block '{0}' does not have a description defined.", result.name);
        }*/
    }

    @Override
    public String getContentName(){
        return result.name;
    }

    @Override
    public String getContentTypeName(){
        return "recipe";
    }

    @Override
    public void onUnlock(){
        for(OrderedMap<BlockStat, StatValue> map : result.stats.toMap().values()){
            for(StatValue value : map.values()){
                if(value instanceof ContentStatValue){
                    ContentStatValue stat = (ContentStatValue) value;
                    UnlockableContent[] content = stat.getValueContent();
                    for(UnlockableContent c : content){
                        control.database().unlockContent(c);
                    }
                }
            }
        }
    }

    @Override
    public UnlockableContent[] getDependencies(){
        if(dependencies == null){
            return null;
        }else if(recipeDependencies == null){
            recipeDependencies = new Recipe[dependencies.length];
            for(int i = 0; i < recipeDependencies.length; i++){
                recipeDependencies[i] = Recipe.getByResult(dependencies[i]);
            }
        }
        return recipeDependencies;
    }

    public Recipe setDependencies(Block... blocks){
        this.dependencies = blocks;
        return this;
    }

    @Override
    public Array<? extends Content> getAll(){
        return allRecipes;
    }
}
