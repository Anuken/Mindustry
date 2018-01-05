package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.function.BiConsumer;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;

public class MapResizeDialog extends FloatingDialog{
	int width, height;
	
	public MapResizeDialog(MapEditor editor, BiConsumer<Integer, Integer> cons){
		super("$text.editor.resizemap");
		shown(() -> {
			content().clear();
			Pixmap pix = editor.pixmap();
			width = pix.getWidth();
			height = pix.getHeight();
			
			Table table = new Table();
			
			for(int d = 0; d < 2; d ++){
				boolean w = d == 0;
				int curr = d == 0 ? pix.getWidth() : pix.getHeight();
				int idx = 0;
				for(int i = 0; i < MapEditor.validMapSizes.length; i ++)
					if(MapEditor.validMapSizes[i] == curr) idx = i;
				
				table.add(d == 0 ? "$text.width": "$text.height").padRight(8f);
				ButtonGroup<TextButton> group = new ButtonGroup<>();
				for(int i = 0; i < MapEditor.validMapSizes.length; i ++){
					int size = MapEditor.validMapSizes[i];
					TextButton button = new TextButton(size + "", "toggle");
					button.clicked(() -> {
						if(w)
							width = size;
						else
							height = size;
					});
					group.add(button);
					if(i == idx) button.setChecked(true);
					table.add(button).size(100f, 54f).pad(2f);
				}
				
				table.row();
			}
			
			content().label(() -> 
				width + height > 512 ? "$text.editor.resizebig" : ""
			).get().setAlignment(Align.center, Align.center);
			content().row();
			content().add(table);
			
		});
		
		buttons().defaults().size(200f, 50f);
		buttons().addButton("$text.cancel", this::hide);
		buttons().addButton("$text.editor.resize", () -> {
			cons.accept(width, height);
			hide();
		});
		
	}
}
