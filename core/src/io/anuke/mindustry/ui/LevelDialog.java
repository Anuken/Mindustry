package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.GameMode;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.StringSupplier;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Elements;

public class LevelDialog extends FloatingDialog{
	private Map selectedMap = Vars.world.maps().getMap(0);
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
		pane.setFadeScrollBars(false);
		
		int maxwidth = 4;
		
		Table selmode = new Table();
		ButtonGroup<TextButton> group = new ButtonGroup<>();
		selmode.add("Gamemode: ").padRight(10f).units(Unit.dp);
		
		for(GameMode mode : GameMode.values()){
			TextButton b = Elements.newButton(mode.toString(), "toggle", ()->{
				Vars.control.setMode(mode);
			});
			group.add(b);
			selmode.add(b).size(130f, 54f).units(Unit.dp);
		}
		
		content().add(selmode);
		content().row();
		
		int i = 0;
		for(Map map : Vars.world.maps().list()){
			
			if(!map.visible && !Vars.debug) continue;
			
			if(i % maxwidth == 0){
				maps.row();
			}
			
			Table inset = new Table("pane-button");
			inset.add("[accent]"+map.name).pad(3f).units(Unit.dp);
			inset.row();
			inset.add((StringSupplier)(()->"High Score: [accent]" + Settings.getInt("hiscore" + map.name)))
			.pad(3f).units(Unit.dp);
			inset.pack();
			
			float images = 154f;
			
			Stack stack = new Stack();
			
			Image back = new Image("white");
			back.setColor(map.backgroundColor);
			
			ImageButton image = new ImageButton(new TextureRegion(map.texture), "togglemap");
			image.row();
			image.add(inset).width(images+6).units(Unit.dp);
			image.clicked(()->{
				selectedMap = map;
				hide();
				Vars.control.playMap(selectedMap);
			});
			image.getImageCell().size(images).units(Unit.dp);
			
			stack.add(back);
			stack.add(image);
			
			maps.add(stack).width(170).pad(4f).units(Unit.dp);
			
			maps.padRight(Unit.dp.inPixels(26));
			
			i ++;
		}
		
		content().add(pane).uniformX();
		
		shown(()->{
			//this is necessary for some reason?
			Timers.run(2f, ()->{
				Core.scene.setScrollFocus(pane);
			});
		});
	}
}
