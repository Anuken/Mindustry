package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.Button;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;

public class MenuButton extends Button{
	
	public MenuButton(String text, PressGroup group, Listenable clicked){
		super("menu");
		String style = "title";
		float scale = 4f;
		BitmapFont font = Core.skin.getFont("title");
		for(char c : Bundles.get(text.substring(1)).toCharArray()){
			if(!font.getData().hasGlyph(c)){
				UCore.log("No glyph found: " + c);
				style = "default";
				scale = Unit.dp.scl(1f);
				break;
			}
		}
		add(text, style, scale);
		clicked(clicked);
		group.add(this);
	}
}
