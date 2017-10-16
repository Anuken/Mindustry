package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.StringSupplier;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Unit;

public class LevelDialog extends Dialog{
	private Map selectedMap = Map.delta;
	
	public LevelDialog(){
		super("Level Select");
		setup();
	}
	
	void setup(){
		addCloseButton();
		getButtonTable().addButton("Play", ()->{
			hide();
			Vars.control.playMap(selectedMap);
		}).pad(3).size(180, 44).units(Unit.dp);
		
		ButtonGroup<ImageButton> mapgroup = new ButtonGroup<>();
		
		for(Map map : Map.values()){
			if(!map.visible) continue;
			
			content().add(map.name());
		}
		
		content().row();
		
		for(Map map : Map.values()){
			if(!map.visible) continue;
			
			ImageButton image = new ImageButton(new TextureRegion(World.getTexture(map)), "togglemap");
			mapgroup.add(image);
			image.clicked(()->{
				selectedMap = map;
			});
			image.getImageCell().size(Unit.dp.inPixels(164));
			content().add(image).size(Unit.dp.inPixels(180));
		}
		
		content().row();
		
		for(Map map : Map.values()){
			if(!map.visible) continue;
			
			content().add((StringSupplier)(()->"High Score: [lime]" + Settings.getInt("hiscore" + map.name())));
		}
	}
}
