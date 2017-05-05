package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.World;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ImageButton;

public class LevelDialog extends Dialog{
	private int selectedMap;
	
	public LevelDialog(){
		super("Level Select");
		setup();
	}
	
	void setup(){
		ButtonGroup<ImageButton> mapgroup = new ButtonGroup<>();
		
		addCloseButton();
		getButtonTable().addButton("Play", ()->{
			hide();
			World.loadMap(selectedMap);
			GameState.play();
		});
		
		for(int i = 0; i < maps.length; i ++){
			content().add(maps[i]);
		}
		
		content().row();
		
		for(int i = 0; i < maps.length; i ++){
			int index = i;
			ImageButton image = new ImageButton(new TextureRegion(mapTextures[i]), "togglemap");
			mapgroup.add(image);
			image.clicked(()->{
				selectedMap = index;
			});
			image.getImageCell().size(150, 150);
			content().add(image).size(180);
		}
	}
}
