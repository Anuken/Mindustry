package io.anuke.ucore.scene.builders;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;

import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.ui.Label;

public class label extends builder<label, Label>{
	
	public label(String text){
		element = new Label(text);
		cell = context().add(element);
	}
	
	public label(String text, String style){
		element = new Label(text, style);
		cell = context().add(element);
	}
	
	public label(Supplier<String> prov){
		element = new Label(prov);
		cell = context().add(element);
	}
	
	public label scale(float scale){
		element.setFontScale(scale);
		return this;
	}
	
	public label textAlign(int align){
		element.setAlignment(align);
		return this;
	}
	
	public label color(Color color){
		element.setColor(color);
		return this;
	}
	
	public label color(String name){
		element.setColor(Colors.get(name));
		return this;
	}
}
