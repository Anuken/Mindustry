package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.Button;
import io.anuke.ucore.scene.ui.Image;

public class MenuButton extends Button{
	
	public MenuButton(String text, PressGroup group, Listenable clicked){
		super("menu");
		add(text, "title", 4);
		clicked(clicked);
		group.add(this);
	}
}
