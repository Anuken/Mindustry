package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.*;

public class BlocksFragment extends Fragment{
    //number of block icon rows
    private static final int rows = 4;
    //number of category button rows
    private static final int secrows = 4;
    //size of each block icon
    private static final float size = 48;
    //maximum recipe rows
    private static final int maxrow = 3;
    /** Table containing description that is shown on top.*/
    private Table descTable;
    /** Main table containing the whole menu.*/
    private Table mainTable;
    /** Table for all section buttons and blocks.*/
    private Table selectTable;
    /** Whether the whole thing is shown or hidden by the popup button.*/
    private boolean shown = true;
    /** Recipe currently hovering over.*/
    private Recipe hoverRecipe;
    /** Last category selected.*/
    private Category lastCategory;
    /** Last block pane scroll Y position.*/
    private float lastScroll;
    /** Temporary recipe array for storage*/
    private Array<Recipe> recipes = new Array<>();

    public void build(Group parent){
        InputHandler input = control.input(0);

        parent.fill(container -> {
            container.bottom().right().visible(() -> !state.is(State.menu));

            mainTable = container.table(main -> {

                //add top description table
                descTable = new Table("button");
                descTable.visible(() -> (hoverRecipe != null || input.recipe != null) && shown); //make sure it's visible when necessary
                descTable.update(() -> {
                    if(state.is(State.menu)){
                        descTable.clear();
                        control.input(0).recipe = null;
                    }
                    // note: This is required because there is no direct connection between input.recipe and the description ui.
                    // If input.recipe gets set to null, a proper cleanup of the ui elements is required.
                    boolean anyRecipeShown = input.recipe != null || hoverRecipe != null;
                    boolean descriptionTableClean = descTable.getChildren().size == 0;
                    boolean cleanupRequired = (!anyRecipeShown && !descriptionTableClean);
                    if(cleanupRequired){
                        descTable.clear();
                    }
                });

                float w = 246f;

                main.add(descTable).width(w);

                main.row();

                //now add the block selection menu
                selectTable = main.table("pane", select -> {})
                .margin(10f).marginLeft(0f).marginRight(0f).marginTop(-5)
                .touchable(Touchable.enabled).right().bottom().width(w).get();

            }).bottom().right().get();
        });

        Events.on(WorldLoadEvent.class, event -> rebuild());

        rebuild();
    }

    /**Rebuilds the whole placement menu, attempting to preserve previous state.*/
    void rebuild(){
        selectTable.clear();

        InputHandler input = control.input(0);
        Stack stack = new Stack();
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        Table catTable = selectTable;

        int cati = 0;
        int checkedi = 0;
        int rowsUsed = 0;

        //add categories
        for(Category cat : Category.values()){
            //get recipes out by category
            Recipe.getUnlockedByCategory(cat, recipes);

            //empty section, nothing to see here
            if(recipes.size == 0){
                continue;
            }

            //table where actual recipes go
            Table recipeTable = new Table();
            recipeTable.margin(4).top().left().marginRight(15);

            //add a new row here when needed
            if(cati == secrows){
                catTable = new Table();
                selectTable.row();
                selectTable.add(catTable).colspan(secrows).padTop(-5).growX();
            }

            //add category button
            ImageButton catb = catTable.addImageButton("icon-" + cat.name(), "toggle", 40, () -> {
                if(!recipeTable.isVisible() && input.recipe != null){
                    input.recipe = null;
                }
                lastCategory = cat;
                stack.act(Gdx.graphics.getDeltaTime());
                stack.act(Gdx.graphics.getDeltaTime());
            }).growX().height(54).group(group)
                    .name("sectionbutton" + cat.name()).get();

            if(lastCategory == cat || lastCategory == null){
                checkedi = cati;
                lastCategory = cat;
            }

            //scrollpane for recipes
            ScrollPane pane = new ScrollPane(recipeTable, "clear-black");
            pane.setOverscroll(false, false);
            pane.visible(catb::isChecked);
            pane.setScrollYForce(lastScroll);
            pane.update(() -> {
                Element e = Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true);
                if(e != null && e.isDescendantOf(pane)){
                    Core.scene.setScrollFocus(pane);
                }else if(Core.scene.getScrollFocus() == pane){
                    Core.scene.setScrollFocus(null);
                }

                if(lastCategory == cat){
                    lastScroll = pane.getVisualScrollY();
                }
            });
            stack.add(pane);

            int i = 0;

            //add actual recipes
            for(Recipe r : recipes){
                if((r.mode != null && r.mode != state.mode) || (r.desktopOnly && mobile) || (r.isPad && !state.mode.showPads)) continue;

                ImageButton image = new ImageButton(new TextureRegion(), "select");

                TextureRegion[] regions = r.result.getCompactIcon();
                Stack istack = new Stack();
                for(TextureRegion region : regions){
                    Image u = new Image(region);
                    u.update(() -> u.setColor(istack.getColor()));
                    istack.add(u);
                }

                image.getImageCell().setActor(istack).size(size);
                image.addChild(istack);
                image.setTouchable(Touchable.enabled);
                image.getImage().remove();

                image.addListener(new ClickListener(){
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                        super.enter(event, x, y, pointer, fromActor);
                        if(hoverRecipe != r){
                            hoverRecipe = r;
                            updateRecipe(r);
                        }
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                        super.exit(event, x, y, pointer, toActor);
                        hoverRecipe = null;
                        updateRecipe(input.recipe);
                    }
                });

                image.clicked(() -> {
                    // note: input.recipe only gets set here during a click.
                    // during a hover only the visual description will be updated.
                    InputHandler handler = mobile ? input : control.input(0);

                    boolean nothingSelectedYet = handler.recipe == null;
                    boolean selectedSomethingElse = !nothingSelectedYet && handler.recipe != r;
                    boolean shouldMakeSelection = nothingSelectedYet || selectedSomethingElse;
                    if(shouldMakeSelection){
                        handler.recipe = r;
                        hoverRecipe = r;
                        updateRecipe(r);
                    }else{
                        handler.recipe = null;
                        hoverRecipe = null;
                        updateRecipe(null);
                    }
                });

                recipeTable.add(image).size(size + 8);

                image.update(() -> {
                    image.setChecked(r == control.input(0).recipe);
                    TileEntity entity = players[0].getClosestCore();

                    if(entity == null) return;

                    if(!state.mode.infiniteResources){
                        for(ItemStack s : r.requirements){
                            if(!entity.items.has(s.item, Mathf.ceil(s.amount))){
                                istack.setColor(Color.GRAY);
                                return;
                            }
                        }
                    }
                    istack.setColor(Color.WHITE);
                });

                if(i % rows == rows - 1){
                    rowsUsed = Math.max((i + 1) / rows, rowsUsed);
                    recipeTable.row();
                }

                i++;
            }

            cati++;
        }

        if(group.getButtons().size > 0){
            group.getButtons().get(checkedi).setChecked(true);
        }

        selectTable.row();
        selectTable.add(stack).growX().left().top().colspan(Category.values().length).padBottom(-5).height((size + 12) * Math.min(rowsUsed, 3));
    }

    void toggle(float t, Interpolation ip){
        if(shown){
            shown = false;
            mainTable.actions(Actions.translateBy(0, mainTable.getTranslation().y + (-mainTable.getHeight() - descTable.getHeight()), t, ip));
        }else{
            shown = true;
            mainTable.actions(Actions.translateBy(0, -mainTable.getTranslation().y, t, ip));
        }
    }

    private void updateRecipe(Recipe recipe){
        if(recipe == null){
            descTable.clear();
            return;
        }

        descTable.clear();
        descTable.setTouchable(Touchable.enabled);

        descTable.defaults().left();
        descTable.left();
        descTable.margin(12);

        Table header = new Table();

        descTable.add(header).left();

        descTable.row();

        TextureRegion[] regions = recipe.result.getCompactIcon();

        Stack istack = new Stack();

        for(TextureRegion region : regions) istack.add(new Image(region));

        header.add(istack).size(8 * 5).padTop(4);
        Label nameLabel = new Label(recipe.result.formalName);
        nameLabel.setWrap(true);
        header.add(nameLabel).padLeft(2).width(120f);

        header.addButton("?", () -> ui.content.show(recipe)).expandX().padLeft(3).top().right().size(40f, 44f).padTop(-2);

        descTable.add().pad(2);

        Table requirements = new Table();

        descTable.row();

        descTable.left();
        descTable.add(requirements);

        for(ItemStack stack : recipe.requirements){
            requirements.addImage(stack.item.region).size(8 * 3);
            Label reqlabel = new Label(() -> {
                TileEntity core = players[0].getClosestCore();
                if(core == null || state.mode.infiniteResources) return "*/*";

                int amount = core.items.get(stack.item);
                String color = (amount < stack.amount / 2f ? "[red]" : amount < stack.amount ? "[orange]" : "[white]");

                return color + format(amount) + "[white]/" + stack.amount;
            });

            requirements.add(reqlabel).left();
            requirements.row();
        }

        descTable.row();
    }

    String format(int number){
        if(number >= 1000000){
            return Strings.toFixed(number / 1000000f, 1) + "[gray]mil[]";
        }else if(number >= 10000){
            return number / 1000 + "[gray]k[]";
        }else if(number >= 1000){
            return Strings.toFixed(number / 1000f, 1) + "[gray]k[]";
        }else{
            return number + "";
        }
    }
}