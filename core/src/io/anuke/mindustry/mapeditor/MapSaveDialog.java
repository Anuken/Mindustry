package io.anuke.mindustry.mapeditor;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.TextField;

public class MapSaveDialog extends FloatingDialog{
	private TextField field;
	
	public MapSaveDialog(Consumer<String> cons){
		super("$text.editor.savemap");
		field = new TextField();
		
		Mindustry.platforms.addDialog(field);
		
		shown(() -> {
			content().clear();
			content().label(() ->{ 
				Map map = Vars.world.maps().getMap(field.getText());
				if(map != null){
					if(map.custom){
						return "$text.editor.overwrite";
					}else{
						return "$text.editor.failoverwrite";
					}
				}
				return "";
			}).colspan(2);
			content().row();
			content().add("$text.editor.mapname").padRight(14f);
			content().add(field).size(220f, 48f);
		});
		
		buttons().defaults().size(200f, 50f).pad(2f);
		buttons().addButton("$text.cancel", this::hide);
		
		TextButton button = new TextButton("$text.save");
		button.clicked(() -> {
			if(!invalid()){
				cons.accept(field.getText());
				hide();
			}
		});
		button.setDisabled(this::invalid);
		buttons().add(button);
	}
	
	public void setFieldText(String text){
		field.setText(text);
	}
		
	
	private boolean invalid(){
		if(field.getText().isEmpty()){
			return true;
		}
		Map map = Vars.world.maps().getMap(field.getText());
		return map != null && !map.custom;
	}
}
