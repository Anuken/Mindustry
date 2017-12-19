package io.anuke.mindustry.mapeditor;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.mindustry.ui.FloatingDialog;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MapLoadDialog extends FloatingDialog{
	Map selected = Vars.world.maps().getMap(0);

	public MapLoadDialog(Consumer<Map> loader) {
		super("load map");
		ButtonGroup<TextButton> group = new ButtonGroup<>();
		
		int maxcol = 3;
		
		int i = 0;
		
		Table table = new Table();
		table.defaults().size(200f, 90f).units(Unit.dp).pad(4f);
		table.pad(Unit.dp.inPixels(10f));
		
		ScrollPane pane = new ScrollPane(table, "horizontal");
		pane.setFadeScrollBars(false);
		
		for(Map map : Vars.world.maps().list()){
			if(!map.visible) continue;
			
			TextButton button = new TextButton(map.name, "toggle");
			button.add(new BorderImage(map.texture, 2f)).size(Unit.dp.inPixels(16*4f));
			button.getCells().reverse();
			button.clicked(() -> selected = map);
			button.getLabelCell().grow().left().padLeft(5f).units(Unit.dp);
			group.add(button);
			table.add(button);
			if(++i % maxcol == 0) table.row();
		}
		
		content().add("Select a map to load:");
		content().row();
		content().add(pane);
		
		TextButton button = new TextButton("Load");
		button.setDisabled(() -> selected == null);
		button.clicked(() -> {
			if(selected != null){
				loader.accept(selected);
				hide();
			}
		});
		
		buttons().defaults().size(200f, 50f).units(Unit.dp);
		buttons().addButton("Cancel", this::hide);
		buttons().add(button);
	}

}
