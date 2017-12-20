package io.anuke.mindustry.ui;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.utils.Align;

import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.SettingsDialog;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MindustrySettingsDialog extends SettingsDialog{
	
	public MindustrySettingsDialog(){
		setFillParent(true);
		title().setAlignment(Align.center);
		getTitleTable().row();
		getTitleTable().add(new Image("white"))
		.growX().height(3f).pad(4f).get().setColor(Colors.get("accent"));
		
		content().remove();
		buttons().remove();
		
		ScrollPane pane = new ScrollPane(content(), "clear");
		pane.setFadeScrollBars(false);
		
		row();
		add(pane).expand().fill();
		row();
		add(buttons()).fillX();
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
