package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.utils.Align;

import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.KeybindDialog;

public class ControlsDialog extends KeybindDialog{
	
	public ControlsDialog(){
		setDialog();
		
		setFillParent(true);
		title().setAlignment(Align.center);
		getTitleTable().row();
		getTitleTable().add(new Image("white"))
		.growX().height(3f).pad(4f).get().setColor(Colors.get("accent"));
	}
	
	@Override
	public void addCloseButton(){
		buttons().addImageTextButton("$text.back", "icon-arrow-left", 30f, this::hide).size(230f, 64f);
		
		keyDown(key->{
			if(key == Keys.ESCAPE || key == Keys.BACK)
				hide();
		});
	}
}
