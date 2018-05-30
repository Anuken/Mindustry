package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class BlocksFragment implements Fragment{
	/**Table containing description that is shown on top.*/
	private Table desctable;
	/**Main table containing the whole menu.*/
	private Table blocks;
	/**Whether the whole thing is shown or hidden by the popup button.*/
	private boolean shown = true;
	/**Recipe currently hovering over.*/
	private Recipe hoverRecipe;
	/**Temporary recipe array for storage*/
	private Array<Recipe> recipes = new Array<>();

	//number of block icon rows
	private static final int rows = 4;
	//number of category button rows
	private static final int secrows = 4;
	//size of each block icon
	private static final float size = 48;
	//maximum recipe rows
	private static final int maxrow = 3;

	public void build(Group parent){
		InputHandler input = control.input(0);

		//create container table
		new table(){{
			abottom();
			aright();

			//make it only be shown when needed.
			visible(() -> !state.is(State.menu) && shown);

			//create the main blocks table
			blocks = new table(){{

				//add top description table
				desctable = new Table("button");
				desctable.setVisible(() -> hoverRecipe != null || input.recipe != null); //make sure it's visible when necessary
				desctable.update(() -> {
					// note: This is required because there is no direct connection between
					// input.recipe and the description ui. If input.recipe gets set to null
					// a proper cleanup of the ui elements is required.
					boolean anyRecipeShown = input.recipe != null || hoverRecipe != null;
					boolean descriptionTableClean = desctable.getChildren().size == 0;
					boolean cleanupRequired = !anyRecipeShown && !descriptionTableClean;
					if(cleanupRequired){
						desctable.clear();
					}
				});

				add(desctable).fillX().uniformX();

				row();

				//now add the block selection menu
				new table("pane") {{
					touchable(Touchable.enabled);
					get().setRound(true);

					Stack stack = new Stack();
					ButtonGroup<ImageButton> group = new ButtonGroup<>();

					//add categories
					for (Category cat : Category.values()) {
						//get recipes out by category
						Recipe.getUnlockedByCategory(cat, recipes);

						//empty section, nothing to see here
						if(recipes.size == 0){
							continue;
						}

						//table where actual recipes go
						Table recipeTable = new Table();
						recipeTable.margin(4).top().left().marginRight(15);

						//add category button
						ImageButton catb = get().addImageButton( "icon-" + cat.name(), "toggle", 40, () -> {
							if (!recipeTable.isVisible() && input.recipe != null) {
								input.recipe = null;
							}
						}).growX().height(54).padTop(cat.ordinal() <= secrows-1 ? -10 : -5).group(group)
								.name("sectionbutton" + cat.name()).get();

						//scrollpane for recipes
						ScrollPane pane = new ScrollPane(recipeTable, "clear-black");
						pane.setOverscroll(false, false);
						pane.setVisible(catb::isChecked);
						pane.update(() -> {
							Element e = Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true);
							if(e != null && e.isDescendantOf(pane)){
								Core.scene.setScrollFocus(pane);
							}
						});
						stack.add(pane);

						//add a new row here when needed
						if (cat.ordinal() % secrows == secrows-1) {
							row();
						}

						int i = 0;

						//add actual recipes
						for (Recipe r : recipes) {
							ImageButton image = new ImageButton(new TextureRegion(), "select");

							TextureRegion[] regions = r.result.getCompactIcon();
							Stack istack = new Stack();
							for(TextureRegion region : regions){
								istack.add(new Image(region));
							}

							image.getImageCell().setActor(istack).size(size);
							image.addChild(istack);
							image.setTouchable(Touchable.enabled);
							image.getImage().remove();

							image.addListener(new ClickListener(){
								@Override
								public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
									super.enter(event, x, y, pointer, fromActor);
									if (hoverRecipe != r) {
										hoverRecipe = r;
										updateRecipe(r);
									}
								}

								@Override
								public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
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
								if (shouldMakeSelection) {
									handler.recipe = r;
									hoverRecipe = r;
									updateRecipe(r);
								} else {
									handler.recipe = null;
									hoverRecipe = null;
									updateRecipe(null);
								}
							});

							recipeTable.add(image).size(size + 8);

							image.update(() -> {
								for(Player player : players){
									if(control.input(player.playerIndex).recipe == r){
										image.setChecked(true);
										return;
									}
								}
								image.setChecked(false);
							});

							if (i % rows == rows - 1) {
								recipeTable.row();
							}

							i++;
						}
					}

					row();
					add(stack).colspan(Category.values().length).padBottom(-5).height((size + 12)*maxrow);
					margin(10f);

					marginLeft(0f);
					marginRight(0f);

					end();
				}}.right().bottom();

				visible(() -> !state.is(State.menu) && shown);

			}}.end().get();
		}}.end();
	}

	public void toggle(boolean show, float t, Interpolation ip){
		if(!show){
			blocks.actions(Actions.translateBy(0, -blocks.getHeight() - desctable.getHeight(), t, ip), Actions.call(() -> shown = false));
		}else{
			shown = true;
			blocks.actions(Actions.translateBy(0, -blocks.getTranslation().y, t, ip));
		}
	}

	void updateRecipe(Recipe recipe){
		if (recipe == null) {
			desctable.clear();
			return;
		}

		desctable.clear();
		desctable.setTouchable(Touchable.enabled);

		desctable.defaults().left();
		desctable.left();
		desctable.margin(12);

		Table header = new Table();

		desctable.add(header).left();

		desctable.row();

		TextureRegion[] regions = recipe.result.getCompactIcon();

		Stack istack = new Stack();

		for(TextureRegion region : regions) istack.add(new Image(region));

		header.add(istack).size(8*5).padTop(4);
		Label nameLabel = new Label(recipe.result.formalName);
		nameLabel.setWrap(true);
		header.add(nameLabel).padLeft(2).width(120f);

		desctable.add().pad(2);

		Table requirements = new Table();

		desctable.row();

		desctable.add(requirements);
		desctable.left();

		for(ItemStack stack : recipe.requirements){
			requirements.addImage(stack.item.region).size(8*3);
			Label reqlabel = new Label("");

			reqlabel.update(() -> {
				int current = stack.amount;
				String text = Mathf.clamp(current, 0, stack.amount) + "/" + stack.amount;

				reqlabel.setText(text);
			});

			requirements.add(reqlabel).left();
			requirements.row();
		}

		desctable.row();
	}

	private void checkUnlockableBlocks(){
		TileEntity entity = players[0].getClosestCore();

		if(entity == null) return;

		for(Recipe recipe : Recipe.all()){
			if(entity.items.hasAtLeastOneOfItems(recipe.requirements)){
				control.database().unlockContent(recipe);
			}
		}
	}
}