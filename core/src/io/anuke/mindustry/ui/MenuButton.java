package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.TextButton;

public class MenuButton extends TextButton{

	public MenuButton(String icon, String text, Listenable clicked){
		this(icon, text, null, clicked);
	}
	
	public MenuButton(String icon, String text, String description, Listenable clicked){
		super("default");
		float s = 70f;

		clicked(clicked);

		clearChildren();

		margin(0);

		table(t -> {
			t.addImage(icon).size(14*3);
			t.update(() -> t.setBackground(getClickListener().isOver() || getClickListener().isVisualPressed() ? "button-over" : "button"));
		}).size(s - 5, s);


		table(t -> {
			t.add(text).wrap().growX().get().setAlignment(Align.center, Align.left);
			if(description != null){
				t.row();
				t.add(description).color(Color.LIGHT_GRAY);
			}
		}).padLeft(5).growX();
	}
}
