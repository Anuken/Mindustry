package io.anuke.moment;

import static io.anuke.moment.world.TileType.tilesize;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import io.anuke.moment.entities.Enemy;
import io.anuke.moment.resource.Item;
import io.anuke.moment.resource.ItemStack;
import io.anuke.moment.resource.Recipe;
import io.anuke.moment.world.Tile;
import io.anuke.moment.world.TileType;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.UGraphics;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.style.Styles;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

public class UI extends SceneModule<Moment>{
	Table itemtable;
	
	public UI(){
		Styles.styles.font().setUseIntegerPositions(false);
		TooltipManager.getInstance().animations = false;
	}
	
	@Override
	public void update(){
		scene.getBatch().setProjectionMatrix(get(Control.class).camera.combined);
		scene.getBatch().begin();
		Tile tile = main.tiles[tilex()][tiley()];
		if(tile.block() != TileType.air){
			String error = tile.block().error(tile);
			if(error != null){
				Draw.tcolor(Color.SCARLET);
				Draw.tscl(1/8f);
				Draw.text(error, tile.worldx(), tile.worldy()+tilesize);
				
			}else if(tile.block().name().contains("turret")){
				Draw.tscl(1/8f);
				Draw.tcolor(Color.GREEN);
				Draw.text("Ammo: " + tile.entity.shots, tile.worldx(), tile.worldy()-tilesize);
			}
			
			Draw.tscl(0.5f);
			Draw.clear();
		}
		scene.getBatch().end();
		
		super.update();
	}
	
	@Override
	public void init(){
		
		build.begin(scene);
		
		new table(){{
			abottom();
			aright();
			
			new table(){{
				
				get().background("button");
				
				for(Recipe r : Recipe.values()){
					Image image = new Image(Draw.region(r.result.name()));
					
					new button(r.result.name(), ()->{
						main.recipe = r;
					}){{
						get().clearChildren();
						get().pad(10f);
						get().add(image).size(42).padRight(4f);
						Table table = new Table();
						table.add(get().getLabel()).left();
						get().add(table);
						get().left();
						
						ItemStack[] req = r.requirements;
						for(ItemStack stack : req){
							table.row();
							table.add("[YELLOW]"+stack.amount +"x " +stack.item.name()).left();
						}
						get().getLabel().setAlignment(Align.left);
						
						String description = r.result.description();
						if(r.result.ammo != null){
							description += "\n[SALMON]Ammo: " + r.result.ammo.name();
						}
						
						Table tiptable = new Table();
						tiptable.background("button");
						tiptable.add("[PURPLE]"+r.result.name(), 0.5f).left().padBottom(2f);
						tiptable.row();
						tiptable.add("[ORANGE]"+description).left();
						tiptable.pad(8f);
						
						Tooltip tip = new Tooltip(tiptable);
						tip.setInstant(true);
						
						get().addListener(tip);
						
						Recipe current = r;
						get().update(()->{
							get().setDisabled(!main.hasItems(current.requirements));
							//get().setTouchable(!main.hasItems(current.requirements) ? Touchable.disabled : Touchable.enabled);
						});
						
					}}.width(220f);
					
					row();
				}
				
				get().pad(20f);
				
			}}.right().bottom();
			
		}}.end();
		
		new table(){{
			atop();
			aleft();
			itemtable = new table().top().left().get();
			itemtable.background("button");
		}}.end();
		
		//wave table...
		new table(){{
			atop();
			aright();
			
			new table(){{
				get().background("button");
				
				new label("Wave 1"){{
					get().setFontScale(1f);
					get().update(()->{
						get().setText("[YELLOW]Wave " + Moment.i.wave);
					});
				}}.left();
				
				row();
				
				new label("Time"){{
					get().update(()->{
						get().setText(Enemy.amount > 0 ? 
								Enemy.amount+" Enemies remaining" : "New wave in " + (int)(main.wavetime/60f));
					});
				}}.minWidth(150);
				
				get().pad(8);
			}};
		}}.end();
		
		
		updateItems();
		
		build.end();
	}
	
	public void updateItems(){
		itemtable.clear();
		
		for(Item stack : main.items.keys()){
			Image image = new Image(Draw.region("icon-" + stack.name()));
			Label label = new Label(""+ main.items.get(stack));
			label.setFontScale(1f);
			itemtable.add(image).size(32);
			itemtable.add(label);
			itemtable.row();
		}
	}
	
	float roundx(){
		return Mathf.round2(UGraphics.mouseWorldPos().x, TileType.tilesize);
	}

	float roundy(){
		return Mathf.round2(UGraphics.mouseWorldPos().y, TileType.tilesize);
	}

	int tilex(){
		return Mathf.scl2(UGraphics.mouseWorldPos().x, TileType.tilesize);
	}

	int tiley(){
		return Mathf.scl2(UGraphics.mouseWorldPos().y, TileType.tilesize);
	}
}
