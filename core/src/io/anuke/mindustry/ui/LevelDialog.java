package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.maps;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.World;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Timers;

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
			Vars.ui.showLoading();
			Timers.run(16, ()->{
				World.loadMap(selectedMap);
				Vars.control.play();
			});
			
			Timers.run(18, ()->{
				Vars.ui.hideLoading();
			});
		}).pad(3).size(180, 44).units(Unit.dp);
		
		ButtonGroup<ImageButton> mapgroup = new ButtonGroup<>();
		
		for(int i = 0; i < maps.length; i ++){
			content().add(maps[i]);
		}
		
		content().row();
		
		for(int i = 0; i < maps.length; i ++){
			int index = i;
			ImageButton image = new ImageButton(new TextureRegion(World.getTexture(i)), "togglemap");
			mapgroup.add(image);
			image.clicked(()->{
				selectedMap = index;
			});
			image.getImageCell().size(Unit.dp.inPixels(164));
			content().add(image).size(Unit.dp.inPixels(180));
		}
		
		content().row();
		
		for(int i = 0; i < maps.length; i ++){
			scores[i] = new Label("");
			content().add(scores[i]);
		}
	}
}
