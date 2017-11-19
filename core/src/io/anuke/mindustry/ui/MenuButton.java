package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.Button;
import io.anuke.ucore.scene.ui.Image;

public class MenuButton extends Button{
	
	public MenuButton(String icon, Listenable clicked){
		super("menu");
		Image image = new Image(icon);
		image.setScaling(Scaling.fit);
		image.setScale(4f);
		image.setOrigin(Align.center);
		add(image);
		clicked(clicked);
	}
}
