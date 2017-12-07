package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Section;
import io.anuke.mindustry.ui.FloatingDialog;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

public class BlocksFragment implements Fragment{
	private Table desctable;
	private Array<String> statlist = new Array<>();
	
	public void build(){
		new table(){{
			abottom();
			aright();
			
			new table("button"){{
				visible(()->player.recipe != null);
				desctable = get();
				fillX();
			}}.end().uniformX();
			
			row();

			new table("pane"){{
				
				int rows = 4;
				int maxcol = 0;
				float size = 48;
				
				Stack stack = new Stack();
				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				Array<Recipe> recipes = new Array<Recipe>();
				
				for(Section sec : Section.values()){
					recipes.clear();
					Recipe.getBy(sec, recipes);
					maxcol = Math.max((int)((float)recipes.size/rows+1), maxcol);
				}
				
				for(Section sec : Section.values()){
					recipes.clear();
					Recipe.getBy(sec, recipes);
					
					Table table = new Table();
					
					ImageButton button = new ImageButton("icon-"+sec.name(), "toggle");
					button.clicked(()->{
						if(!table.isVisible() && player.recipe != null){
							player.recipe = null;
						}
					});
					button.setName("sectionbutton" + sec.name());
					add(button).growX().height(54).padTop(sec.ordinal() <= 2 ? -10 : -5).units(Unit.dp);
					button.getImageCell().size(40).padBottom(4).padTop(2).units(Unit.dp);
					group.add(button);
					
					if(sec.ordinal() % 3 == 2 && sec.ordinal() > 0){
						row();
					}
					
					table.pad(4);
					table.top().left();
					
					int i = 0;
					
					for(Recipe r : recipes){
						TextureRegion region = Draw.hasRegion(r.result.name() + "-icon") ? 
								Draw.region(r.result.name() + "-icon") : Draw.region(r.result.name());
						ImageButton image = new ImageButton(region, "select");
						
						image.clicked(()->{
							if(player.recipe == r){
								player.recipe = null;
							}else{
								player.recipe = r;
								updateRecipe();
							}
						});
						
						table.add(image).size(size+8).pad(2).units(Unit.dp);
						image.getImageCell().size(size).units(Unit.dp);
						
						image.update(()->{
							
							boolean canPlace = !control.getTutorial().active() || control.getTutorial().canPlace();
							boolean has = (control.hasItems(r.requirements)) && canPlace;
							//image.setDisabled(!has);
							image.setChecked(player.recipe == r);
							image.setTouchable(canPlace ? Touchable.enabled : Touchable.disabled);
							image.getImage().setColor(has ? Color.WHITE : Hue.lightness(0.33f));
						});
						
						if(i % rows == rows-1)
							table.row();
						
						i++;
					}
					
					table.setVisible(()-> button.isChecked());
					
					stack.add(table);
				}
				
				
				row();
				add(stack).colspan(Section.values().length);
				get().pad(10f);
				
				get().padLeft(0f);
				get().padRight(0f);
				
				end();
			}}.right().bottom().uniformX();
			
			visible(()->!GameState.is(State.menu));

		}}.end();
	}
	
	void updateRecipe(){
		Recipe recipe = player.recipe;
		desctable.clear();
		
		desctable.defaults().left();
		desctable.left();
		desctable.pad(Unit.dp.inPixels(12));
		
		Table header = new Table();
		
		desctable.add(header).left();
		
		desctable.row();
		
		TextureRegion region = Draw.hasRegion(recipe.result.name() + "-icon") ? 
				Draw.region(recipe.result.name() + "-icon") : Draw.region(recipe.result.name());
		
		header.addImage(region).size(8*5).padTop(4).units(Unit.dp);
		Label nameLabel = new Label(recipe.result.formalName);
		nameLabel.setWrap(true);
		header.add(nameLabel).padLeft(4).width(135f).units(Unit.dp);
		
		//extra info
		if(recipe.result.fullDescription != null){
			header.addButton("?", ()->{
				statlist.clear();
				recipe.result.getStats(statlist);
				
				Label desclabel = new Label(recipe.result.fullDescription);
				desclabel.setWrap(true);
				
				boolean wasPaused = GameState.is(State.paused);
				GameState.set(State.paused);
				
				FloatingDialog d = new FloatingDialog("Block Info");
				Table table = new Table();
				table.defaults().pad(1f).units(Unit.dp);
				ScrollPane pane = new ScrollPane(table, "clear");
				pane.setFadeScrollBars(false);
				Table top = new Table();
				top.left();
				top.add(new Image(Draw.region(recipe.result.name))).size(8*5 * recipe.result.width).units(Unit.dp);
				top.add("[orange]"+recipe.result.formalName).padLeft(6f).units(Unit.dp);
				table.add(top).fill().left();
				table.row();
				table.add(desclabel).width(600).units(Unit.dp);
				table.row();
				
				d.content().add(pane).grow();
				
				if(statlist.size > 0){
					table.add("[coral][[extra block info]:").padTop(6).padBottom(5).left();
					table.row();
				}
				
				for(String s : statlist){
					table.add(s).left();
					table.row();
				}
				
				d.buttons().addButton("OK", ()->{
					if(!wasPaused) GameState.set(State.playing);
					d.hide();
				}).size(110, 50).pad(10f).units(Unit.dp);
				
				d.show();
			}).expandX().padLeft(4).top().right().size(42f, 46f).padTop(-2).units(Unit.dp);
		}
		
		
		desctable.add().pad(2).units(Unit.dp);
		
		Table requirements = new Table();
		
		desctable.row();
		
		desctable.add(requirements);
		desctable.left();
		
		for(ItemStack stack : recipe.requirements){
			ItemStack fs = stack;
			requirements.addImage(Draw.region("icon-"+stack.item.name())).size(8*3).units(Unit.dp);
			Label reqlabel = new Label("");
			
			reqlabel.update(()->{
				int current = control.getAmount(fs.item);
				String text = Mathf.clamp(current, 0, stack.amount) + "/" + stack.amount;
				
				reqlabel.setColor(current < stack.amount ? Colors.get("missingitems") : Color.WHITE);
				
				reqlabel.setText(text);
			});
			
			requirements.add(reqlabel).left();
			requirements.row();
		}
		
		desctable.row();
		
		Label label = new Label("[health]health: " + recipe.result.health + (recipe.result.description == null ?
				"" : ("\n[]" + recipe.result.description)));
		label.setWrap(true);
		desctable.add(label).width(200).padTop(4).padBottom(2).units(Unit.dp);
		
	}
}
