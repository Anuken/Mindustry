package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.ui.FloatingDialog;
import io.anuke.ucore.function.BiConsumer;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MapResizeDialog extends FloatingDialog{
	int width, height;
	
	public MapResizeDialog(MapEditor editor, BiConsumer<Integer, Integer> cons){
		super("resize map");
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
				
				table.add(d == 0 ? "Width: ": "Height: ");
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
					table.add(button).size(100f, 54f).pad(2f).units(Unit.dp);
				}
				
				table.row();
			}
			
			content().label(() -> 
				width + height > 512 ? "[scarlet]Warning!\n[]Maps larger than 256 units may be laggy and unstable." : ""
			).get().setAlignment(Align.center, Align.center);
			content().row();
			content().add(table);
			
		});
		
		buttons().defaults().size(200f, 50f).units(Unit.dp);
		buttons().addButton("Cancel", this::hide);
		buttons().addButton("Resize", () -> {
			cons.accept(width, height);
			hide();
		});
		
	}
}
