package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
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

public class Recipe extends UnlockableContent{
    private static ObjectMap<Block, Recipe> recipeMap = new ObjectMap<>();
    private static Array<Recipe> returnArray = new Array<>();

    public final Block result;
    public final ItemStack[] requirements;
    public final Category category;
    public final float cost;

    public RecipeVisibility visibility = RecipeVisibility.all;
    //the only gamemode in which the recipe shows up
    public GameMode mode;
    public boolean hidden;
    public boolean alwaysUnlocked;

    private UnlockableContent[] dependencies;
    private Block[] blockDependencies;

    public Recipe(Category category, Block result, ItemStack... requirements){
        this.result = result;
        this.requirements = requirements;
        this.category = category;

        Arrays.sort(requirements, (a, b) -> Integer.compare(a.item.id, b.item.id));

        float timeToPlace = 0f;
        for(ItemStack stack : requirements){
            timeToPlace += stack.amount * stack.item.cost;
        }

        this.cost = timeToPlace;

        recipeMap.put(result, this);
    }

    /**Returns all non-hidden recipes in a category.*/
    public static Array<Recipe> getByCategory(Category category){
        returnArray.clear();
        for(Recipe recipe : content.recipes()){
            if(recipe.category == category && recipe.visibility.shown() && (recipe.mode == state.mode || recipe.mode == null)){
                returnArray.add(recipe);
            }
        }
        return returnArray;
    }

    public static Recipe getByResult(Block block){
        return recipeMap.get(block);
    }

    public Recipe setVisible(RecipeVisibility visibility){
        this.visibility = visibility;
        return this;
    }

    public Recipe setMode(GameMode mode){
        this.mode = mode;
        return this;
    }

    public Recipe setHidden(boolean hidden){
        this.hidden = hidden;
        return this;
    }

    public Recipe setAlwaysUnlocked(boolean unlocked){
        this.alwaysUnlocked = unlocked;
        return this;
    }

    @Override
    public boolean alwaysUnlocked(){
        return alwaysUnlocked;
    }

    @Override
    public boolean isHidden(){
        return !visibility.shown() || hidden;
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
    public ContentType getContentType(){
        return ContentType.recipe;
    }

    @Override
    public void onUnlock(){
        for(OrderedMap<BlockStat, StatValue> map : result.stats.toMap().values()){
            for(StatValue value : map.values()){
                if(value instanceof ContentStatValue){
                    ContentStatValue stat = (ContentStatValue) value;
                    UnlockableContent[] content = stat.getValueContent();
                    for(UnlockableContent c : content){
                        control.unlocks.unlockContent(c);
                    }
                }
            }
        }
    }

    @Override
    public UnlockableContent[] getDependencies(){
        if(blockDependencies != null && dependencies == null){
            dependencies = new UnlockableContent[blockDependencies.length];
            for(int i = 0; i < dependencies.length; i++){
                dependencies[i] = Recipe.getByResult(blockDependencies[i]);
            }
            return dependencies;
        }
        return dependencies;
    }

    public Recipe setDependencies(UnlockableContent... dependencies){
        this.dependencies = dependencies;
        return this;
    }

    public Recipe setDependencies(Block... dependencies){
        this.blockDependencies = dependencies;
        return this;
    }

    public enum RecipeVisibility{
        mobileOnly(true, false),
        desktopOnly(false, true),
        all(true, true);

        public final boolean mobile, desktop;

        RecipeVisibility(boolean mobile, boolean desktop){
            this.mobile = mobile;
            this.desktop = desktop;
        }

        public boolean shown(){
            return (Vars.mobile && mobile) || (!Vars.mobile && desktop);
        }
    }
}
