package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.type.Section;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class BlocksFragment implements Fragment{
	private Table desctable, blocks;
	private Stack stack = new Stack();
	private boolean shown = true;
	private Recipe hoveredDescriptionRecipe;

	public void build(Group parent){
		InputHandler input = control.input(0);

		new table(){{
			abottom();
			aright();

			visible(() -> !state.is(State.menu) && shown);

			blocks = new table(){{
				desctable = new Table("button");
				desctable.setVisible(() -> hoveredDescriptionRecipe != null || input.recipe != null);
				desctable.update(() -> {
					// note: This is required because there is no direct connection between
					// input.recipe and the description ui. If input.recipe gets set to null
					// a proper cleanup of the ui elements is required.
					boolean anyRecipeShown = input.recipe != null || hoveredDescriptionRecipe != null;
					boolean descriptionTableClean = desctable.getChildren().size == 0;
					boolean cleanupRequired = !anyRecipeShown && !descriptionTableClean;
					if(cleanupRequired){
						desctable.clear();
					}
				});

				stack.add(desctable);

				add(stack).fillX().uniformX();

				row();

				new table("pane") {{
					touchable(Touchable.enabled);
					int rows = 4;
					int maxcol = 0;
					float size = 48;

					Stack stack = new Stack();
					ButtonGroup<ImageButton> group = new ButtonGroup<>();
					Array<Recipe> recipes = new Array<>();

					for (Section sec : Section.values()) {
						recipes.clear();
						Recipe.getBySection(sec, recipes);
						maxcol = Math.max((int) ((float) recipes.size / rows + 1), maxcol);
					}

					for (Section sec : Section.values()) {
						int secrows = 4;

						recipes.clear();
						Recipe.getBySection(sec, recipes);

						Table table = new Table();

						ImageButton button = new ImageButton("icon-" + sec.name(), "toggle");
						button.clicked(() -> {
							if (!table.isVisible() && input.recipe != null) {
								input.recipe = null;
							}
						});

						button.setName("sectionbutton" + sec.name());
						add(button).growX().height(54).padLeft(-1).padTop(sec.ordinal() <= secrows-1 ? -10 : -5);
						button.getImageCell().size(40).padBottom(4).padTop(2);
						group.add(button);

						if (sec.ordinal() % secrows == secrows-1) {
							row();
						}

						table.margin(4);
						table.top().left();

						int i = 0;

						for (Recipe r : recipes) {
							ImageButton image = new ImageButton(new TextureRegion(), "select");

							TextureRegion[] regions = r.result.getCompactIcon();
							Stack istack = new Stack();
							for(TextureRegion region : regions){
								istack.add(new Image(region));
							}

							image.getImageCell().setActor(istack).size(size);
							image.addChild(istack);
							image.getImage().remove();

							image.addListener(new ClickListener(){
								@Override
								public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
									super.enter(event, x, y, pointer, fromActor);
									if (hoveredDescriptionRecipe != r) {
										hoveredDescriptionRecipe = r;
										updateRecipe(r);
									}
								}

								@Override
								public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
									super.exit(event, x, y, pointer, toActor);
									hoveredDescriptionRecipe = null;
									updateRecipe(input.recipe);
								}
							});

							image.addListener(new ClickListener(){
								@Override
								public void clicked(InputEvent event, float x, float y){
									// note: input.recipe only gets set here during a click.
									// during a hover only the visual description will be updated.
									InputHandler handler = mobile ? input : control.input(event.getPointer());

									boolean nothingSelectedYet = handler.recipe == null;
									boolean selectedSomethingElse = !nothingSelectedYet && handler.recipe != r;
									boolean shouldMakeSelection = nothingSelectedYet || selectedSomethingElse;
									if (shouldMakeSelection) {
										handler.recipe = r;
										hoveredDescriptionRecipe = r;
										updateRecipe(r);
									} else {
										handler.recipe = null;
										hoveredDescriptionRecipe = null;
										updateRecipe(null);
									}
								}
							});

							table.add(image).size(size + 8);

							image.update(() -> {
								image.setTouchable(Touchable.enabled);
								for(Element e : istack.getChildren()){
									e.setColor(Color.WHITE);
								}

								for(Player player : players){
									if(control.input(player.playerIndex).recipe == r){
										image.setChecked(true);
										return;
									}
								}
								image.setChecked(false);
							});

							if (i % rows == rows - 1) {
								table.row();
							}

							i++;
						}

						table.setVisible(button::isChecked);

						stack.add(table);
					}

					row();
					add(stack).colspan(Section.values().length);
					margin(10f);

					marginLeft(1f);
					marginRight(1f);

					end();
				}}.right().bottom().uniformX();

				visible(() -> !state.is(State.menu) && shown);

			}}.end().get();
		}}.end();
	}

	public void toggle(boolean show, float t, Interpolation ip){
		if(!show){
			blocks.actions(Actions.translateBy(0, -blocks.getHeight() - stack.getHeight(), t, ip), Actions.call(() -> shown = false));
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

		Label label = new Label("[health]"+ Bundles.get("text.health")+": " + recipe.result.health);
		label.setWrap(true);
		desctable.add(label).width(200).padTop(4).padBottom(2);
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