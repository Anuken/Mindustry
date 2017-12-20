package io.anuke.mindustry.ui;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.utils.Align;

import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.KeybindDialog;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MindustryKeybindDialog extends KeybindDialog{
	
	public MindustryKeybindDialog(){
		setDialog();
		
		setFillParent(true);
		title().setAlignment(Align.center);
		getTitleTable().row();
		getTitleTable().add(new Image("white"))
		.growX().height(3f).pad(4f).get().setColor(Colors.get("accent"));
	}
	
	@Override
	public void addCloseButton(){
		buttons().addImageTextButton("Back", "icon-arrow-left", Unit.dp.scl(30f), ()->{
			hide();
		}).size(230f, 64f);
		
		keyDown(key->{
			if(key == Keys.ESCAPE || key == Keys.BACK)
				hide();
		});
	}
}
