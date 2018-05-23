package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ScrollPane;

public class FloatingDialog extends Dialog{
	
	public FloatingDialog(String title){
		super(title, "dialog");
		setFillParent(true);
		title().setAlignment(Align.center);
		getTitleTable().row();
		getTitleTable().addImage("white", Palette.accent)
		.growX().height(3f).pad(4f);

		boolean[] done = {false};

		shown(() -> Gdx.app.postRunnable(() ->
				forEach(child -> {
					if (done[0]) return;

					if (child instanceof ScrollPane) {
						Core.scene.setScrollFocus(child);
						done[0] = true;
					}
				})));
	}
	
	@Override
	public void addCloseButton(){
		buttons().addImageTextButton("$text.back", "icon-arrow-left", 30f, this::hide).size(230f, 64f);
		
		keyDown(key -> {
			if(key == Keys.ESCAPE || key == Keys.BACK)
				hide();
		});
	}
}
