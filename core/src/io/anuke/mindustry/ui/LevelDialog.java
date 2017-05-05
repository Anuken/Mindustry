package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.World;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.*;

public class LevelDialog extends Dialog{
	Label[] scores = new Label[maps.length];
	private int selectedMap;
	
	public LevelDialog(){
		super("Level Select");
		setup();
		
		shown(()->{
			for(int i = 0; i < maps.length; i ++)
				scores[i].setText("High Score: [lime]" + Settings.getInt("hiscore"+maps[i]));
		});
	}
	
	void setup(){
		addCloseButton();
		getButtonTable().addButton("Play", ()->{
			hide();
			World.loadMap(selectedMap);
			GameState.play();
		});
		
		ButtonGroup<ImageButton> mapgroup = new ButtonGroup<>();
		
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
		
		content().row();
		
		for(int i = 0; i < maps.length; i ++){
			scores[i] = new Label("");
			content().add(scores[i]);
		}
	}
}
