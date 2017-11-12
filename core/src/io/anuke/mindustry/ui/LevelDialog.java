package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.StringSupplier;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class LevelDialog extends FloatingDialog{
	private Map selectedMap = Map.delta;
	private TextureRegion region = new TextureRegion();
	
	public LevelDialog(){
		super("Level Select");
		getTitleTable().getCell(title()).growX().center();
		getTitleTable().center();
		setup();
	}
	
	void setup(){
		addCloseButton();
		Table maps = new Table();
		ScrollPane pane = new ScrollPane(maps);
		
		int maxwidth = 4;
		
		for(int i = 0; i < Map.values().length; i ++){
			Map map = Map.values()[i];
			
			if(!map.visible && !Vars.debug) continue;
			
			if(i % maxwidth == 0){
				maps.row();
			}
			
			Table inset = new Table("pane-button");
			inset.add("[orange]"+map.name()).pad(3f).units(Unit.dp);
			inset.row();
			inset.add((StringSupplier)(()->"High Score: [orange]" + Settings.getInt("hiscore" + map.name())))
			.pad(3f).units(Unit.dp);
			inset.pack();
			
			float images = 154f;
			
			ImageButton image = new ImageButton(new TextureRegion(World.getTexture(map)), "togglemap");
			image.row();
			image.add(inset).width(images+6).units(Unit.dp);
			image.clicked(()->{
				selectedMap = map;
				hide();
				Vars.control.playMap(selectedMap);
			});
			image.getImageCell().size(images).units(Unit.dp);
			maps.add(image).width(170).pad(4f).units(Unit.dp);
		}
		
		content().add(pane);
		
		shown(()->{
			//this is necessary for some reason?
			Timers.run(2f, ()->{
				Core.scene.setScrollFocus(pane);
			});
		});
	}
}
