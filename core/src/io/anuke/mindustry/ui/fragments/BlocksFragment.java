package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.*;

public class BlocksFragment implements Fragment{
	private Table desctable, itemtable, blocks, weapons;
	private Stack stack = new Stack();
	private Array<String> statlist = new Array<>();
	private boolean shown = true;
	private Recipe hoveredDescriptionRecipe;
	
	public void build(){
		InputHandler input = control.input();

		new table(){{
			abottom();
			aright();

            visible(() -> !state.is(State.menu) && shown);

			blocks = new table(){{

				itemtable = new Table("button");
				itemtable.setVisible(() -> input.recipe == null && !state.mode.infiniteResources);

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

				stack.add(itemtable);
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
						Recipes.getBy(sec, recipes);
						maxcol = Math.max((int) ((float) recipes.size / rows + 1), maxcol);
					}

					for (Section sec : Section.values()) {
						recipes.clear();
						Recipes.getBy(sec, recipes);

						Table table = new Table();

						ImageButton button = new ImageButton("icon-" + sec.name(), "toggle");
						button.clicked(() -> {
							if (!table.isVisible() && input.recipe != null) {
								input.recipe = null;
							}
						});
						button.setName("sectionbutton" + sec.name());
						add(button).growX().height(54).padLeft(-1).padTop(sec.ordinal() <= 2 ? -10 : -5);
						button.getImageCell().size(40).padBottom(4).padTop(2);
						group.add(button);

						if (sec.ordinal() % 3 == 2 && sec.ordinal() > 0) {
							row();
						}

						table.margin(4);
						table.top().left();

						int i = 0;

						for (Recipe r : recipes) {
							TextureRegion region = Draw.hasRegion(r.result.name() + "-icon") ?
									Draw.region(r.result.name() + "-icon") : Draw.region(r.result.name());
							ImageButton image = new ImageButton(region, "select");

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

							image.clicked(() -> {
								// note: input.recipe only gets set here during a click.
								// during a hover only the visual description will be updated.
								boolean nothingSelectedYet = input.recipe == null;
								boolean selectedSomethingElse = !nothingSelectedYet && input.recipe != r;
								boolean shouldMakeSelection = nothingSelectedYet || selectedSomethingElse;
								if (shouldMakeSelection) {
									input.recipe = r;
									hoveredDescriptionRecipe = r;
									updateRecipe(r);
								} else {
									input.recipe = null;
									hoveredDescriptionRecipe = null;
									updateRecipe(null);
								}
							});

							table.add(image).size(size + 8);
							image.getImageCell().size(size);

							image.update(() -> {
								boolean canPlace = !control.tutorial().active() || control.tutorial().canPlace();
								boolean has = (state.inventory.hasItems(r.requirements)) && canPlace;
								image.setChecked(input.recipe == r);
								image.setTouchable(canPlace ? Touchable.enabled : Touchable.disabled);
								image.getImage().setColor(has ? Color.WHITE : Hue.lightness(0.33f));
							});

							if (i % rows == rows - 1)
								table.row();

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

				row();

				if(!mobile) {
					weapons = new table("button").margin(0).fillX().end().get();
				}

				visible(() -> !state.is(State.menu) && shown);

			}}.end().get();
		}}.end();

		updateWeapons();
	}

	public void updateWeapons(){
		if(mobile) return;

		weapons.clearChildren();
		weapons.left();

		ButtonGroup<ImageButton> group = new ButtonGroup<>();

		for(int i = 0; i < control.upgrades().getWeapons().size; i ++){
			Weapon weapon = control.upgrades().getWeapons().get(i);
			weapons.addImageButton(weapon.name, "toggle", 8*3, () -> {
				player.weaponLeft = player.weaponRight = weapon;
			}).left().size(40f, 45f).padRight(-1).group(group);
		}

		int idx = control.upgrades().getWeapons().indexOf(player.weaponLeft, true);

		if(idx != -1)
			group.getButtons().get(idx).setChecked(true);
		else if(group.getButtons().size > 0)
			group.getButtons().get(0).setChecked(true);
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
		
		TextureRegion region = Draw.hasRegion(recipe.result.name() + "-icon") ? 
				Draw.region(recipe.result.name() + "-icon") : Draw.region(recipe.result.name());
		
		header.addImage(region).size(8*5).padTop(4);
		Label nameLabel = new Label(recipe.result.formalName);
		nameLabel.setWrap(true);
		header.add(nameLabel).padLeft(2).width(120f);
		
		//extra info
		if(recipe.result.fullDescription != null){
			header.addButton("?", () -> showBlockInfo(recipe.result)).expandX().padLeft(3).top().right().size(40f, 44f).padTop(-2);
		}
		
		desctable.add().pad(2);
		
		Table requirements = new Table();
		
		desctable.row();
		
		desctable.add(requirements);
		desctable.left();
		
		for(ItemStack stack : recipe.requirements){
			requirements.addImage(Draw.region("icon-"+stack.item.name)).size(8*3);
			Label reqlabel = new Label("");
			
			reqlabel.update(()->{
				int current = state.inventory.getAmount(stack.item);
				String text = Mathf.clamp(current, 0, stack.amount) + "/" + stack.amount;
				
				reqlabel.setColor(current < stack.amount ? Colors.get("missingitems") : Color.WHITE);
				
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

	public void showBlockInfo(Block block){
		statlist.clear();
		block.getStats(statlist);

		Label desclabel = new Label(block.fullDescription);
		desclabel.setWrap(true);

		boolean wasPaused = state.is(State.paused);
		state.set(State.paused);

		FloatingDialog d = new FloatingDialog("$text.blocks.blockinfo");
		Table table = new Table();
		table.defaults().pad(1f);
		ScrollPane pane = new ScrollPane(table, "clear");
		pane.setFadeScrollBars(false);
		Table top = new Table();
		top.left();
		top.add(new Image(Draw.region(block.name))).size(8*5 * block.width);
		top.add("[accent]"+block.formalName).padLeft(6f);
		table.add(top).fill().left();
		table.row();
		table.add(desclabel).width(600);
		table.row();

		d.content().add(pane).grow();

		if(statlist.size > 0){
			table.add("$text.blocks.extrainfo").padTop(6).padBottom(5).left();
			table.row();
		}

		for(String s : statlist){
			if(s.contains(":")) {
				String color = s.substring(0, s.indexOf("]")+1);
				String first = s.substring(color.length(), s.indexOf(":")).replace("/", "").replace(" ", "").toLowerCase();
				String last = s.substring(s.indexOf(":"), s.length());
				s = color + Bundles.get("text.blocks." + first) + last;
			}
			table.add(s).left();
			table.row();
		}

		d.buttons().addButton("$text.ok", ()->{
			if(!wasPaused) state.set(State.playing);
			d.hide();
		}).size(110, 50).pad(10f);

		d.show();
	}

	public void updateItems(){

		itemtable.clear();
		itemtable.left();

		if(state.mode.infiniteResources){
			return;
		}

		for(int i = 0; i < state.inventory.getItems().length; i ++){
			int amount = state.inventory.getItems()[i];
			if(amount == 0) continue;
			String formatted = amount > 99999999 ? "inf" : format(amount);
			Image image = new Image(Draw.hasRegion("icon-" + Item.getByID(i).name) ?
					Draw.region("icon-" + Item.getByID(i).name) : Draw.region("blank"));
			Label label = new Label(formatted);
			label.setFontScale(fontscale*1.5f);
			itemtable.add(image).size(8*3);
			itemtable.add(label).expandX().left();
			if(i % 2 == 1 && i > 0) itemtable.row();
		}
	}

	String format(int number){
		if(number > 1000000) {
			return Strings.toFixed(number/1000000f, 1) + "[gray]mil";
		}else if(number > 10000){
			return number/1000 + "[gray]k";
		}else if(number > 1000){
			return Strings.toFixed(number/1000f, 1) + "[gray]k";
		}else{
			return number + "";
		}
	}
}
