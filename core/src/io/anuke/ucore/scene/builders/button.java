package io.anuke.ucore.scene.builders;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.TextButton;

public class button extends builder<button, TextButton>{
	
	public button(String text, Listenable listener){
		this(text, "default", listener);
	}
	
	public button(String text, String style, Listenable listener){
		element = new TextButton(text, style);
		element.clicked(listener);
		cell = context().add(element);
	}
	
	public void textColor(Color color){
		element.getLabel().setColor(color);
	}
}
